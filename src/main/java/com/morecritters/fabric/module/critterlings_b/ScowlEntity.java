package com.morecritters.fabric.module.critterlings_b;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.module.critterling_system.Critterling;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A grumpy little flier that never walks. Every 3-10 seconds (unless dancing) it turns to a
 * random direction and hops into the air with a flap, then drifts down under slow falling,
 * playing its {@code air} loop until it lands ({@code air_end}).
 */
public class ScowlEntity extends Critterling {
	private static final int FIRST_HOP = 40, HOP_MIN = 60, HOP_MAX = 200;
	private static final double HOP_UP = 0.5, HOP_SIDEWAYS = 0.2;
	private static final int SLOW_FALL_TICKS = 20;
	private static final String AIR = "air", AIR_END = "air_end";
	private static final RawAnimation AIR_LOOP = RawAnimation.begin().thenLoop(AIR);

	/** Ticks until the next hop. */
	private int hopTimer = FIRST_HOP;
	private boolean airborne;

	public ScowlEntity(EntityType<? extends ScowlEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 3.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	/** As the original's spawn procedure: the first hop comes 60-200 ticks after spawning, like every later one. */
	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		this.hopTimer = Mth.nextInt(this.random, HOP_MIN, HOP_MAX);
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(2, new FloatGoal(this));
	}

	@Override
	protected List<String> extraActions() {
		return List.of(AIR_END);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!(this.level() instanceof ServerLevel)) return;
		if (--this.hopTimer <= 1) {
			this.hopTimer = Mth.nextInt(this.random, HOP_MIN, HOP_MAX);
			if (!isDancing()) hop();
		}
		fallSlowly();
	}

	/** Faces a random way and springs up with a flap. */
	private void hop() {
		Sounds.playAt(this, CritterlingsBModule.SCOWL_FLY);
		float yaw = (float) Mth.nextDouble(this.random, 0.0, 360.0);
		this.setYRot(yaw);
		this.setXRot(0.0F);
		this.setYBodyRot(yaw);
		this.setYHeadRot(yaw);
		this.yRotO = yaw;
		this.xRotO = 0.0F;
		this.yBodyRotO = yaw;
		this.yHeadRotO = yaw;
		this.setDeltaMovement(
			Mth.nextDouble(this.random, -HOP_SIDEWAYS, HOP_SIDEWAYS),
			HOP_UP,
			Mth.nextDouble(this.random, -HOP_SIDEWAYS, HOP_SIDEWAYS));
	}

	/** Glides down while in the air and plays the landing animation when it touches down. */
	private void fallSlowly() {
		if (!this.onGround()) {
			this.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALL_TICKS, 0, false, false));
			this.airborne = true;
		} else if (this.airborne) {
			this.airborne = false;
			playAction(AIR_END);
		}
	}

	/** Adds the {@code air} loop, which overrides the others while the scowl is off the ground. */
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		super.registerControllers(controllers);
		controllers.add(new AnimationController<ScowlEntity>(AIR, 0, test ->
			this.onGround() ? PlayState.STOP : test.setAndContinue(AIR_LOOP)));
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return CritterlingsBModule.SCOWL_IDLE;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return CritterlingsBModule.SCOWL_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return CritterlingsBModule.SCOWL_HURT;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("HopTimer", this.hopTimer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.hopTimer = input.getIntOr("HopTimer", FIRST_HOP);
	}
}
