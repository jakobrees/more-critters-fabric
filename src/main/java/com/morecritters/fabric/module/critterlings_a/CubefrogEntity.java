package com.morecritters.fabric.module.critterlings_a;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.module.critterling_system.Critterling;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The cubefrog: a critterling that does not walk but hops. Every 3-10 seconds, when on the
 * ground and not dancing, it crouches, then leaps half a block up in a random direction with a
 * jump sound; it curls up in the air and lands with a little squash. It croaks now and then.
 */
public class CubefrogEntity extends Critterling {
	private static final int HOP_MIN = 60, HOP_MAX = 200;
	/** The original's starting value before the first spawn roll. */
	private static final int HOP_UNSET = 40;
	/** Ticks from the crouch to the leap, and from the leap to the jump sound. */
	private static final int CROUCH_TICKS = 5, SOUND_DELAY = 2;
	private static final double LEAP_UP = 0.5, LEAP_SIDEWAYS = 0.2;

	private static final String HOP_CONTROLLER = "hop";
	private static final String JUMP_START = "jump_start";
	private static final RawAnimation AIR = RawAnimation.begin().thenLoop("air");
	private static final RawAnimation JUMP_END = RawAnimation.begin().thenPlay("jump_end");

	/** Ticks until the next hop. */
	private int hopTimer = HOP_UNSET;
	/** Ticks until the crouched frog leaps; 0 when not crouching. */
	private int leapIn;
	/** Ticks until the jump sound after a leap; 0 when none is due. */
	private int jumpSoundIn;

	public CubefrogEntity(EntityType<? extends CubefrogEntity> type, Level level) {
		super(type, level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(2, new FloatGoal(this));
	}

	/** Pushed along the ground it rolls like a die. */
	@Override
	protected String walkAnimation() {
		return "roll";
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		this.hopTimer = Mth.nextInt(this.random, HOP_MIN, HOP_MAX);
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level() instanceof ServerLevel) hop();
	}

	/** Counts down to the next hop, then crouches, leaps and croaks the jump sound in turn. */
	private void hop() {
		if (--this.hopTimer <= 1) {
			this.hopTimer = Mth.nextInt(this.random, HOP_MIN, HOP_MAX);
			if (onGround() && !isDancing()) {
				triggerAnim(HOP_CONTROLLER, JUMP_START);
				this.leapIn = CROUCH_TICKS;
			}
		}
		if (this.leapIn > 0 && --this.leapIn == 0) {
			leap();
			this.jumpSoundIn = SOUND_DELAY;
		}
		if (this.jumpSoundIn > 0 && --this.jumpSoundIn == 0) {
			Sounds.playAt(this, CritterlingsAModule.CUBEFROG_JUMP);
		}
	}

	/** Turns to a random heading and jumps up with a small random sideways push (not along the heading). */
	private void leap() {
		float heading = this.random.nextFloat() * 360.0F;
		this.setYRot(heading);
		this.setXRot(0.0F);
		this.setYBodyRot(heading);
		this.setYHeadRot(heading);
		this.yRotO = heading;
		this.xRotO = 0.0F;
		this.yBodyRotO = heading;
		this.yHeadRotO = heading;
		this.setDeltaMovement(
			Mth.nextDouble(this.random, -LEAP_SIDEWAYS, LEAP_SIDEWAYS),
			LEAP_UP,
			Mth.nextDouble(this.random, -LEAP_SIDEWAYS, LEAP_SIDEWAYS));
	}

	// --- sounds -----------------------------------------------------------------------

	@Override
	protected SoundEvent getAmbientSound() {
		return CritterlingsAModule.CUBEFROG_IDLE;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return CritterlingsAModule.CUBEFROG_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return CritterlingsAModule.CUBEFROG_HURT;
	}

	// --- saving -----------------------------------------------------------------------

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("HopTimer", this.hopTimer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.hopTimer = input.getIntOr("HopTimer", HOP_UNSET);
	}

	// --- GeckoLib ---------------------------------------------------------------------

	/**
	 * Adds the hop controller: the crouch ({@code jump_start}) when the server triggers it,
	 * {@code air} while off the ground, and {@code jump_end} once on landing.
	 */
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		super.registerControllers(controllers);
		controllers.add(new AnimationController<CubefrogEntity>(HOP_CONTROLLER, 0, this::hopAnimation)
			.triggerableAnim(JUMP_START, RawAnimation.begin().thenPlayAndHold(JUMP_START))
			.receiveTriggeredAnimations());
	}

	private PlayState hopAnimation(AnimationTest<CubefrogEntity> test) {
		if (!onGround()) return test.setAndContinue(AIR);
		if (test.controller().isPlayingTriggeredAnimation()) return PlayState.CONTINUE;
		if (test.isCurrentAnimation(AIR)) return test.setAndContinue(JUMP_END);
		if (test.isCurrentAnimation(JUMP_END) && !test.controller().hasAnimationFinished()) return PlayState.CONTINUE;
		return PlayState.STOP;
	}
}
