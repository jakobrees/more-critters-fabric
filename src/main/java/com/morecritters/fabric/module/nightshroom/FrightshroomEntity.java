package com.morecritters.fabric.module.nightshroom;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.ids.MightshroomIds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The hostile shroom an ancient skeleton becomes after a stew of death. It hunts players and
 * sheds rot whenever it strikes or is struck. Every five to seven seconds in a fight it either
 * blows: it stands still and, a second later, coughs up wave after wave of rot pieces that rain
 * down around it; or it stomps, and rot splashes erupt around its target. When it dies, every
 * rot zombie within 30 blocks crumbles. Takes no fall or drowning damage and never despawns.
 */
public class FrightshroomEntity extends Monster implements GeoEntity {
	private static final int ATTACK_MIN = 100, ATTACK_MAX = 150;
	private static final int BLOW_DELAY = 20, WAVE_TICKS = 5, FOLLOW_UP_WAVES = 5;
	private static final int STOMP_DELAY = 22, SPLASHES = 3;
	private static final double SPLASH_SCATTER = 8.0;
	private static final float ROT_PIECE_DAMAGE = 5.0F;
	private static final double ROT_ZOMBIE_BOND_RANGE = 30.0;
	private static final double ATTACK_REACH_SQR = 9.0;

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
	private static final RawAnimation CHASE = RawAnimation.begin().thenLoop("chase2");

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	/** Ticks until its next special attack, if it is fighting. */
	private int attackTimer;

	public FrightshroomEntity(EntityType<? extends FrightshroomEntity> type, Level level) {
		super(type, level);
		this.xpReward = 15;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 150.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 8.0)
			.add(Attributes.FOLLOW_RANGE, 32.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false) {
			@Override
			protected boolean canPerformAttack(LivingEntity target) {
				return this.isTimeToAttack() && this.mob.distanceToSqr(target) < ATTACK_REACH_SQR && this.mob.getSensing().hasLineOfSight(target);
			}
		});
		this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
		this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(6, new FloatGoal(this));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		this.attackTimer = Mth.nextInt(this.random, ATTACK_MIN, ATTACK_MAX);
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!(this.level() instanceof ServerLevel level)) return;
		if (--this.attackTimer == 1) {
			this.attackTimer = Mth.nextInt(this.random, ATTACK_MIN, ATTACK_MAX);
			if (this.getTarget() != null) {
				if (this.random.nextBoolean()) blow(level);
				else stomp(level);
			}
		}
	}

	/**
	 * Freezes, and a second later bursts two to four rot pieces from its cap. Each of those starts
	 * a volley of five more waves of one to four pieces, five ticks apart, bursting again on the
	 * second and fourth.
	 */
	private void blow(ServerLevel level) {
		this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 30, false, false));
		this.triggerAnim(Animations.ACTIONS, "blows");
		Vec3 feet = this.position();
		ServerScheduler.runLater(BLOW_DELAY, () -> {
			playBurst(level, feet);
			int firstWave = Mth.nextInt(this.random, 2, 4);
			for (int i = 0; i < firstWave; i++) {
				shootRotPiece(level, feet);
				ServerScheduler.runLater(WAVE_TICKS, () -> followUpWave(level, feet, 1));
			}
		});
	}

	private void followUpWave(ServerLevel level, Vec3 feet, int wave) {
		if (wave % 2 == 0) playBurst(level, feet);
		int pieces = Mth.nextInt(this.random, 1, 4);
		for (int i = 0; i < pieces; i++) shootRotPiece(level, feet);
		if (wave < FOLLOW_UP_WAVES) ServerScheduler.runLater(WAVE_TICKS, () -> followUpWave(level, feet, wave + 1));
	}

	private static void playBurst(ServerLevel level, Vec3 feet) {
		level.playSound(null, BlockPos.containing(feet), NightshroomModule.FRIGHTSHROOM_BURST_SOUND, SoundSource.HOSTILE, 2.0F, 1.0F);
	}

	/** A rot piece flung from three blocks up in a random, mostly upward direction. */
	private void shootRotPiece(ServerLevel level, Vec3 feet) {
		RotPieceEntity piece = RotPieceEntity.create(level, this, ROT_PIECE_DAMAGE, 0);
		piece.setPos(feet.x, feet.y + 3.0, feet.z);
		piece.shoot(Mth.nextDouble(this.random, -90.0, 90.0), Mth.nextDouble(this.random, 4.0, 45.0), Mth.nextDouble(this.random, -90.0, 90.0), 1.0F, 0.0F);
		level.addFreshEntity(piece);
	}

	/** Stamps the ground; a moment later three rot splashes come down within eight blocks of its target. */
	private void stomp(ServerLevel level) {
		OtherModules.sound(MightshroomIds.Sounds.ENTITY_MIGHTSHROOM_STOMP)
			.ifPresent(sound -> Sounds.playAt(this, sound, SoundSource.HOSTILE, 1.0F, 1.0F));
		this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 30, false, false));
		this.triggerAnim(Animations.ACTIONS, "stomp");
		double y = this.getY();
		ServerScheduler.runLater(STOMP_DELAY, () -> {
			LivingEntity target = this.getTarget();
			if (target == null) return;
			for (int i = 0; i < SPLASHES; i++) {
				BlockPos at = BlockPos.containing(
					target.getX() + Mth.nextDouble(this.random, -SPLASH_SCATTER, SPLASH_SCATTER), y,
					target.getZ() + Mth.nextDouble(this.random, -SPLASH_SCATTER, SPLASH_SCATTER));
				RotSplashEntity splash = NightshroomModule.ROT_SPLASH.spawn(level, at, EntitySpawnReason.MOB_SUMMONED);
				if (splash != null) splash.setDeltaMovement(0.0, 0.0, 0.0);
			}
		});
	}

	/** A puff of rot from its cap, when it strikes or is struck. */
	void shedRot(ServerLevel level) {
		Bursts.rot(level, this.position().add(0.0, 2.0, 0.0), 10, 0.5, 0.5, 0.5, 0.0);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		shedRot(level);
		if (source.is(DamageTypes.FALL) || source.is(DamageTypes.DROWN)) return false;
		return super.hurtServer(level, source, damage);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		this.triggerAnim(Animations.ACTIONS, "attack");
		return super.doHurtTarget(level, target);
	}

	/** The rot zombies it raised crumble away with it. */
	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!(this.level() instanceof ServerLevel level)) return;
		for (RotZombieEntity zombie : level.getEntitiesOfClass(RotZombieEntity.class, this.getBoundingBox().inflate(ROT_ZOMBIE_BOND_RANGE))) {
			zombie.discard();
			Bursts.rot(level, zombie.position().add(0.0, 1.0, 0.0), 25, 0.2, 0.5, 0.2, 0.05);
		}
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		OtherModules.sound(MightshroomIds.Sounds.ENTITY_MIGHTSHROOM_STEP).ifPresent(sound -> this.playSound(sound, 0.15F, 1.0F));
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override protected SoundEvent getAmbientSound() { return NightshroomModule.FRIGHTSHROOM_IDLE_SOUND; }
	@Override protected SoundEvent getHurtSound(DamageSource source) { return NightshroomModule.FRIGHTSHROOM_HURT_SOUND; }
	@Override protected SoundEvent getDeathSound() { return NightshroomModule.FRIGHTSHROOM_DEATH_SOUND; }

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("AttackTimer", this.attackTimer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.attackTimer = input.getIntOr("AttackTimer", ATTACK_MAX);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(Animations.MOVEMENT, 2, (AnimationTest<FrightshroomEntity> test) -> {
			if (!test.isMoving()) return test.setAndContinue(IDLE);
			return test.setAndContinue(this.isAggressive() ? CHASE : WALK);
		}));
		controllers.add(Animations.actions(this, "attack", "blows", "stomp"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
