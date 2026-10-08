package com.morecritters.fabric.module.custodian;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.TextureVariants;
import com.morecritters.fabric.ids.MiscIds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A stone guardian of the deep dark. When it picks a target it opens up and charges its laser
 * for five seconds: a Warden is killed outright and leaves sculk essence, anything else makes it
 * blast every monster it can see within 25 blocks. Afterwards it shuts down, sinks and does not
 * move for ten minutes. Wardens cannot hurt it, and its own swipes do no damage.
 */
public class CustodianEntity extends PathfinderMob implements GeoEntity, TextureVariants {
	private static final EntityDataAccessor<Boolean> CLOSED = SynchedEntityData.defineId(CustodianEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> ANCIENT_LOOK = SynchedEntityData.defineId(CustodianEntity.class, EntityDataSerializers.BOOLEAN);

	private static final int CHARGE_TICKS = 100;
	private static final int SHUTDOWN_TICKS = 12_000;
	private static final int OPEN_UP_AT = 30;
	private static final double BLAST_RANGE = 25.0;
	private static final float BLAST_DAMAGE = 200.0F;
	private static final int BEAM_STEPS = 20;

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private boolean charging;
	private int chargeTimer;
	private int shutdownTimer;

	public CustodianEntity(EntityType<? extends CustodianEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			.add(Attributes.MAX_HEALTH, 150.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 32.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CLOSED, false);
		builder.define(ANCIENT_LOOK, false);
	}

	/** Shut down after firing: no AI, sinks, puffs smoke. */
	public boolean closed() {
		return this.entityData.get(CLOSED);
	}

	/** A custodian awakened from an ancient one keeps the ancient texture. */
	void setAncientLook() {
		this.entityData.set(ANCIENT_LOOK, true);
	}

	@Override
	public String textureName() {
		return this.entityData.get(ANCIENT_LOOK) ? "ancient_custodian" : "custodian";
	}

	@Override
	protected void registerGoals() {
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Warden.class, false, false) {
			@Override public boolean canUse() { return !closed() && super.canUse(); }
			@Override public boolean canContinueToUse() { return !closed() && super.canContinueToUse(); }
		});
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, false, false) {
			@Override public boolean canUse() { return !closed() && super.canUse(); }
			@Override public boolean canContinueToUse() { return !closed() && super.canContinueToUse(); }
		});
		this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 0.7, false) {
			@Override public boolean canUse() { return !closed() && super.canUse(); }
			@Override public boolean canContinueToUse() { return !closed() && super.canContinueToUse(); }
		});
		this.goalSelector.addGoal(4, new RandomStrollGoal(this, 0.7) {
			@Override public boolean canUse() { return !closed() && super.canUse(); }
			@Override public boolean canContinueToUse() { return !closed() && super.canContinueToUse(); }
		});
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this) {
			@Override public boolean canUse() { return !closed() && super.canUse(); }
			@Override public boolean canContinueToUse() { return !closed() && super.canContinueToUse(); }
		});
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (level() instanceof ServerLevel level) {
			tickLaser(level);
			tickShutdown(level);
		}
	}

	/** Opens up when a target appears, folds back if it is lost, fires when the charge is full. */
	private void tickLaser(ServerLevel level) {
		LivingEntity target = getTarget();
		if (target != null && !this.charging && !closed()) {
			startCharging(target instanceof Warden);
		}
		if (target == null && this.charging) {
			this.charging = false;
			triggerAnim(Animations.ACTIONS, "open_cancel");
		}
		if (this.charging) {
			holdStill(10);
		}
		this.chargeTimer--;
		if (this.chargeTimer == 1 && this.charging && target != null) {
			if (target instanceof Warden) {
				killWarden(level, target);
			} else {
				blastMonsters(level);
			}
			shutDown();
		}
	}

	private void startCharging(boolean atWarden) {
		Sounds.playAt(this, CustodianModule.LASER_START_SOUND, SoundSource.NEUTRAL, 5.0F, 1.0F);
		this.charging = true;
		this.chargeTimer = CHARGE_TICKS;
		triggerAnim(Animations.ACTIONS, atWarden ? "open2" : "open_small");
	}

	/** The original roots the custodian with Slowness 26 while it charges or flinches. */
	private void holdStill(int ticks) {
		addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 25, false, false));
	}

	private void killWarden(ServerLevel level, LivingEntity warden) {
		Sounds.playAt(this, CustodianModule.LASER_SHOOT_WARDEN_SOUND, SoundSource.NEUTRAL, 5.0F, 1.0F);
		Sounds.playAt(this, CustodianModule.CLOSE_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
		warden.spawnAtLocation(level, new ItemStack(CustodianModule.SCULK_ESSENCE));
		if (BuiltInRegistries.PARTICLE_TYPE.getValue(MiscIds.Particles.WARDEN_EXPLOSION) instanceof ParticleOptions explosion) {
			level.sendParticles(explosion, warden.getX(), warden.getY() + 1.0, warden.getZ(), 1, 0, 0, 0, 0);
		}
		drawBeamTo(level, warden);
		warden.setHealth(0.0F);
	}

	/** A line of laser and smoke from the custodian's eye to the target. */
	private void drawBeamTo(ServerLevel level, LivingEntity target) {
		Vec3 eye = new Vec3(getX(), getY() + getBbHeight() * 0.75 + 0.8, getZ());
		Vec3 toTarget = new Vec3(target.getX() - getX(), target.getY() - getY() + target.getBbHeight() * 0.75 - getBbHeight() * 0.75, target.getZ() - getZ());
		for (int step = 0; step < BEAM_STEPS; step++) {
			Vec3 point = eye.add(toTarget.scale(step * 0.05));
			level.sendParticles(CustodianModule.LASER_PARTICLE, point.x, point.y, point.z, 5, 0.05, 0.05, 0.05, 0.0);
			level.sendParticles(ParticleTypes.SMOKE, point.x, point.y, point.z, 5, 0.05, 0.05, 0.05, 0.0);
		}
	}

	private void blastMonsters(ServerLevel level) {
		Sounds.playAt(this, CustodianModule.SHOOT_SOUND, SoundSource.NEUTRAL, 5.0F, 1.0F);
		Sounds.playAt(this, CustodianModule.CLOSE_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
		for (Monster monster : level.getEntitiesOfClass(Monster.class, getBoundingBox().inflate(BLAST_RANGE), this::hasLineOfSight)) {
			monster.hurtServer(level, damageSources().magic(), BLAST_DAMAGE);
		}
	}

	private void shutDown() {
		this.charging = false;
		this.entityData.set(CLOSED, true);
		this.shutdownTimer = SHUTDOWN_TICKS;
	}

	/** While shut down it smokes and is pressed to the ground; it opens up again at the end. */
	private void tickShutdown(ServerLevel level) {
		if (closed()) {
			level.sendParticles(ParticleTypes.CLOUD, true, true, getX(), getY() + 2.5, getZ(), 1, 0, 0, 0, 0.012);
			setDeltaMovement(new Vec3(0.0, -2.0, 0.0));
		}
		this.shutdownTimer--;
		if (this.shutdownTimer == OPEN_UP_AT) {
			triggerAnim(Animations.ACTIONS, "openup");
		}
		if (this.shutdownTimer == 1) {
			this.entityData.set(CLOSED, false);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() instanceof Warden) {
			flinchFromWarden();
			return false;
		}
		return !isImmuneTo(source) && super.hurtServer(level, source, amount);
	}

	/** Shrugs a Warden's blow off with one of two hurt animations, unless busy or shut down. */
	private void flinchFromWarden() {
		if (this.charging || closed()) {
			return;
		}
		triggerAnim(Animations.ACTIONS, getRandom().nextBoolean() ? "hurt1" : "hurt2");
		holdStill(10);
	}

	/** Fire, arrows, potions, falls, cacti, drowning and anvils do not hurt a custodian. */
	static boolean isImmuneTo(DamageSource source) {
		Entity direct = source.getDirectEntity();
		return direct instanceof AbstractArrow || direct instanceof AbstractThrownPotion || direct instanceof AreaEffectCloud
			|| source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.FALL) || source.is(DamageTypes.CACTUS)
			|| source.is(DamageTypes.DROWN) || source.is(DamageTypes.FALLING_ANVIL);
	}

	/** The original cancels every direct hit a custodian deals, so its melee swipe is harmless. */
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		return false;
	}

	@Override
	public boolean canBeCollidedWith(@Nullable Entity other) {
		return isAlive();
	}

	@Override
	public boolean canCollideWith(Entity entity) {
		return true;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).scale(1.2F);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return CustodianModule.HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return CustodianModule.DEATH_SOUND;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Closed", closed());
		output.putBoolean("Charging", this.charging);
		output.putBoolean("AncientLook", this.entityData.get(ANCIENT_LOOK));
		output.putInt("ChargeTimer", this.chargeTimer);
		output.putInt("ShutdownTimer", this.shutdownTimer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.entityData.set(CLOSED, input.getBooleanOr("Closed", false));
		this.entityData.set(ANCIENT_LOOK, input.getBooleanOr("AncientLook", false));
		this.charging = input.getBooleanOr("Charging", false);
		this.chargeTimer = input.getIntOr("ChargeTimer", 0);
		this.shutdownTimer = input.getIntOr("ShutdownTimer", 0);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		RawAnimation idle = RawAnimation.begin().thenLoop("idle_open");
		RawAnimation walk = RawAnimation.begin().thenLoop("walk");
		RawAnimation shut = RawAnimation.begin().thenPlayAndHold("close");
		controllers.add(new AnimationController<CustodianEntity>(Animations.MOVEMENT, 2, test ->
			test.setAndContinue(closed() ? shut : test.isMoving() ? walk : idle)));
		controllers.add(Animations.actions(this, "open2", "open_small", "open_cancel", "openup", "hurt1", "hurt2"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}
}
