package com.morecritters.fabric.module.kelpire;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.TextureVariants;
import com.morecritters.fabric.ids.BlubberfishIds;
import com.morecritters.fabric.ids.MiscIds;
import com.morecritters.fabric.ids.NauticrawlIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A big red sea predator of lukewarm oceans. Wild ones hide in kelp (they turn invisible next
 * to it), dive now and then, and hunt blubberfish. Fed raw blubberfish they may be tamed: a tame
 * kelpire follows its owner through the water, sits and stands on a right-click from the owner,
 * and after each kill burps up healing bubbles. Out of water it dries out slowly.
 */
public class KelpireEntity extends TamableAnimal implements GeoEntity, TextureVariants {
	private static final float ADULT_SCALE = 1.2F;
	/** NeoForge's swim speed attribute in the original; multiplies in-water acceleration. */
	private static final float SWIM_SPEED = 2.0F;
	private static final int TAME_CHANCE = 3;
	private static final float DEFAULT_HEAL = 4.0F;

	private static final int DIVE_MIN = 60, DIVE_MAX = 200;
	private static final int DIVE_DEPTH = 5;
	private static final int SPAWN_AIR = 400, AIR_IN_WATER = 200, AIR_AFTER_DRY_OUT = 20;

	private static final double FOLLOW_SEARCH_RADIUS = 50.0;
	private static final float TELEPORT_DISTANCE = 15.0F;

	private static final int BURP_DELAY = 20;
	private static final double BURP_RECOIL = 0.6;
	private static final float BIG_PREY_HEALTH = 10.0F;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	/** Ticks until the kelpire next considers diving. */
	private int diveTimer;
	/** Ticks of air left out of water before it starts drying out. */
	private int airTicks = SPAWN_AIR;

	public KelpireEntity(EntityType<? extends KelpireEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setPathfindingMalus(PathType.WATER, 0.0F);
		this.moveControl = new KelpireMoveControl(this);
		this.diveTimer = Mth.nextInt(this.random, DIVE_MIN, DIVE_MAX);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
			.add(Attributes.MOVEMENT_SPEED, 2.0)
			.add(Attributes.MAX_HEALTH, 50.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 7.0)
			.add(Attributes.FOLLOW_RANGE, 32.0)
			.add(Attributes.STEP_HEIGHT, 0.6);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new WaterBoundPathNavigation(this, level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false) {
			@Override
			protected boolean canPerformAttack(LivingEntity target) {
				return this.isTimeToAttack() && this.mob.distanceToSqr(target) < 4.0 && this.mob.getSensing().hasLineOfSight(target);
			}

			@Override
			public boolean canUse() { return super.canUse() && !isOrderedToSit(); }

			@Override
			public boolean canContinueToUse() { return super.canContinueToUse() && !isOrderedToSit(); }
		});
		this.goalSelector.addGoal(2, new RandomSwimmingGoal(this, 1.0, 40) {
			@Override
			public boolean canUse() { return super.canUse() && !isOrderedToSit(); }

			@Override
			public boolean canContinueToUse() { return super.canContinueToUse() && !isOrderedToSit(); }
		});
		this.targetSelector.addGoal(3, new OwnerHurtTargetGoal(this) {
			@Override
			public boolean canUse() { return super.canUse() && !isOrderedToSit(); }

			@Override
			public boolean canContinueToUse() { return super.canContinueToUse() && !isOrderedToSit(); }
		});
		this.targetSelector.addGoal(4, new OwnerHurtByTargetGoal(this) {
			@Override
			public boolean canUse() { return super.canUse() && !isOrderedToSit(); }

			@Override
			public boolean canContinueToUse() { return super.canContinueToUse() && !isOrderedToSit(); }
		});
		this.goalSelector.addGoal(5, new BreedGoal(this, 1.0) {
			@Override
			public boolean canUse() { return super.canUse() && !isOrderedToSit(); }

			@Override
			public boolean canContinueToUse() { return super.canContinueToUse() && !isOrderedToSit(); }
		});
		this.targetSelector.addGoal(6, new HurtByTargetGoal(this));
		// Only wild kelpires come for the fish; a tame one waits to be handed it.
		this.goalSelector.addGoal(7, new TemptGoal(this, 1.0, this::isFood, false) {
			@Override
			public boolean canUse() { return super.canUse() && !isTame(); }

			@Override
			public boolean canContinueToUse() { return super.canContinueToUse() && !isTame(); }
		});
		this.targetSelector.addGoal(8, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, false, false,
			(target, level) -> blubberfishType() != null && target.getType() == blubberfishType()) {
			@Override
			public boolean canUse() { return super.canUse() && !isOrderedToSit(); }

			@Override
			public boolean canContinueToUse() { return super.canContinueToUse() && !isOrderedToSit(); }
		});
		this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
	}

	// --- Ticking ----------------------------------------------------------------------

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level() instanceof ServerLevel) {
			if (this.isTame()) {
				followOwner();
			} else {
				diveNowAndThen();
			}
			breatheOrDryOut();
		}
	}

	/** Swims after its owner, and catches up by teleporting when both are in water but far apart. */
	private void followOwner() {
		LivingEntity owner = this.getOwner();
		if (owner == null || this.isOrderedToSit() || this.getTarget() != null) {
			return;
		}
		if (this.distanceToSqr(owner) <= FOLLOW_SEARCH_RADIUS * FOLLOW_SEARCH_RADIUS) {
			this.getNavigation().moveTo(owner.getX(), owner.getY(), owner.getZ(), 1.0);
		}
		if (this.isInWater() && owner.isInWater() && this.distanceTo(owner) >= TELEPORT_DISTANCE) {
			this.teleportTo(owner.getX(), owner.getY(), owner.getZ());
		}
	}

	/** Every few seconds a wild kelpire near the surface heads a few blocks down. */
	private void diveNowAndThen() {
		if (--this.diveTimer > -1) {
			return;
		}
		this.diveTimer = Mth.nextInt(this.random, DIVE_MIN, DIVE_MAX);
		if (this.level().getBlockState(BlockPos.containing(this.getX(), this.getY() + 2.0, this.getZ())).is(Blocks.AIR)) {
			this.getNavigation().moveTo(this.getX(), this.getY() - DIVE_DEPTH, this.getZ(), 1.0);
		}
	}

	/** In water its air refills; on land it runs out and then dries out a heart every second. */
	private void breatheOrDryOut() {
		if (this.isInWater()) {
			this.airTicks = AIR_IN_WATER;
			return;
		}
		if (--this.airTicks <= 1) {
			this.hurt(this.damageSources().dryOut(), 1.0F);
			this.airTicks = AIR_AFTER_DRY_OUT;
		}
	}

	/** The original's swim speed attribute of 2 doubles how hard it pushes through water. */
	@Override
	public void moveRelative(float speed, Vec3 input) {
		super.moveRelative(this.isInWater() ? speed * SWIM_SPEED : speed, input);
	}

	// --- Combat -----------------------------------------------------------------------

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return !source.is(DamageTypes.DROWN) && super.hurtServer(level, source, amount);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		Sounds.playAt(target, KelpireModule.BITE_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
		this.triggerAnim(Animations.ACTIONS, "attack");
		return super.doHurtTarget(level, target);
	}

	/** A tame kelpire that kills in water burps a second later, blowing out bubbles that heal. */
	@Override
	public boolean killedEntity(ServerLevel level, LivingEntity victim, DamageSource source) {
		if (this.isTame() && this.isInWater()) {
			Vec3 killPos = victim.position();
			boolean bigPrey = victim.getMaxHealth() > BIG_PREY_HEALTH;
			ServerScheduler.runLater(BURP_DELAY, () -> burp(level, killPos, bigPrey));
		}
		return super.killedEntity(level, victim, source);
	}

	private void burp(ServerLevel level, Vec3 killPos, boolean bigPrey) {
		this.triggerAnim(Animations.ACTIONS, "burp");
		level.playSound(null, BlockPos.containing(killPos), KelpireModule.BURP_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
		this.setDeltaMovement(this.getLookAngle().scale(-BURP_RECOIL));
		ServerScheduler.runLater(1, () -> blowBubbles(level, bigPrey ? Mth.nextInt(this.random, 2, 3) : 1));
	}

	private void blowBubbles(ServerLevel level, int bubbles) {
		if (BuiltInRegistries.PARTICLE_TYPE.getValue(MiscIds.Particles.BLOOD_BUBBLE_PARTICLE) instanceof ParticleOptions bloodBubble) {
			Particles.spawnAt(this, bloodBubble, Mth.nextInt(this.random, 5, 9), 0.3, 0.3, 0.3, 0.02);
		}
		if (!BuiltInRegistries.ENTITY_TYPE.containsKey(NauticrawlIds.Entities.BUBBLE_ENTITY)) {
			return;
		}
		EntityType<?> bubble = BuiltInRegistries.ENTITY_TYPE.getValue(NauticrawlIds.Entities.BUBBLE_ENTITY);
		BlockPos spawnPos = BlockPos.containing(this.getX(), this.getY() + 0.2, this.getZ());
		for (int i = 0; i < bubbles; i++) {
			Entity spawned = bubble.spawn(level, spawnPos, EntitySpawnReason.MOB_SUMMONED);
			if (spawned != null) {
				spawned.setDeltaMovement(Vec3.ZERO);
			}
		}
	}

	// --- Taming and interaction -------------------------------------------------------

	@Override
	public boolean isFood(ItemStack stack) {
		return !stack.isEmpty() && stack.is(BuiltInRegistries.ITEM.getValue(BlubberfishIds.Items.RAW_BLUBBERFISH));
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.getItem() instanceof SpawnEggItem) {
			return super.mobInteract(player, hand);
		}
		if (this.level().isClientSide()) {
			return (this.isTame() && this.isOwnedBy(player)) || this.isFood(stack) ? InteractionResult.SUCCESS : InteractionResult.PASS;
		}
		InteractionResult result = interactOnServer(player, hand, stack);
		toggleSittingFor(player);
		return result;
	}

	private InteractionResult interactOnServer(Player player, InteractionHand hand, ItemStack stack) {
		if (this.isTame()) {
			if (!this.isOwnedBy(player)) {
				return InteractionResult.SUCCESS;
			}
			if (this.isFood(stack) && this.getHealth() < this.getMaxHealth()) {
				FoodProperties food = stack.get(DataComponents.FOOD);
				this.usePlayerItem(player, hand, stack);
				this.heal(food != null ? food.nutrition() : DEFAULT_HEAL);
				return InteractionResult.SUCCESS;
			}
			return super.mobInteract(player, hand);
		}
		if (this.isFood(stack)) {
			this.usePlayerItem(player, hand, stack);
			if (this.random.nextInt(TAME_CHANCE) == 0) {
				this.tame(player);
				this.level().broadcastEntityEvent(this, (byte) 7);
			} else {
				this.level().broadcastEntityEvent(this, (byte) 6);
			}
			this.setPersistenceRequired();
			return InteractionResult.SUCCESS;
		}
		InteractionResult result = super.mobInteract(player, hand);
		if (result.consumesAction()) {
			this.setPersistenceRequired();
		}
		return result;
	}

	/** Any right-click by the owner (even feeding, even the taming one) makes it sit or get up. */
	private void toggleSittingFor(Player player) {
		if (!this.isTame() || !this.isOwnedBy(player)) {
			return;
		}
		boolean sit = !this.isOrderedToSit();
		this.setOrderedToSit(sit);
		this.setInSittingPose(sit);
		if (!sit) {
			this.triggerAnim(Animations.ACTIONS, "sit_end");
		}
	}

	@Override
	public void tame(Player player) {
		super.tame(player);
		Advancements.award(player, MoreCritters.id("tame_kelpire"));
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return KelpireModule.KELPIRE.create(level, EntitySpawnReason.BREEDING);
	}

	// --- Body ---------------------------------------------------------------------------

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).scale(ADULT_SCALE);
	}

	@Override
	public boolean checkSpawnObstruction(LevelReader level) {
		return level.isUnobstructed(this);
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return KelpireModule.IDLE_SOUND;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return KelpireModule.HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return KelpireModule.DEATH_SOUND;
	}

	/** Named "Crusty" it always shows its crust; otherwise tame, or invisible when lurking in kelp. */
	@Override
	public String textureName() {
		String name = this.getName().getString();
		if (name.equals("Crusty") || name.equals("crusty")) {
			return "kelpire_crusty";
		}
		if (this.isTame()) {
			return "kelpire_tamed";
		}
		return isBesideKelp() ? "kelpire_invisible" : "kelpire";
	}

	private boolean isBesideKelp() {
		BlockPos pos = BlockPos.containing(this.getX(), this.getY(), this.getZ());
		return isKelp(pos) || isKelp(pos.east()) || isKelp(pos.west()) || isKelp(pos.south()) || isKelp(pos.north());
	}

	private boolean isKelp(BlockPos pos) {
		return this.level().getBlockState(pos).is(Blocks.KELP_PLANT);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("DiveTimer", this.diveTimer);
		output.putInt("Air", this.airTicks);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.diveTimer = input.getIntOr("DiveTimer", this.diveTimer);
		this.airTicks = input.getIntOr("Air", SPAWN_AIR);
	}

	/** Null while the blubberfish is not registered (the entity registry would answer "pig"). */
	private static @Nullable EntityType<?> blubberfishType() {
		return BuiltInRegistries.ENTITY_TYPE.getOptional(BlubberfishIds.Entities.BLUBBERFISH).orElse(null);
	}

	// --- GeckoLib ---------------------------------------------------------------------

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("swim");
	private static final RawAnimation LAND = RawAnimation.begin().thenLoop("land");
	private static final RawAnimation SITTING = RawAnimation.begin().thenLoop("sitting");

	/** Flops about on land, sits when told to, otherwise idles or swims. */
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<KelpireEntity>(Animations.MOVEMENT, 4, test -> {
			if (!this.isInWater()) {
				return test.setAndContinue(LAND);
			}
			if (this.isInSittingPose()) {
				return test.setAndContinue(SITTING);
			}
			return test.setAndContinue(test.isMoving() ? SWIM : IDLE);
		}));
		controllers.add(Animations.actions(this, "attack", "burp", "sit_end"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
