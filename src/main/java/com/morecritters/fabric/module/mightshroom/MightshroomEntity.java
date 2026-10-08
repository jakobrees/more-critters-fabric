package com.morecritters.fabric.module.mightshroom;

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
import com.morecritters.fabric.ids.NightshroomIds;
import java.util.List;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The mightshroom: a five-block fungal colossus risen from an ancient skeleton. It screams when it finds a
 * target, making it tremble. Every 5 to 10 seconds in a fight it either stomps (the ground around it shakes
 * and up to four fungal zombies burst out of it) or leaps at its target, crashing down with a shock wave
 * that leaves an echo zapping everything nearby. It never takes fall or drowning damage and never despawns.
 */
public class MightshroomEntity extends Monster implements GeoEntity {
	private static final int SPECIAL_MIN = 100, SPECIAL_MAX = 200;
	/** Slowness strong enough to root it in place while it winds up. */
	private static final int ROOTED = 30;
	private static final double MELEE_REACH_SQR = 9.0;
	private static final double SHOCKWAVE_RANGE = 15.0;
	private static final double ZOMBIE_LAUNCH_RANGE = 3.0;
	private static final double LEAP_FORWARD = 2.0, LEAP_UP = 1.5;
	private static final List<int[]> STOMP_CORNERS = List.of(new int[] {1, 1}, new int[] {-1, 1}, new int[] {1, -1}, new int[] {-1, -1});

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
	private static final RawAnimation CHASE = RawAnimation.begin().thenLoop("chase");

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	/** Ticks until the next stomp or leap. */
	private int specialTimer;
	/** Winding up a stomp: no melee meanwhile. */
	private boolean stunned;
	/** Had a target last tick; a new target is greeted with a scream. */
	private boolean hadTarget;
	/** In the air after a leap; landing makes the crash. */
	private boolean leaping;

	public MightshroomEntity(EntityType<? extends MightshroomEntity> type, Level level) {
		super(type, level);
		this.xpReward = 15;
		this.setPersistenceRequired();
		this.specialTimer = nextSpecialDelay();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 200.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 5.0)
			.add(Attributes.FOLLOW_RANGE, 32.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 2.0, false) {
			@Override
			protected boolean canPerformAttack(LivingEntity target) {
				return this.isTimeToAttack() && this.mob.distanceToSqr(target) < MELEE_REACH_SQR && this.mob.getSensing().hasLineOfSight(target);
			}

			@Override
			public boolean canUse() {
				return !stunned && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return !stunned && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
		this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(6, new FloatGoal(this));
	}

	private int nextSpecialDelay() {
		return Mth.nextInt(this.random, SPECIAL_MIN, SPECIAL_MAX);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level() instanceof ServerLevel level) {
			tickSpecialAttack(level);
			tickTargetScream();
			tickLanding(level);
		}
	}

	/** Every 100-200 ticks, with a target: one time in three a stomp, otherwise a leap. */
	private void tickSpecialAttack(ServerLevel level) {
		if (--specialTimer > 1) {
			return;
		}
		specialTimer = nextSpecialDelay();
		if (!(getTarget() instanceof LivingEntity)) {
			return;
		}
		if (this.random.nextInt(3) == 0) {
			stomp(level);
		} else {
			leap();
		}
	}

	/**
	 * Roots itself, stamps, and a second later the ground shakes: everything within fifteen blocks trembles,
	 * and from each solid block at its four diagonal corners a fungal zombie bursts up in a spray of mycelium.
	 */
	private void stomp(ServerLevel level) {
		root(40);
		this.triggerAnim(Animations.ACTIONS, "stomp");
		stunned = true;
		ServerScheduler.runLater(20, () -> stunned = false);

		Vec3 at = position();
		List<LivingEntity> shaken = level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(SHOCKWAVE_RANGE));
		ServerScheduler.runLater(22, () -> shaken.forEach(entity -> tremble(entity, 10)));
		ServerScheduler.runLater(21, () -> {
			level.playSound(null, BlockPos.containing(at), MightshroomModule.STOMP_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
			for (int[] corner : STOMP_CORNERS) {
				raiseFungalZombie(level, at.x + corner[0], at.y, at.z + corner[1]);
			}
			for (Entity zombie : level.getEntitiesOfClass(Entity.class, new AABB(at, at).inflate(ZOMBIE_LAUNCH_RANGE),
					entity -> OtherModules.isOfType(entity, NightshroomIds.Entities.FUNGAL_ZOMBIE))) {
				zombie.setDeltaMovement(0.0, 0.7, 0.0);
			}
		});
	}

	/** A fungal zombie spawns inside the ground block (it is launched out right after), as in the original. */
	private static void raiseFungalZombie(ServerLevel level, double x, double y, double z) {
		BlockPos ground = BlockPos.containing(x, y - 1.0, z);
		if (!level.getBlockState(ground).canOcclude()) {
			return;
		}
		OtherModules.spawn(level, NightshroomIds.Entities.FUNGAL_ZOMBIE, ground).ifPresent(zombie -> zombie.setDeltaMovement(0.0, 0.0, 0.0));
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MYCELIUM.defaultBlockState()), x, y, z, 55, 0.2, 0.0, 0.2, 0.05);
	}

	/** Crouches for half a second, then jumps high at its target. */
	private void leap() {
		root(10);
		this.triggerAnim(Animations.ACTIONS, "jump_start");
		playHostileSound(MightshroomModule.LEAP_READY_SOUND);
		BlockPos takeOff = blockPosition();
		ServerScheduler.runLater(10, () -> {
			LivingEntity target = getTarget();
			if (target != null) {
				lookAt(EntityAnchorArgument.Anchor.EYES, target.position());
			}
			Vec3 look = getLookAngle();
			setDeltaMovement(look.x * LEAP_FORWARD, LEAP_UP, look.z * LEAP_FORWARD);
			level().playSound(null, takeOff, MightshroomModule.LEAP_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
			ServerScheduler.runLater(1, () -> {
				leaping = true;
				this.triggerAnim(Animations.ACTIONS, "air");
			});
		});
	}

	/** Landing from a leap: a crash, a zapping echo where it lands, and everything within fifteen blocks trembles. */
	private void tickLanding(ServerLevel level) {
		if (!leaping || !onGround()) {
			return;
		}
		leaping = false;
		root(20);
		this.triggerAnim(Animations.ACTIONS, "jump_end");
		level.playSound(null, blockPosition(), MightshroomModule.CRASH_SOUND, SoundSource.HOSTILE, 2.0F, 1.0F);
		EchoEntity echo = MightshroomModule.MIGHTSHROOM_ECHO.spawn(level, blockPosition(), EntitySpawnReason.MOB_SUMMONED);
		if (echo != null) {
			echo.setDeltaMovement(0.0, 0.0, 0.0);
		}
		List<LivingEntity> shaken = level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(SHOCKWAVE_RANGE));
		ServerScheduler.runLater(2, () -> shaken.forEach(entity -> tremble(entity, 10)));
	}

	/** On finding a new target it stops, screams, and the target trembles. */
	private void tickTargetScream() {
		boolean hasTarget = getTarget() instanceof LivingEntity;
		if (hasTarget && !hadTarget) {
			root(40);
			this.triggerAnim(Animations.ACTIONS, "scream");
			BlockPos at = blockPosition();
			ServerScheduler.runLater(10, () -> {
				if (getTarget() instanceof LivingEntity target) {
					tremble(target, 30);
				}
				level().playSound(null, at, MightshroomModule.SCREAM_SOUND, SoundSource.HOSTILE, 2.0F, 1.0F);
			});
		}
		hadTarget = hasTarget;
	}

	/** Started by the Spawn Mightshroom effect. */
	void transform() {
		this.triggerAnim(Animations.ACTIONS, "transform");
		level().playSound(null, blockPosition(), MightshroomModule.TRANSFORM_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
	}

	private void root(int ticks) {
		this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, ROOTED, false, false));
	}

	private static void tremble(LivingEntity entity, int ticks) {
		entity.addEffect(new MobEffectInstance(MightshroomModule.TREMBLE, ticks, 0, false, false));
	}

	private void playHostileSound(SoundEvent sound) {
		level().playSound(null, blockPosition(), sound, SoundSource.HOSTILE, 1.0F, 1.0F);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		this.triggerAnim(Animations.ACTIONS, "attack");
		level.playSound(null, target.blockPosition(), MightshroomModule.ATTACK_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
		return super.doHurtTarget(level, target);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypes.FALL) || source.is(DamageTypes.DROWN)) {
			return false;
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (source.getEntity() != null) {
			Advancements.award(source.getEntity(), MoreCritters.id("kill_mightshroom"));
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return MightshroomModule.IDLE_SOUND;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(MightshroomModule.STEP_SOUND, 0.15F, 1.0F);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return MightshroomModule.HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return MightshroomModule.DEATH_SOUND;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("SpecialTimer", specialTimer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		specialTimer = input.getIntOr("SpecialTimer", nextSpecialDelay());
	}

	/** Walks when wandering, runs ("chase") when hunting, idles when still; special moves play over it. */
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<MightshroomEntity>(Animations.MOVEMENT, 1, (AnimationTest<MightshroomEntity> test) -> {
			if (!test.isMoving()) {
				return test.setAndContinue(IDLE);
			}
			return test.setAndContinue(isAggressive() ? CHASE : WALK);
		}));
		controllers.add(Animations.actions(this, "attack", "scream", "stomp", "jump_start", "air", "jump_end", "transform"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
