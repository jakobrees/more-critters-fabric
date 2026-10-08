package com.morecritters.fabric.module.shriekbat;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.TextureVariants;
import java.util.Comparator;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A cave bat. Under a ceiling it clings upside down; in the open dark it flutters upward looking for
 * one. Every so often a hanging bat wakes and lets out three test shrieks below it ({@link TesterShriekEntity});
 * a player moving near one alarms the nearest bat for thirty seconds. An alarmed bat darts about and
 * every five seconds shrieks, setting every monster within twenty blocks that can see the nearest player on them.
 */
public class ShriekbatEntity extends Monster implements GeoEntity, TextureVariants {
	/** What the bat is doing; drives its animation, its texture and whether it wanders. */
	enum Pose { FLYING, HANGING, TESTING, FLYING_ANGRY, SHRIEKING }

	private static final EntityDataAccessor<Integer> POSE = SynchedEntityData.defineId(ShriekbatEntity.class, EntityDataSerializers.INT);
	private static final Set<String> MISTY_NAMES = Set.of("Misty", "misty", "MistyJam", "mistyjam", "Mistyjam");

	private static final int TEST_INTERVAL_MIN = 200, TEST_INTERVAL_MAX = 700;
	private static final int TEST_SHRIEK_DELAY = 42, TEST_SHRIEK_SPACING = 5, TEST_DURATION = 59;
	private static final int SHRIEK_INTERVAL = 100;
	private static final int FLAP_INTERVAL_MIN = 100, FLAP_INTERVAL_MAX = 200;
	static final int ALARM_DURATION = 600;
	private static final double SHRIEK_REACH = 20.0, PLAYER_SEARCH = 40.0;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private boolean alarmed;
	/** Countdowns, decremented every tick; each fires when it reaches one, as in the original. */
	private int alarmLeft, testTimer = 100, shriekTimer = 100, flapTimer;

	public ShriekbatEntity(EntityType<? extends ShriekbatEntity> type, Level level) {
		super(type, level);
		this.xpReward = 2;
		this.moveControl = new FlyingMoveControl(this, 10, true);
		this.setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 6.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 16.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FLYING_SPEED, 0.3);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(POSE, Pose.FLYING.ordinal());
	}

	Pose pose() {
		return Pose.values()[this.entityData.get(POSE)];
	}

	private void setPose(Pose pose) {
		this.entityData.set(POSE, pose.ordinal());
	}

	/** Whether a test shriek has caught someone moving and the bat is hunting about. */
	public boolean isAlarmed() {
		return this.alarmed;
	}

	/** Alarms the bat: it hunts about and shrieks for the next thirty seconds. */
	void alarm() {
		this.alarmed = true;
		this.alarmLeft = ALARM_DURATION;
	}

	@Override
	public String textureName() {
		boolean open = pose() == Pose.TESTING || pose() == Pose.SHRIEKING;
		boolean misty = MISTY_NAMES.contains(this.getDisplayName().getString());
		return "shriekbat" + (misty ? "_misty" : "") + (open ? "_open" : "");
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new FlyingPathNavigation(this, level);
	}

	/** Wanders through the air up to sixteen blocks in any direction, unless hanging or testing. */
	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new RandomStrollGoal(this, 0.8, 20) {
			@Override
			protected Vec3 getPosition() {
				var random = ShriekbatEntity.this.getRandom();
				return ShriekbatEntity.this.position().add((random.nextFloat() * 2 - 1) * 16, (random.nextFloat() * 2 - 1) * 16, (random.nextFloat() * 2 - 1) * 16);
			}

			@Override
			public boolean canUse() {
				return super.canUse() && isAwake();
			}

			@Override
			public boolean canContinueToUse() {
				return super.canContinueToUse() && isAwake();
			}
		});
		this.goalSelector.addGoal(2, new RandomLookAroundGoal(this) {
			@Override
			public boolean canUse() {
				return super.canUse() && isAwake();
			}

			@Override
			public boolean canContinueToUse() {
				return super.canContinueToUse() && isAwake();
			}
		});
	}

	private boolean isAwake() {
		return pose() != Pose.HANGING && pose() != Pose.TESTING;
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level,
			DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
		this.flapTimer = Mth.nextInt(this.getRandom(), 40, 60);
		return super.finalizeSpawn(level, difficulty, reason, data);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!(this.level() instanceof ServerLevel level)) return;
		perchOrFlutter(level);
		testTheAir(level);
		countDownAlarm();
		if (this.isInWall()) this.teleportTo(this.getX(), this.getY() - 1.0, this.getZ());
		shriekWhenAlarmed(level);
		dartWhenAlarmed();
	}

	/** Clings to a solid ceiling two blocks up; in the dark without one, flutters upwards flapping. */
	private void perchOrFlutter(ServerLevel level) {
		if (--this.flapTimer <= 0) this.flapTimer = Mth.nextInt(this.getRandom(), FLAP_INTERVAL_MIN, FLAP_INTERVAL_MAX);
		BlockPos pos = this.blockPosition();
		if (level.getBlockState(BlockPos.containing(this.getX(), this.getY() + 2.0, this.getZ())).canOcclude()) {
			if (this.alarmed) {
				this.setDeltaMovement(0.0, -0.5, 0.0);
			} else {
				if (pose() != Pose.TESTING) setPose(Pose.HANGING);
				this.setDeltaMovement(0.0, 1.0, 0.0);
			}
		} else if (!this.alarmed) {
			if (level.canSeeSkyFromBelowWater(pos)) {
				setPose(Pose.FLYING);
			} else {
				setPose(Pose.FLYING_ANGRY);
				Vec3 look = this.getLookAngle();
				this.setDeltaMovement(0.1 * look.x, 0.5, 0.1 * look.z);
				if (this.flapTimer <= 0) Sounds.playAt(this, ShriekbatModule.FLAP_SOUND, SoundSource.HOSTILE, 2.0F, 1.0F);
			}
		} else if (pose() == Pose.HANGING) {
			setPose(Pose.FLYING);
		}
	}

	/** Now and then a hanging bat wakes, cries, and drops three test shrieks below itself. */
	private void testTheAir(ServerLevel level) {
		if (--this.testTimer != 1) return;
		this.testTimer = Mth.nextInt(this.getRandom(), TEST_INTERVAL_MIN, TEST_INTERVAL_MAX);
		if (pose() != Pose.HANGING) return;
		setPose(Pose.TESTING);
		triggerAnim(Animations.ACTIONS, "idle_angry");
		Sounds.playAt(this, ShriekbatModule.TEST_SHRIEK_SOUND, SoundSource.HOSTILE, 3.0F, 1.0F);
		ServerScheduler.runLater(TEST_DURATION, () -> {
			if (pose() == Pose.TESTING) setPose(Pose.HANGING);
		});
		BlockPos below = BlockPos.containing(this.getX(), this.getY() - 1.0, this.getZ());
		for (int i = 0; i < 3; i++) {
			ServerScheduler.runLater(TEST_SHRIEK_DELAY + i * TEST_SHRIEK_SPACING, () -> {
				if (this.isAlive()) ShriekbatModule.TESTER_SHRIEK.spawn(level, below, EntitySpawnReason.MOB_SUMMONED);
			});
		}
	}

	private void countDownAlarm() {
		if (--this.alarmLeft == 1) this.alarmed = false;
	}

	/** Every five seconds while alarmed: a shriek that points the monsters around at the nearest player it can see. */
	private void shriekWhenAlarmed(ServerLevel level) {
		if (--this.shriekTimer != 1) return;
		this.shriekTimer = SHRIEK_INTERVAL;
		if (!this.alarmed) return;
		setPose(Pose.SHRIEKING);
		triggerAnim(Animations.ACTIONS, "shriek");
		level.sendParticles(ShriekbatModule.SHRIEK_PARTICLE, this.getX(), this.getY(), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		Sounds.playAt(this, ShriekbatModule.SHRIEK_SOUND, SoundSource.HOSTILE, 3.0F, 1.0F);
		Vec3 centre = this.position();
		Player nearest = level.getEntitiesOfClass(Player.class, AABB.ofSize(centre, PLAYER_SEARCH, PLAYER_SEARCH, PLAYER_SEARCH)).stream()
			.min(Comparator.comparingDouble(p -> p.distanceToSqr(centre))).orElse(null);
		if (nearest == null) return;
		for (Monster monster : level.getEntitiesOfClass(Monster.class, new AABB(centre, centre).inflate(SHRIEK_REACH))) {
			if (monster.hasLineOfSight(nearest)) monster.setTarget(nearest);
		}
	}

	/** An alarmed bat darts towards whatever lies two blocks ahead, bobbing at random. */
	private void dartWhenAlarmed() {
		if (this.alarmed) {
			Vec3 eyes = this.getEyePosition(1.0F);
			BlockPos ahead = this.level().clip(new ClipContext(eyes, eyes.add(this.getViewVector(1.0F).scale(2.0)),
				ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this)).getBlockPos();
			this.setDeltaMovement((ahead.getX() - this.getX()) / 3.0, Mth.nextDouble(this.getRandom(), -0.5, 0.5), (ahead.getZ() - this.getZ()) / 3.0);
			if (pose() != Pose.SHRIEKING) setPose(Pose.FLYING_ANGRY);
		} else if (pose() == Pose.FLYING_ANGRY || pose() == Pose.SHRIEKING) {
			ServerScheduler.runLater(2, () -> {
				if (!this.alarmed && (pose() == Pose.FLYING_ANGRY || pose() == Pose.SHRIEKING)) setPose(Pose.FLYING);
			});
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() instanceof Player player) Advancements.award(player, MoreCritters.id("encounter_shriekbat"));
		return super.hurtServer(level, source, amount);
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	public void setNoGravity(boolean ignored) {
		super.setNoGravity(true);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ShriekbatModule.IDLE_SOUND;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ShriekbatModule.HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ShriekbatModule.DEATH_SOUND;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Alarmed", this.alarmed);
		output.putInt("AlarmLeft", this.alarmLeft);
		output.putInt("TestTimer", this.testTimer);
		output.putInt("ShriekTimer", this.shriekTimer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.alarmed = input.getBooleanOr("Alarmed", false);
		this.alarmLeft = input.getIntOr("AlarmLeft", 0);
		this.testTimer = input.getIntOr("TestTimer", 100);
		this.shriekTimer = input.getIntOr("ShriekTimer", 100);
	}

	// --- GeckoLib: looping poses, with the cries played once on top -------------------------
	private static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");
	private static final RawAnimation HANG = RawAnimation.begin().thenLoop("idle_hang");
	private static final RawAnimation FLY_ANGRY = RawAnimation.begin().thenLoop("fly_angry");

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(Animations.MOVEMENT, 2, (AnimationTest<ShriekbatEntity> test) -> switch (pose()) {
			case FLYING -> test.setAndContinue(FLY);
			case HANGING -> test.setAndContinue(HANG);
			case FLYING_ANGRY -> test.setAndContinue(FLY_ANGRY);
			case TESTING, SHRIEKING -> PlayState.STOP;
		}));
		controllers.add(Animations.actions(this, "idle_angry", "shriek"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
