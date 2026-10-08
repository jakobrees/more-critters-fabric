package com.morecritters.fabric.module.shadelet;

import net.minecraft.world.item.component.SwingAnimation;
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
import com.morecritters.fabric.core.Config;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.TextureVariants;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The shadelet: a shy dark-forest shade. Every 10-20 seconds it sneaks up on a nearby player and
 * screams in their face (Spooked), then runs away. Fed a sweet, it gets a sugar rush: it sprints
 * about dripping, and instead of players it hunts down monsters to scream at.
 */
public class ShadeletEntity extends PathfinderMob implements GeoEntity, TextureVariants {
	static final float SIZE_SCALE = 1.1F;
	private static final double HUNT_RANGE = 15.0;
	private static final double MONSTER_SEARCH_SIZE = 30.0;
	private static final float SCREAM_DISTANCE = 2.0F;
	private static final int SPOOK_MIN = 200, SPOOK_MAX = 400;
	private static final int SUGAR_RUSH_SPOOK_INTERVAL = 60;
	private static final int FLEE_TICKS = 40;
	private static final int SPOOKED_TICKS = 100;
	private static final int SCREAM_FREEZE_TICKS = 10, FREEZE_AMPLIFIER = 49;
	private static final double CHASE_PLAYER_SPEED = 1.5, CHASE_MONSTER_SPEED = 1.0;
	private static final String LOLIPOP_NAME = "Lolipop";

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
	private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	/** Ticks of sugar rush left; negative once it has worn off (only then will it eat again). */
	private int sugarRush;
	/** Counts down to the next scare; below -1 it picks a victim. */
	private int spookTimer;
	/** Ticks left fleeing after a scream. */
	private int fleeTimer;
	private boolean chasing;
	private boolean shivering;

	public ShadeletEntity(EntityType<? extends ShadeletEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 10.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 0.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, LivingEntity.class, 6.0F));
		this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, LivingEntity.class, 6.0F, 1.0, 1.2) {
			@Override
			public boolean canUse() {
				return isFleeing() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return isFleeing() && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(5, new FloatGoal(this));
	}

	private boolean isFleeing() {
		return fleeTimer > 0;
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
		spookTimer = Mth.nextInt(this.random, SPOOK_MIN, SPOOK_MAX);
		return super.finalizeSpawn(level, difficulty, reason, data);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level() instanceof ServerLevel level) {
			sugarRush--;
			spookTimer--;
			fleeTimer--;
			updateSugarRush(level);
			if (spookTimer < -1) {
				pickVictim(level);
			}
			if (chasing) {
				chase(level);
			}
		}
	}

	/** On a sugar rush it sprints while moving, dripping now and then, and shivers when standing still. */
	private void updateSugarRush(ServerLevel level) {
		if (sugarRush <= 0) {
			this.setSprinting(false);
			return;
		}
		if (this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6) {
			this.setSprinting(true);
			if (this.random.nextInt(5) == 0) {
				level.sendParticles(ParticleTypes.FALLING_WATER, true, true, getX(), getY() + 1, getZ(), 25, 0.1, 0.1, 0.1, 1);
			}
			stopShivering();
		} else {
			this.setSprinting(false);
			if (!shivering) {
				shivering = true;
				triggerAnim(Animations.ACTIONS, "idle_shiver");
			}
		}
	}

	private void stopShivering() {
		if (shivering) {
			shivering = false;
			triggerAnim(Animations.ACTIONS, "anim_reset");
		}
	}

	/** Time for a scare: a monster if sugar-rushed (checked every 3 s), otherwise a player if the config allows. */
	private void pickVictim(ServerLevel level) {
		stopShivering();
		if (sugarRush > 0) {
			spookTimer = SUGAR_RUSH_SPOOK_INTERVAL;
			if (!nearby(level, Monster.class, HUNT_RANGE, ShadeletEntity::notSpooked).isEmpty()) {
				startChasing();
			}
		} else {
			spookTimer = Mth.nextInt(this.random, SPOOK_MIN, SPOOK_MAX);
			if (jumpscaresPlayers() && !nearby(level, Player.class, HUNT_RANGE, ShadeletEntity::isScareablePlayer).isEmpty()) {
				startChasing();
			}
		}
	}

	private void startChasing() {
		chasing = true;
		this.setSprinting(true);
	}

	private void chase(ServerLevel level) {
		if (sugarRush > 0) {
			if (!nearby(level, Monster.class, HUNT_RANGE, ShadeletEntity::notSpooked).isEmpty()) {
				nearestMonster(level).filter(ShadeletEntity::notSpooked).ifPresent(monster -> approachAndScream(monster, CHASE_MONSTER_SPEED));
			}
		} else if (jumpscaresPlayers()) {
			nearby(level, Player.class, HUNT_RANGE, ShadeletEntity::isScareablePlayer).stream().findFirst()
				.ifPresent(player -> approachAndScream(player, CHASE_PLAYER_SPEED));
		}
	}

	private void approachAndScream(LivingEntity victim, double speed) {
		this.getNavigation().moveTo(victim.getX(), victim.getY(), victim.getZ(), speed);
		if (this.distanceTo(victim) < SCREAM_DISTANCE) {
			screamAt(victim);
		}
	}

	/** Freezes for half a second, screams, spooks the victim, then flees for two seconds. */
	private void screamAt(LivingEntity victim) {
		triggerAnim(Animations.ACTIONS, "scream");
		this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SCREAM_FREEZE_TICKS, FREEZE_AMPLIFIER, false, false));
		Sounds.playAt(this, ShadeletModule.SCREAM_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
		victim.addEffect(new MobEffectInstance(ShadeletModule.SPOOKED, SPOOKED_TICKS, 0, false, true));
		this.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(victim.getX(), victim.getY() + victim.getBbHeight(), victim.getZ()));
		fleeTimer = FLEE_TICKS;
		this.setSprinting(false);
		chasing = false;
	}

	private static boolean jumpscaresPlayers() {
		return Config.flag("can_shadelet_jumpscare_player", true);
	}

	private static boolean notSpooked(LivingEntity entity) {
		return !entity.hasEffect(ShadeletModule.SPOOKED);
	}

	private static boolean isScareablePlayer(Player player) {
		return !player.isCreative() && !player.isSpectator() && notSpooked(player);
	}

	/** Entities of a kind within {@code range} of this one, nearest first. */
	private <E extends LivingEntity> java.util.List<E> nearby(ServerLevel level, Class<E> kind, double range, Predicate<E> filter) {
		Vec3 centre = this.position();
		return level.getEntitiesOfClass(kind, new AABB(centre, centre).inflate(range), filter).stream()
			.sorted(Comparator.comparingDouble(e -> e.distanceToSqr(centre)))
			.toList();
	}

	private Optional<Monster> nearestMonster(ServerLevel level) {
		return level.getEntitiesOfClass(Monster.class, AABB.ofSize(this.position(), MONSTER_SEARCH_SIZE, MONSTER_SEARCH_SIZE, MONSTER_SEARCH_SIZE), e -> true).stream()
			.min(Comparator.comparingDouble(this::distanceToSqr));
	}

	/** Unreachable in practice (it deals no damage and has no attack goal), kept from the original. */
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (target instanceof LivingEntity victim) {
			triggerAnim(Animations.ACTIONS, "scream");
			this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, FREEZE_AMPLIFIER, false, false));
			victim.addEffect(new MobEffectInstance(ShadeletModule.SPOOKED, SPOOKED_TICKS, 0, false, true));
		}
		return hit;
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND || sugarRush >= 0) {
			return super.mobInteract(player, hand);
		}
		ItemStack stack = player.getMainHandItem();
		ShadeletTreats.Treat treat = ShadeletTreats.forItem(stack);
		if (treat == null) {
			return super.mobInteract(player, hand);
		}
		if (this.level() instanceof ServerLevel level) {
			eat(level, player, stack, treat);
		}
		return InteractionResult.SUCCESS;
	}

	private void eat(ServerLevel level, Player player, ItemStack stack, ShadeletTreats.Treat treat) {
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack.getItem()), getX(), getY() + 0.5, getZ(), 7, 0.2, 0.2, 0.2, 0.05);
		if (treat.leftover() != null) {
			ItemStack leftover = new ItemStack(treat.leftover());
			if (!player.getInventory().add(leftover)) {
				player.spawnAtLocation(level, leftover);
			}
		}
		if (!player.hasInfiniteMaterials()) {
			stack.shrink(1);
		}
		Sounds.playAt(this, treat.sound(), SoundSource.NEUTRAL, 1.0F, 1.0F);
		sugarRush = treat.sugarRushTicks();
		spookTimer = 0;
		if (player instanceof ServerPlayer) {
			Advancements.award(player, MoreCritters.id("feed_shadelet"));
		}
	}

	/** Players' arrows, hands and most environmental harm do nothing; any hit still knocks it along the attacker's gaze. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		Entity attacker = source.getEntity();
		if (attacker != null) {
			this.setDeltaMovement(attacker.getLookAngle());
		}
		if (isImmuneTo(source)) {
			return false;
		}
		return super.hurtServer(level, source, amount);
	}

	static boolean isImmuneTo(DamageSource source) {
		return source.getDirectEntity() instanceof AbstractArrow
			|| source.getDirectEntity() instanceof Player
			|| source.is(DamageTypes.IN_FIRE)
			|| source.is(DamageTypes.FALL)
			|| source.is(DamageTypes.CACTUS)
			|| source.is(DamageTypes.DROWN)
			|| source.is(DamageTypes.LIGHTNING_BOLT)
			|| source.is(DamageTypeTags.IS_EXPLOSION)
			|| source.is(DamageTypes.TRIDENT)
			|| source.is(DamageTypes.FALLING_ANVIL)
			|| source.is(DamageTypes.DRAGON_BREATH)
			|| source.is(DamageTypes.WITHER)
			|| source.is(DamageTypes.WITHER_SKULL);
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).scale(SIZE_SCALE);
	}

	@Override
	public String textureName() {
		String name = this.getName().getString();
		return name.equals(LOLIPOP_NAME) || name.equals("lolipop") ? "shadelet_lolipop" : "shadelet";
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ShadeletModule.IDLE_SOUND;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ShadeletModule.HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ShadeletModule.DEATH_SOUND;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("SugarRush", sugarRush);
		output.putInt("SpookTimer", spookTimer);
		output.putInt("FleeTimer", fleeTimer);
		output.putBoolean("Chasing", chasing);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		sugarRush = input.getIntOr("SugarRush", 0);
		spookTimer = input.getIntOr("SpookTimer", 0);
		fleeTimer = input.getIntOr("FleeTimer", 0);
		chasing = input.getBooleanOr("Chasing", false);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(Animations.MOVEMENT, 2, (AnimationTest<ShadeletEntity> test) -> {
			if (!test.isMoving()) {
				return test.setAndContinue(IDLE);
			}
			return test.setAndContinue(this.isSprinting() ? RUN : WALK);
		}));
		controllers.add(Animations.actions(this, "scream", "idle_shiver", "anim_reset"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animations;
	}
}
