package com.morecritters.fabric.module.critterlings_c;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.module.critterling_system.Critterling;
import java.util.List;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The mothkid: a glowing moth critterling. Every 5-10 seconds, when on the ground and not
 * dancing, it turns to a random heading and flutters up into a short hop (fly sound, {@code fly}
 * animation while airborne, {@code reset} on landing). Otherwise it strolls on dry land, fidgets
 * and dances like any critterling, and unlike most it can drown.
 */
public class MothkidEntity extends Critterling {
	private static final String FLIGHT_CONTROLLER = "flight";
	private static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");

	/** The original's starting value; its spawn procedure left it alone, so the first flight comes after 2 s. */
	private static final int FIRST_FLIGHT = 40;
	private static final int FLIGHT_MIN = 100, FLIGHT_MAX = 200;
	private static final double LIFT = 0.5, DRIFT = 0.2;
	private static final int FLY_SOUND_DELAY = 2;

	/** Ticks until the next hop into the air. */
	private int flightTimer = FIRST_FLIGHT;
	private boolean wasAirborne;

	public MothkidEntity(EntityType<? extends MothkidEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return CommonTraits.common();
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, CommonTraits.dryWander(this, 0.8));
		this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(3, new FloatGoal(this));
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level().isClientSide()) return;
		if (--this.flightTimer <= 1) {
			this.flightTimer = Mth.nextInt(this.random, FLIGHT_MIN, FLIGHT_MAX);
			if (!isDancing() && this.onGround()) takeOff();
		}
		landing();
	}

	/** Faces a random way and flutters upwards with a little sideways drift; the wings buzz a moment later. */
	private void takeOff() {
		float heading = this.random.nextFloat() * 360.0F;
		this.setYRot(heading);
		this.setXRot(0.0F);
		this.setYBodyRot(heading);
		this.setYHeadRot(heading);
		this.yRotO = heading;
		this.xRotO = 0.0F;
		this.yBodyRotO = heading;
		this.yHeadRotO = heading;
		this.setDeltaMovement(new Vec3(Mth.nextDouble(this.random, -DRIFT, DRIFT), LIFT, Mth.nextDouble(this.random, -DRIFT, DRIFT)));
		ServerScheduler.runLater(FLY_SOUND_DELAY, () -> Sounds.playAt(this, CritterlingsCModule.MOTHKID_FLY));
	}

	/** Folds its wings ({@code reset}) when it touches down after being in the air. */
	private void landing() {
		boolean airborne = !this.onGround();
		if (this.wasAirborne && !airborne) playAction("reset");
		this.wasAirborne = airborne;
	}

	@Override
	protected List<String> extraActions() {
		return List.of("reset");
	}

	/** Adds the wing-flapping loop, which plays over the others while the mothkid is off the ground. */
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		super.registerControllers(controllers);
		controllers.add(new AnimationController<MothkidEntity>(FLIGHT_CONTROLLER, 2, test ->
			this.onGround() ? PlayState.STOP : test.setAndContinue(FLY)));
	}

	@Override
	protected boolean drowns() {
		return true;
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).scale(1.1F);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return CritterlingsCModule.MOTHKID_IDLE;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return CritterlingsCModule.MOTHKID_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return CritterlingsCModule.MOTHKID_DEATH;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("FlightTimer", this.flightTimer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.flightTimer = input.getIntOr("FlightTimer", FIRST_FLIGHT);
	}
}
