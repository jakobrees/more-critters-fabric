package com.morecritters.fabric.module.nightshroom;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.TextureVariants;
import com.morecritters.fabric.ids.CritterlingsEIds;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The loyal shroom an ancient skeleton becomes after a stew of life. It is tamed by the nearest
 * player when it appears (or by the first player to right-click it); its owner right-clicks it
 * to make it sit or stand. It follows and defends its owner, is stronger and faster at night and
 * now and then heals itself. Every five to seven seconds in a fight it ruffles its cap and lets
 * loose five lightflies, which a moment later all dive at its target. Named "Natsirt" it wears a
 * different coat. Takes no fall or drowning damage and never despawns.
 */
public class NightshroomEntity extends TamableAnimal implements GeoEntity, TextureVariants {
	private static final int RUFFLE_MIN = 100, RUFFLE_MAX = 150;
	private static final int LIGHTFLIES = 5, LIGHTFLY_RELEASE_DELAY = 5, LIGHTFLY_SEND_DELAY = 40;
	private static final double LIGHTFLY_SWARM_RANGE = 40.0, PREY_SEARCH_SIZE = 80.0;
	private static final double TAME_SEARCH_SIZE = 50.0;
	private static final int HEAL_CHANCE = 300, NIGHT_SPARKLE_CHANCE = 50;
	private static final double ATTACK_REACH_SQR = 9.0;
	private static final Identifier TAME_ADVANCEMENT = MoreCritters.id("tame_nightshroom");

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
	private static final RawAnimation CHASE = RawAnimation.begin().thenLoop("chase");
	private static final RawAnimation SIT = RawAnimation.begin().thenLoop("sit");

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	/** Ticks until it next ruffles and calls lightflies, if it is fighting. */
	private int ruffleTimer;

	public NightshroomEntity(EntityType<? extends NightshroomEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			.add(Attributes.MAX_HEALTH, 150.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 8.0)
			.add(Attributes.FOLLOW_RANGE, 32.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5, false) {
			@Override
			protected boolean canPerformAttack(LivingEntity target) {
				return this.isTimeToAttack() && this.mob.distanceToSqr(target) < ATTACK_REACH_SQR && this.mob.getSensing().hasLineOfSight(target);
			}
			@Override public boolean canUse() { return !isSitting() && super.canUse(); }
			@Override public boolean canContinueToUse() { return !isSitting() && super.canContinueToUse(); }
		});
		this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
		this.targetSelector.addGoal(3, new HurtByTargetGoal(this) {
			@Override public boolean canUse() { return !isSitting() && super.canUse(); }
			@Override public boolean canContinueToUse() { return !isSitting() && super.canContinueToUse(); }
		});
		// Only while it has nobody to fight.
		this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.0, 5.0F, 2.0F) {
			@Override public boolean canUse() { return getTarget() == null && !isSitting() && super.canUse(); }
			@Override public boolean canContinueToUse() { return getTarget() == null && !isSitting() && super.canContinueToUse(); }
		});
		this.targetSelector.addGoal(5, new OwnerHurtByTargetGoal(this));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0) {
			@Override public boolean canUse() { return !isSitting() && super.canUse(); }
			@Override public boolean canContinueToUse() { return !isSitting() && super.canContinueToUse(); }
		});
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(9, new FloatGoal(this));
	}

	private boolean isSitting() {
		return this.isOrderedToSit();
	}

	private void setSitting(boolean sitting) {
		this.setOrderedToSit(sitting);
		this.setInSittingPose(sitting);
	}

	/** Appears tamed by the nearest player within 25 blocks, with hearts and an advancement. */
	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, groupData);
		level.getEntitiesOfClass(Player.class, AABB.ofSize(this.position(), TAME_SEARCH_SIZE, TAME_SEARCH_SIZE, TAME_SEARCH_SIZE), player -> true).stream()
			.min(Comparator.comparingDouble(this::distanceToSqr))
			.ifPresent(owner -> {
				this.tame(owner);
				level.getLevel().sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 4.0, this.getZ(), 5, 1.0, 1.0, 1.0, 1.0);
				Advancements.award(owner, TAME_ADVANCEMENT);
			});
		this.ruffleTimer = Mth.nextInt(this.random, RUFFLE_MIN, RUFFLE_MAX);
		return result;
	}

	/** The owner makes it sit or stand; anyone may tame it while it has no owner. */
	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		InteractionResult result = super.mobInteract(player, hand);
		if (result.consumesAction() || hand != InteractionHand.MAIN_HAND) return result;
		if (this.isTame() && !this.isOwnedBy(player)) return result;
		if (!this.level().isClientSide()) {
			if (this.isTame()) toggleSitting();
			else this.tame(player);
		}
		return InteractionResult.SUCCESS;
	}

	private void toggleSitting() {
		if (isSitting()) {
			setSitting(false);
			this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 5, 30, false, false));
			this.triggerAnim(Animations.ACTIONS, "sit_rise");
		} else {
			setSitting(true);
		}
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!(this.level() instanceof ServerLevel level)) return;
		boolean night = !level.isBrightOutside();
		if (night) {
			this.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20, 0, false, false));
			this.addEffect(new MobEffectInstance(MobEffects.SPEED, 20, 0, false, false));
			if (this.random.nextInt(NIGHT_SPARKLE_CHANCE) == 0) {
				Bursts.forced(level, ParticleTypes.WAX_OFF, this.position().add(0.0, 3.0, 0.0), 3, 1.0, 1.0, 1.0, 1.0);
			}
		}
		if (this.getHealth() < this.getMaxHealth() && this.random.nextInt(HEAL_CHANCE) == 0) {
			this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 2, false, true));
		}
		if (--this.ruffleTimer == 1) {
			this.ruffleTimer = Mth.nextInt(this.random, RUFFLE_MIN, RUFFLE_MAX);
			if (this.getTarget() != null && !isSitting()) ruffle(level);
		}
	}

	/** Shakes its cap in place; lightflies come out of it and soon after go for its target. */
	private void ruffle(ServerLevel level) {
		this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 49, false, false));
		this.triggerAnim(Animations.ACTIONS, "shake");
		Sounds.playAt(this, NightshroomModule.NIGHTSHROOM_RUFFLE_SOUND, SoundSource.VOICE, 1.0F, 1.0F);
		Vec3 origin = this.position();
		ServerScheduler.runLater(LIGHTFLY_RELEASE_DELAY, () -> releaseLightflies(level, origin));
		ServerScheduler.runLater(LIGHTFLY_SEND_DELAY, () -> sendLightflies(level, origin));
	}

	private void releaseLightflies(ServerLevel level, Vec3 origin) {
		OtherModules.entityType(CritterlingsEIds.Entities.LIGHTFLY).ifPresent(lightfly -> {
			for (int i = 0; i < LIGHTFLIES; i++) {
				BlockPos at = BlockPos.containing(origin.x + Mth.nextInt(this.random, -1, 1), origin.y + 3.0, origin.z + Mth.nextInt(this.random, -1, 1));
				Entity spawned = lightfly.spawn(level, at, EntitySpawnReason.MOB_SUMMONED);
				if (spawned != null) spawned.setDeltaMovement(0.0, 0.3, 0.0);
			}
		});
	}

	/**
	 * Every lightfly within 40 blocks goes for this nightshroom's target, or, if it has none any
	 * more, for the monster nearest to where it ruffled.
	 */
	private void sendLightflies(ServerLevel level, Vec3 origin) {
		OtherModules.sound(CritterlingsEIds.Sounds.ENTITY_LIGHTFLY_TARGET)
			.ifPresent(sound -> level.playSound(null, BlockPos.containing(origin), sound, SoundSource.VOICE, 1.0F, 1.0F));
		LivingEntity target = this.getTarget() != null ? this.getTarget() : nearestMonster(level, origin);
		if (target == null) return;
		for (Mob lightfly : level.getEntitiesOfClass(Mob.class, new AABB(origin, origin).inflate(LIGHTFLY_SWARM_RANGE), NightshroomEntity::isLightfly)) {
			lightfly.setTarget(target);
		}
	}

	private static @Nullable LivingEntity nearestMonster(ServerLevel level, Vec3 origin) {
		return level.getEntitiesOfClass(Monster.class, AABB.ofSize(origin, PREY_SEARCH_SIZE, PREY_SEARCH_SIZE, PREY_SEARCH_SIZE), monster -> !isLightfly(monster)).stream()
			.min(Comparator.comparingDouble(monster -> monster.distanceToSqr(origin)))
			.orElse(null);
	}

	private static boolean isLightfly(Entity entity) {
		return OtherModules.isType(entity, CritterlingsEIds.Entities.LIGHTFLY);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypes.FALL) || source.is(DamageTypes.DROWN)) return false;
		return super.hurtServer(level, source, damage);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		this.triggerAnim(Animations.ACTIONS, "attack");
		return super.doHurtTarget(level, target);
	}

	@Override
	public String textureName() {
		String name = this.getName().getString();
		return name.equals("Natsirt") || name.equals("natsirt") ? "nightshroom_texture_natsirt" : "nightshroom";
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return false;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return NightshroomModule.NIGHTSHROOM.create(level, EntitySpawnReason.BREEDING);
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override protected SoundEvent getAmbientSound() { return NightshroomModule.NIGHTSHROOM_IDLE_SOUND; }
	@Override protected SoundEvent getHurtSound(DamageSource source) { return NightshroomModule.NIGHTSHROOM_HURT_SOUND; }
	@Override protected SoundEvent getDeathSound() { return NightshroomModule.NIGHTSHROOM_DEATH_SOUND; }

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("RuffleTimer", this.ruffleTimer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.ruffleTimer = input.getIntOr("RuffleTimer", RUFFLE_MAX);
		this.setInSittingPose(this.isOrderedToSit());
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(Animations.MOVEMENT, 2, (AnimationTest<NightshroomEntity> test) -> {
			if (this.isInSittingPose()) return test.setAndContinue(SIT);
			if (!test.isMoving()) return test.setAndContinue(IDLE);
			return test.setAndContinue(this.isAggressive() ? CHASE : WALK);
		}));
		controllers.add(Animations.actions(this, "attack", "shake", "sit_rise"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
