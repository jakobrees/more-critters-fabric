package com.morecritters.fabric.module.nauticrawl;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.DryingOut;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.SwimmingFish;
import com.morecritters.fabric.core.TextureVariants;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The nauticrawl: a shelled, tentacled sea creature that drifts along the bottom of warm
 * oceans and swims in strokes. Struck, it squirts ink and fights back; while it has a target
 * it now and then curls into its shell and rolls straight at it, trailing bubbles. Its shell
 * cracks below half health; named "Argonaut" it wears a different shell. One in twenty rises
 * from the dead as a zombie nauticrawl instead. Dries out on land.
 */
public class NauticrawlEntity extends Monster implements GeoEntity, TextureVariants {
	private static final EntityDataAccessor<String> POSE = SynchedEntityData.defineId(NauticrawlEntity.class, EntityDataSerializers.STRING);
	static final String LAND = "land", SWIM = "swim", SWIM_START = "swim_start", SWIM_MAD = "swim_mad", SWIM_START_MAD = "swim_start_mad",
		ROLL_START = "roll_start", ROLL = "roll", ROLL_END = "roll_end";

	private static final double SWIM_SPEED = 0.6;
	private static final int ZOMBIE_ONE_IN = 20;
	private static final int DIVE_INTERVAL = 100;
	private static final int ROLL_CHECK_MIN = 100, ROLL_CHECK_MAX = 200;
	private static final int ROLL_MIN = 30, ROLL_MAX = 60;
	private static final int ROLL_START_TICKS = 10;
	private static final int STROKE_WHILE_HUNTING = 20, STROKE_MIN = 60, STROKE_MAX = 100;
	private static final int STROKE_WINDUP_TICKS = 7;

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final DryingOut air = new DryingOut(this);
	private int rollCheckTimer = ROLL_CHECK_MIN;
	private int rollTimer;
	private int strokeTimer = STROKE_WHILE_HUNTING;
	private int diveTimer = DIVE_INTERVAL;

	public NauticrawlEntity(EntityType<? extends NauticrawlEntity> type, Level level) {
		super(type, level);
		this.xpReward = 2;
		this.setPathfindingMalus(PathType.WATER, 0.0F);
		this.moveControl = SwimmingFish.setUp(this);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.6)
			.add(Attributes.MAX_HEALTH, 50.0)
			.add(Attributes.ATTACK_DAMAGE, 5.0)
			.add(Attributes.FOLLOW_RANGE, 100.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.2)
			.add(Attributes.STEP_HEIGHT, 0.6);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(POSE, "");
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return SwimmingFish.navigation(this, level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false));
		this.goalSelector.addGoal(2, new RandomSwimmingGoal(this, 1.0, 40));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return NauticrawlModule.IDLE_SOUND;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return NauticrawlModule.HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return NauticrawlModule.DEATH_SOUND;
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	/** Struck by someone, it squirts ink; a player meeting one this way earns the encounter advancement. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() != null) {
			level.sendParticles(ParticleTypes.SQUID_INK, true, false, getX(), getY(), getZ(),
				Mth.nextInt(getRandom(), 3, 5), 0.0, 0.0, 0.0, 0.1);
			if (source.getEntity() instanceof ServerPlayer player) {
				Advancements.award(player, MoreCritters.id("encounter_nauticrawl"));
			}
		}
		return !source.is(DamageTypes.DROWN) && super.hurtServer(level, source, amount);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit) {
			triggerAnim(Animations.ACTIONS, "attack");
			Sounds.playAt(this, NauticrawlModule.ATTACK_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
		}
		return hit;
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData data) {
		SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
		rollCheckTimer = Mth.nextInt(getRandom(), ROLL_CHECK_MIN, ROLL_CHECK_MAX);
		onFirstSpawn(level.getLevel());
		return result;
	}

	/** One nauticrawl in twenty is replaced by a zombie nauticrawl on the spot. */
	protected void onFirstSpawn(ServerLevel level) {
		if (getRandom().nextInt(ZOMBIE_ONE_IN) != 0) return;
		discard();
		NauticrawlModule.ZOMBIE_NAUTICRAWL.spawn(level, blockPosition(), EntitySpawnReason.MOB_SUMMONED);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!(level() instanceof ServerLevel level)) return;
		countDown();
		sinkAndDive();
		roll(level);
		swimStrokes();
		if (isInWater()) {
			if (pose().equals(LAND)) setPose(SWIM);
		} else {
			setPose(LAND);
		}
		air.tick(level);
	}

	private void countDown() {
		rollCheckTimer--;
		rollTimer--;
		strokeTimer--;
		diveTimer--;
	}

	/** Idle in water it slowly sinks, and every five seconds near the surface it dives. */
	private void sinkAndDive() {
		if (!isInWater() || hasTarget()) return;
		push(0.0, -0.001, 0.0);
		if (diveTimer < 0) {
			diveTimer = DIVE_INTERVAL;
			if (level().getBlockState(blockPosition().above()).isAir()) {
				Vec3 look = getLookAngle();
				setDeltaMovement(look.x * 0.4, -0.3, look.z * 0.4);
			}
		}
	}

	/** Every five to ten seconds, hunting in water, it may curl up and roll at its target for a few seconds. */
	private void roll(ServerLevel level) {
		if (rollCheckTimer < 0) {
			rollCheckTimer = Mth.nextInt(getRandom(), ROLL_CHECK_MIN, ROLL_CHECK_MAX);
			if (rollTimer < 0 && hasTarget() && isInWater()) {
				rollTimer = Mth.nextInt(getRandom(), ROLL_MIN, ROLL_MAX);
			}
		}
		if (rollTimer > 0) {
			if (!pose().equals(ROLL_START) && !pose().equals(ROLL)) {
				setPose(ROLL_START);
				ServerScheduler.runLater(ROLL_START_TICKS, () -> {
					if (pose().equals(ROLL_START)) setPose(ROLL);
				});
			}
			LivingEntity target = getTarget();
			if (target != null) lookAt(EntityAnchorArgument.Anchor.EYES, target.position());
			if (isInWater()) setDeltaMovement(getLookAngle().scale(0.6));
		} else if (pose().equals(ROLL)) {
			setPose(ROLL_END);
			strokeTimer = STROKE_WHILE_HUNTING;
		}
		if (pose().equals(ROLL)) {
			level.sendParticles(ParticleTypes.BUBBLE, true, false, getX(), getY(), getZ(), 5, 0.8, 0.8, 0.8, 0.1);
		}
	}

	/** Between rolls it swims in strokes: every second while hunting, every three to five seconds otherwise. */
	private void swimStrokes() {
		if (strokeTimer > -1) return;
		if (isInWater() && !pose().equals(ROLL) && !pose().equals(ROLL_START)) {
			addEffect(new MobEffectInstance(NauticrawlModule.SWIMMER, 1, 0, false, false));
		}
		strokeTimer = hasTarget() ? STROKE_WHILE_HUNTING : Mth.nextInt(getRandom(), STROKE_MIN, STROKE_MAX);
	}

	/**
	 * One swim stroke (what the swimmer effect does to a nauticrawl), unless it is within two blocks
	 * of the surface: hunting, it winds up and lunges along its gaze; otherwise it dips, then kicks
	 * forward and up.
	 */
	void swimStroke() {
		if (level().getBlockState(blockPosition().above(2)).isAir()) return;
		if (hasTarget()) {
			setPose(SWIM_START_MAD);
			ServerScheduler.runLater(STROKE_WINDUP_TICKS, () -> {
				setDeltaMovement(getLookAngle());
				setPose(SWIM_MAD);
			});
		} else {
			setPose(SWIM_START);
			setDeltaMovement(0.0, -0.3, 0.0);
			ServerScheduler.runLater(STROKE_WINDUP_TICKS, () -> {
				setPose(SWIM);
				Vec3 look = getLookAngle();
				setDeltaMovement(look.x * 0.6, 0.5, look.z * 0.6);
			});
		}
	}

	private boolean hasTarget() {
		return getTarget() != null;
	}

	private String pose() {
		return entityData.get(POSE);
	}

	private void setPose(String pose) {
		entityData.set(POSE, pose);
	}

	@Override
	protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
		SwimmingFish.swimFaster(this, SWIM_SPEED, input);
		super.travelInWater(input, baseGravity, isFalling, oldY);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		air.save(output);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		air.load(input);
	}

	@Override
	public String textureName() {
		String shell = hasCustomName() && getCustomName().getString().equalsIgnoreCase("argonaut") ? "nauticrawl_argonaut" : "nauticrawl";
		return isCracked() ? shell + "_cracked" : shell;
	}

	protected boolean isCracked() {
		return getHealth() <= getMaxHealth() / 2.0F;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		RawAnimation idle = RawAnimation.begin().thenLoop("idle");
		controllers.add(new AnimationController<NauticrawlEntity>(Animations.MOVEMENT, 4, test -> {
			String pose = pose();
			return test.setAndContinue(pose.isEmpty() ? idle : RawAnimation.begin().thenPlay(pose));
		}));
		controllers.add(Animations.actions(this, "attack"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
