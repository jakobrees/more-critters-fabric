package com.morecritters.fabric.module.dripper;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.TextureVariants;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
 * A dripstone-caves monster that hunts players. Every few seconds, when its target is more
 * than four blocks away, it leaps at it in a rolling arc, keeping its eyes on the target
 * until it lands. It takes no fall or stalagmite damage. Named "Murmurtal!" it wears a
 * different skin.
 */
public class DripperEntity extends Monster implements GeoEntity, TextureVariants {
	private static final int ROLL_DELAY_MIN = 60, ROLL_DELAY_MAX = 150;
	/** The target must be further than this before the dripper leaps. */
	private static final float LEAP_MIN_DISTANCE = 4.0F;
	private static final double LEAP_FORWARD_SPEED = 1.5, LEAP_UPWARD_SPEED = 0.8;
	/** Ticks after the leap before touching the ground counts as landing (it starts on the ground). */
	private static final int LIFT_OFF_TICKS = 3;
	/** The roll ends this many ticks after landing. */
	private static final int ROLL_END_DELAY = 2;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	/** Ticks until the next chance to leap. */
	private int rollTimer;
	/** Ticks since the current leap began, or -1 when not rolling. */
	private int rollTicks = -1;
	/** Ticks spent on the ground since the current roll landed. */
	private int landedTicks;

	public DripperEntity(EntityType<? extends DripperEntity> type, Level level) {
		super(type, level);
		this.xpReward = 3;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.4)
			.add(Attributes.MAX_HEALTH, 20.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 64.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false) {
			// The original's reach: closer than its own width squared plus the target's width.
			@Override
			protected boolean canPerformAttack(LivingEntity target) {
				float width = this.mob.getBbWidth();
				return this.isTimeToAttack()
					&& this.mob.distanceToSqr(target) < width * width + target.getBbWidth()
					&& this.mob.getSensing().hasLineOfSight(target);
			}
		});
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
		this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0));
		this.targetSelector.addGoal(4, new HurtByTargetGoal(this).setAlertOthers());
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(6, new FloatGoal(this));
	}

	/** The original checks the shown name, so both spellings with the exclamation mark count. */
	@Override
	public String textureName() {
		String name = this.getDisplayName().getString();
		return name.equals("murmurtal!") || name.equals("Murmurtal!") ? "dripper_murmurtal" : "dripper";
	}

	// --- lifecycle --------------------------------------------------------------------

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		this.rollTimer = nextRollDelay();
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!(this.level() instanceof ServerLevel)) return;

		if (--this.rollTimer <= 0) {
			this.rollTimer = nextRollDelay();
			LivingEntity target = this.getTarget();
			if (target != null && this.distanceTo(target) > LEAP_MIN_DISTANCE) leapAt(target);
		}
		if (this.rollTicks >= 0) tickRoll();
	}

	private int nextRollDelay() {
		return Mth.nextInt(this.random, ROLL_DELAY_MIN, ROLL_DELAY_MAX);
	}

	/** Launches itself forward and up towards the target, rolling through the air. */
	private void leapAt(LivingEntity target) {
		this.getNavigation().stop();
		Sounds.playAt(this, DripperModule.JUMP_SOUND, SoundSource.BLOCKS, 1.0F, 1.0F);
		this.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), 1.0);
		Vec3 look = this.getLookAngle();
		this.setDeltaMovement(look.x * LEAP_FORWARD_SPEED, LEAP_UPWARD_SPEED, look.z * LEAP_FORWARD_SPEED);
		this.triggerAnim(Animations.ACTIONS, "roll");
		this.rollTicks = 0;
		this.landedTicks = 0;
	}

	/** In the air it keeps facing the target; shortly after touching ground or water the roll ends. */
	private void tickRoll() {
		this.rollTicks++;
		LivingEntity target = this.getTarget();
		if (target != null) {
			this.getNavigation().stop();
			this.lookAt(EntityAnchorArgument.Anchor.EYES, target.position());
		}
		boolean landed = this.onGround() || this.isInWater();
		if (this.rollTicks > LIFT_OFF_TICKS && landed && ++this.landedTicks >= ROLL_END_DELAY) {
			this.rollTicks = -1;
			this.stopTriggeredAnim(Animations.ACTIONS, "roll");
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		this.triggerAnim(Animations.ACTIONS, "attack");
		return super.doHurtTarget(level, target);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypes.FALL) || source.is(DamageTypes.STALAGMITE)) return false;
		return super.hurtServer(level, source, amount);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("RollTimer", this.rollTimer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.rollTimer = input.getIntOr("RollTimer", nextRollDelay());
	}

	// --- vanilla hooks ----------------------------------------------------------------

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(DripperModule.STEP_SOUND, 0.15F, 1.0F);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return DripperModule.HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return DripperModule.DEATH_SOUND;
	}

	// --- GeckoLib ---------------------------------------------------------------------

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(Animations.idleWalk(this, "idle", "walk"));
		controllers.add(Animations.actions(this, "attack", "roll"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
