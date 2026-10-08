package com.morecritters.fabric.module.nervoid;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.TextureVariants;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The nervoid: a flying brain that hunts players. Every five seconds, if it sees its target, it
 * freezes to wind up, then rushes along its gaze for two seconds. A bite has a one in three chance
 * to possess the victim (Under Control); the nervoid vanishes into it and comes back out, wet, when
 * the victim dies. A wet nervoid drips spinal fluid that a glass bottle can collect. Hitting it
 * knocks it back and breaks off its rush.
 */
public class NervoidEntity extends Monster implements GeoEntity, TextureVariants {
	private static final EntityDataAccessor<Boolean> WET = SynchedEntityData.defineId(NervoidEntity.class, EntityDataSerializers.BOOLEAN);
	private static final int RUSH_INTERVAL = 100;
	private static final int WIND_UP_TICKS = 12;
	private static final int RUSH_TICKS = 40;
	private static final double RUSH_SPEED = 0.4;
	private static final double KNOCKBACK = -0.5;
	private static final int POSSESS_CHANCE = 3;
	private static final int POSSESSION_TICKS = 60;

	private enum Phase { NONE, WIND_UP, RUSH }

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private int rushCountdown = RUSH_INTERVAL;
	/** Ticks since the last wind-up began, or -1; runs on even if a hit breaks off the rush, as in the original. */
	private int rushClock = -1;
	private Phase phase = Phase.NONE;

	public NervoidEntity(EntityType<? extends NervoidEntity> type, Level level) {
		super(type, level);
		this.xpReward = 3;
		this.moveControl = new FlyingMoveControl(this, 10, true);
		setNoGravity(true);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.5)
			.add(Attributes.MAX_HEALTH, 30.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 64.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FLYING_SPEED, 0.5);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new FlyingPathNavigation(this, level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
		this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.8, 20) {
			/** Wanders anywhere within 16 blocks, up and down too. */
			@Override
			protected Vec3 getPosition() {
				return new Vec3(wanderOffset(getX()), wanderOffset(getY()), wanderOffset(getZ()));
			}
		});
		this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(6, new FloatGoal(this));
	}

	private double wanderOffset(double from) {
		return from + (this.random.nextFloat() * 2.0F - 1.0F) * 16.0F;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(WET, false);
	}

	public void setWet(boolean wet) {
		this.entityData.set(WET, wet);
	}

	public boolean isWet() {
		return this.entityData.get(WET);
	}

	/**
	 * A nervoid summoned out of a host (the custodian module's Under Control ending) comes out wet,
	 * as the original's come-out procedure set the wet texture on it.
	 */
	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		if (reason == EntitySpawnReason.MOB_SUMMONED) {
			setWet(true);
		}
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	@Override
	public String textureName() {
		return isWet() ? "nervoid_wet" : "nervoid";
	}

	// --- rush -------------------------------------------------------------------------

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level() instanceof ServerLevel level) {
			tickRush();
			dripSpinalFluid(level);
		}
	}

	private void tickRush() {
		if (--this.rushCountdown <= 0) {
			this.rushCountdown = RUSH_INTERVAL;
			LivingEntity target = getTarget();
			if (target != null && hasLineOfSight(target)) {
				startWindUp();
			}
		}
		if (this.rushClock >= 0) {
			this.rushClock++;
			if (this.rushClock == WIND_UP_TICKS) {
				this.phase = Phase.RUSH;
				triggerAnim(Animations.ACTIONS, "attack_rush");
				Sounds.playAt(this, NervoidModule.RUSH_START_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
			} else if (this.rushClock == WIND_UP_TICKS + RUSH_TICKS) {
				this.rushClock = -1;
				breakOffRush();
			}
		}
		switch (this.phase) {
			case WIND_UP -> setDeltaMovement(Vec3.ZERO);
			case RUSH -> setDeltaMovement(getLookAngle().scale(RUSH_SPEED));
			case NONE -> { }
		}
	}

	private void startWindUp() {
		this.phase = Phase.WIND_UP;
		this.rushClock = 0;
		triggerAnim(Animations.ACTIONS, "attack_start");
		Sounds.playAt(this, NervoidModule.RUSH_READY_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
	}

	private void breakOffRush() {
		this.phase = Phase.NONE;
		triggerAnim(Animations.ACTIONS, "animation_reseter");
	}

	/** A wet nervoid drips spinal fluid a third of the time. */
	private void dripSpinalFluid(ServerLevel level) {
		if (isWet() && this.random.nextInt(3) == 0) {
			level.sendParticles(NervoidModule.SPINAL_FLUID, getX(), getY() + 1.0, getZ(), 1, 0.5, 0.5, 0.5, 0.0);
		}
	}

	// --- combat -----------------------------------------------------------------------

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit) {
			breakOffRush();
			triggerAnim(Animations.ACTIONS, "attack");
			Sounds.playAt(this, NervoidModule.ATTACK_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
			if (target instanceof LivingEntity victim) {
				maybePossess(victim);
			}
		}
		return hit;
	}

	/** One bite in three slips the nervoid inside a victim not already under control. */
	private void maybePossess(LivingEntity victim) {
		Holder<MobEffect> underControl = NervoidModule.underControl();
		if (underControl == null || victim.hasEffect(underControl) || this.random.nextInt(POSSESS_CHANCE) != 0) {
			return;
		}
		victim.addEffect(new MobEffectInstance(underControl, POSSESSION_TICKS, 0, false, true));
		Sounds.playAt(victim, NervoidModule.POSSESS_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
		discard();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypes.FALL)) {
			return false;
		}
		if (source.getEntity() != null) {
			breakOffRush();
			setDeltaMovement(getLookAngle().scale(KNOCKBACK));
			Advancements.award(source.getEntity(), MoreCritters.id("encounter_nervoid"));
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
		return false;
	}

	/** A glass bottle draws spinal fluid from a wet nervoid. */
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (hand != InteractionHand.MAIN_HAND || !held.is(Items.GLASS_BOTTLE) || !isWet()) {
			return super.mobInteract(player, hand);
		}
		player.swing(hand, SwingAnimation.DEFAULT, true);
		if (this.level() instanceof ServerLevel) {
			Sounds.playAt(this, SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
			setWet(false);
			if (!player.hasInfiniteMaterials()) {
				held.shrink(1);
			}
			ItemStack fluid = new ItemStack(NervoidModule.spinalFluidBottle);
			if (!player.getInventory().add(fluid)) {
				player.spawnAtLocation((ServerLevel) this.level(), fluid);
			}
		}
		return InteractionResult.SUCCESS;
	}

	// --- sounds, saving, animation ----------------------------------------------------

	@Override
	protected SoundEvent getAmbientSound() {
		return NervoidModule.IDLE_SOUND;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return NervoidModule.HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return NervoidModule.DEATH_SOUND;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Wet", isWet());
		output.putInt("RushCountdown", this.rushCountdown);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setWet(input.getBooleanOr("Wet", false));
		this.rushCountdown = input.getIntOr("RushCountdown", RUSH_INTERVAL);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(Animations.idleWalk(this, "idle", "idle"));
		controllers.add(Animations.actions(this, "attack_start", "attack_rush", "attack", "animation_reseter"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
