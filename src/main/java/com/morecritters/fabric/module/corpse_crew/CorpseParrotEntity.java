package com.morecritters.fabric.module.corpse_crew;

import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.AnimationTest;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The crew's undead parrot. It flies without gravity, keeps watch over 20 blocks, pecks at its
 * target and drops pebbles on it from the air; landed, it sits tight and now and then hops back
 * into flight. It takes no fall or explosion damage (but can drown). The captain sends it after
 * his targets and calls it back when at peace.
 */
public class CorpseParrotEntity extends CorpseCrewMember {
	private static final double WATCH_RANGE = 20.0;
	private static final double PECK_REACH_SQR = 1.0;

	private final ParrotHabits<CorpseParrotEntity> habits = new ParrotHabits<>(this, SoundSource.HOSTILE);

	public CorpseParrotEntity(EntityType<? extends CorpseParrotEntity> type, Level level) {
		super(type, level, 1);
		this.moveControl = new FlyingMoveControl<>(this, 10, true);
		this.setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
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

	/** Chases and wanders twice as fast in the air as on the ground. */
	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, peck(2.0, false));
		this.goalSelector.addGoal(2, peck(1.0, true));
		this.goalSelector.addGoal(3, new ParrotFlight(this, 2.0, () -> !this.onGround()));
		this.goalSelector.addGoal(4, new ParrotFlight(this, 1.0, this::onGround));
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(6, new FloatGoal(this));
	}

	private MeleeAttackGoal peck(double speed, boolean grounded) {
		return new MeleeAttackGoal(this, speed, false) {
			@Override
			protected boolean canPerformAttack(LivingEntity target) {
				return this.isTimeToAttack() && this.mob.distanceToSqr(target) < PECK_REACH_SQR && this.mob.getSensing().hasLineOfSight(target);
			}

			@Override public boolean canUse() { return super.canUse() && onGround() == grounded; }
			@Override public boolean canContinueToUse() { return super.canContinueToUse() && onGround() == grounded; }
		};
	}

	@Override
	protected double lookoutRange() {
		return WATCH_RANGE;
	}

	@Override
	protected int firstSpeechDelay() {
		return Mth.nextInt(this.random, 100, 400);
	}

	@Override
	protected int songOdds() {
		return 20;
	}

	@Override
	protected void crewTick(ServerLevel level) {
		this.habits.tick();
	}

	@Override
	public void aiStep() {
		super.aiStep();
		this.setNoGravity(true);
	}

	@Override
	protected boolean isImmuneTo(DamageSource source) {
		return source.is(DamageTypes.FALL) || source.is(DamageTypes.EXPLOSION);
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
		return false;
	}

	@Override public @Nullable SoundEvent attackSound() { return CorpseCrewModule.PARROT_ATTACK; }
	@Override protected @Nullable String strikeAnimation() { return "attack"; }
	@Override protected @Nullable SoundEvent speechSound() { return CorpseCrewModule.PARROT_SPEECH; }
	@Override protected @Nullable SoundEvent songSound() { return CorpseCrewModule.PARROT_SING; }
	@Override protected SoundEvent getAmbientSound() { return CorpseCrewModule.PARROT_IDLE; }
	@Override protected SoundEvent getHurtSound(DamageSource source) { return CorpseCrewModule.PARROT_HURT; }
	@Override protected SoundEvent getDeathSound() { return CorpseCrewModule.PARROT_DEATH; }

	@Override
	protected String[] actionAnimations() {
		return new String[] {"attack", "throw", "ready"};
	}

	@Override
	protected RawAnimation movementAnimation(AnimationTest<CorpseCrewMember> test) {
		return ParrotPoses.of(this, test.isMoving(), false);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		this.habits.save(output);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.habits.load(input);
	}
}
