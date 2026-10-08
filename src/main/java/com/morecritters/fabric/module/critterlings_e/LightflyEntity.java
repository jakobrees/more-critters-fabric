package com.morecritters.fabric.module.critterlings_e;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The lightfly: a glowing spark the nightshroom releases. It drifts about for five seconds, leaving
 * a trail of sparks, then fizzles out. Once it has a target (the nightshroom hands it one) it dives
 * at it ever faster, trailing smoke; when it hits, it burns out and sets its victim alight
 * (critterling_system's {@code LightflyStrike}). Weapons, fire, potions and most hazards do not
 * touch it.
 */
public class LightflyEntity extends Monster implements GeoEntity {
	private static final int LIFETIME = 100;
	private static final double DIVE_ACCELERATION = 0.03;
	/** Sparks of the trail appear where it was this many ticks ago, as the original's chained delays. */
	private static final int[] TRAIL_DELAYS = {3, 5, 7};
	private static final int SMOKE_DELAY = 3;
	private static final int HISTORY = 8;
	private static final double SPARK_SPEED = 1.0, SMOKE_SPEED = 0.01;
	private static final double ATTACK_REACH_SQR = 4.0;
	private static final double STROLL_RANGE = 16.0;
	private static final double SPAWN_LIFT = 1.0;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private int lifeTicks;
	/** Speed of the dive, growing every tick it has a target. */
	private double diveSpeed;
	/** Where it was and whether it was diving, for the last few ticks (index = lifeTicks modulo HISTORY). */
	private final Vec3[] pastPositions = new Vec3[HISTORY];
	private final boolean[] pastDiving = new boolean[HISTORY];

	public LightflyEntity(EntityType<? extends LightflyEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setPersistenceRequired();
		this.moveControl = new FlyingMoveControl(this, 10, true);
		this.setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 10.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 16.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FLYING_SPEED, 0.3);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new FlyingPathNavigation(this, level);
	}

	/** Bites a target within two blocks; otherwise flits to random spots up to sixteen blocks away in any direction. */
	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false) {
			@Override
			protected boolean canPerformAttack(LivingEntity target) {
				return this.isTimeToAttack() && this.mob.distanceToSqr(target) < ATTACK_REACH_SQR && this.mob.getSensing().hasLineOfSight(target);
			}
		});
		this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.8, 20) {
			@Override
			protected Vec3 getPosition() {
				return LightflyEntity.this.position().add(randomOffset(), randomOffset(), randomOffset());
			}
		});
	}

	private double randomOffset() {
		return (this.random.nextFloat() * 2.0F - 1.0F) * STROLL_RANGE;
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		this.setDeltaMovement(0.0, SPAWN_LIFT, 0.0);
		return data;
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!(this.level() instanceof ServerLevel level)) return;
		LivingEntity target = this.getTarget();
		if (target != null) dive(target);
		trail(level, target != null);
		if (++this.lifeTicks == LIFETIME) {
			this.discard();
			spark(level, ParticleTypes.WAX_OFF, this.position(), SPARK_SPEED);
		}
	}

	/** Heads straight for the target, a little faster every tick. */
	private void dive(LivingEntity target) {
		this.diveSpeed += DIVE_ACCELERATION;
		this.lookAt(EntityAnchorArgument.Anchor.EYES, target.position());
		this.setDeltaMovement(this.getLookAngle().scale(this.diveSpeed));
	}

	/** Sparks where it was a few ticks ago, and smoke a block above where it was diving. */
	private void trail(ServerLevel level, boolean diving) {
		int now = this.lifeTicks % HISTORY;
		this.pastPositions[now] = this.position();
		this.pastDiving[now] = diving;
		for (int delay : TRAIL_DELAYS) {
			Vec3 then = this.pastPositions[(now - delay + HISTORY) % HISTORY];
			if (then != null) spark(level, ParticleTypes.WAX_OFF, then, SPARK_SPEED);
		}
		int smokeTick = (now - SMOKE_DELAY + HISTORY) % HISTORY;
		Vec3 smokeAt = this.pastPositions[smokeTick];
		if (smokeAt != null && this.pastDiving[smokeTick]) spark(level, ParticleTypes.LARGE_SMOKE, smokeAt.add(0.0, 1.0, 0.0), SMOKE_SPEED);
	}

	/** One particle, seen from far away, as the original's forced {@code /particle} commands. */
	private static void spark(ServerLevel level, ParticleOptions particle, Vec3 at, double speed) {
		level.sendParticles(particle, true, false, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, speed);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		Entity direct = source.getDirectEntity();
		if (direct instanceof AbstractArrow || direct instanceof Player || direct instanceof AbstractThrownPotion || direct instanceof AreaEffectCloud) {
			return false;
		}
		if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.FALL) || source.is(DamageTypes.CACTUS) || source.is(DamageTypes.DROWN)
			|| source.is(DamageTypes.LIGHTNING_BOLT) || source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.TRIDENT)
			|| source.is(DamageTypes.FALLING_ANVIL) || source.is(DamageTypes.DRAGON_BREATH) || source.is(DamageTypes.WITHER)
			|| source.is(DamageTypes.WITHER_SKULL)) {
			return false;
		}
		return super.hurtServer(level, source, damage);
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
		return false;
	}

	@Override
	protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
	}

	@Override
	public void setNoGravity(boolean ignored) {
		super.setNoGravity(true);
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).scale(0.5F);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.GENERIC_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.GENERIC_DEATH;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Age", this.lifeTicks);
		output.putDouble("DiveSpeed", this.diveSpeed);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.lifeTicks = input.getIntOr("Age", 0);
		this.diveSpeed = input.getDoubleOr("DiveSpeed", 0.0);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(Animations.idleWalk(this, "idle", "idle"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
