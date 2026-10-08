package com.morecritters.fabric.module.stincarp;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.TextureVariants;
import com.morecritters.fabric.ids.ShockCubeIds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Bucketable;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * An electric river fish. Every five to ten seconds it discharges: it freezes in place, flashes
 * its skeleton and throws sparks. Hitting it while it is in water sets off a discharge at once
 * and leaves a shock cube behind. It dries out on land, cannot drown, shrugs off lightning,
 * and can be scooped up in a water bucket.
 */
public class StincarpEntity extends PathfinderMob implements GeoEntity, TextureVariants, Bucketable {
	/** Index into {@link #TEXTURES}; animated on the server and synced. */
	private static final EntityDataAccessor<Integer> TEXTURE = SynchedEntityData.defineId(StincarpEntity.class, EntityDataSerializers.INT);
	private static final String[] TEXTURES = {
		"stincarp", "stincarp_loop1", "stincarp_loop2", "stincarp_loop3", "stincarp_loop4", "stincarp_loop5",
		"stincarp_skeleton1", "stincarp_skeleton2"
	};
	private static final int PLAIN = 0, FIRST_LOOP = 1, LOOP_FRAMES = 5, SKELETON_1 = 6, SKELETON_2 = 7;
	/** The glow pattern moves on one frame every three ticks. */
	private static final int LOOP_FRAME_TICKS = 3;

	private static final int ZAP_INTERVAL_MIN = 100, ZAP_INTERVAL_MAX = 200;
	private static final int ZAP_FLASH_TICKS = 10;
	private static final int ZAP_STUN_AMPLIFIER = 30;
	private static final int TICKS_BEFORE_DRYING = 200, DRY_DAMAGE_INTERVAL = 20;
	private static final int HURT_COOLDOWN = 20;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	/** Ticks until the next discharge; the discharge fires when this reaches 1. */
	private int zapTimer;
	/** Ticks left of the skeleton flash after a discharge. */
	private int zapFlash;
	/** Ticks out of water before drying out hurts. */
	private int dryTimer = TICKS_BEFORE_DRYING;
	/** Ticks before being hit can set off another discharge. */
	private int hurtCooldown = HURT_COOLDOWN;
	private int loopFrameTicks;
	private boolean fromBucket;

	public StincarpEntity(EntityType<? extends StincarpEntity> type, Level level) {
		super(type, level);
		this.xpReward = 2;
		this.zapTimer = nextZapInterval();
		this.setPathfindingMalus(PathType.WATER, 0.0F);
		this.moveControl = new SwimControl();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 1.0)
			.add(Attributes.MAX_HEALTH, 30.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 16.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
			.add(Attributes.STEP_HEIGHT, 0.6);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(TEXTURE, PLAIN);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new WaterBoundPathNavigation(this, level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new RandomSwimmingGoal(this, 5.0, 40));
		this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
	}

	@Override
	public String textureName() {
		return TEXTURES[this.entityData.get(TEXTURE)];
	}

	private void setTexture(int index) {
		this.entityData.set(TEXTURE, index);
	}

	// --- ticking ----------------------------------------------------------------------

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level() instanceof ServerLevel level) {
			this.hurtCooldown--;
			tickDischarge();
			tickTexture();
			tickDrying(level);
		}
	}

	/** Every so often the fish discharges, holding still while it does. */
	private void tickDischarge() {
		this.zapTimer--;
		this.zapFlash--;
		if (this.zapTimer == 1) {
			this.zapTimer = nextZapInterval();
			triggerAnim(Animations.ACTIONS, "shock");
			addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ZAP_FLASH_TICKS, ZAP_STUN_AMPLIFIER, false, false));
			this.zapFlash = ZAP_FLASH_TICKS;
		}
	}

	/** During a discharge the skeleton flashes with sparks every tick; otherwise the glow pattern cycles. */
	private void tickTexture() {
		if (this.zapFlash > 0) {
			setTexture(this.zapFlash % 2 == 0 ? SKELETON_1 : SKELETON_2);
			if (BuiltInRegistries.PARTICLE_TYPE.getValue(ShockCubeIds.Particles.ZAP) instanceof ParticleOptions zap) {
				Particles.spawnAt(this, zap, 2, 1.0, 1.0, 1.0, 0.0);
			}
			return;
		}
		int texture = this.entityData.get(TEXTURE);
		if (texture == PLAIN || texture == SKELETON_2 || texture == SKELETON_1) {
			setTexture(FIRST_LOOP);
			this.loopFrameTicks = 0;
		} else if (++this.loopFrameTicks >= LOOP_FRAME_TICKS) {
			setTexture(FIRST_LOOP + (texture - FIRST_LOOP + 1) % LOOP_FRAMES);
			this.loopFrameTicks = 0;
		}
	}

	/** Out of water for ten seconds, it starts drying out: one or two damage every second. */
	private void tickDrying(ServerLevel level) {
		if (isInWater()) {
			this.dryTimer = TICKS_BEFORE_DRYING;
			return;
		}
		if (--this.dryTimer <= -1) {
			this.dryTimer = DRY_DAMAGE_INTERVAL;
			hurtServer(level, damageSources().dryOut(), Mth.nextFloat(this.random, 1.0F, 2.0F));
		}
	}

	private int nextZapInterval() {
		return Mth.nextInt(this.random, ZAP_INTERVAL_MIN, ZAP_INTERVAL_MAX);
	}

	// --- damage -----------------------------------------------------------------------

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		dischargeWhenHit(level);
		if (source.is(DamageTypes.DROWN) || source.is(DamageTypes.LIGHTNING_BOLT)) {
			return false;
		}
		return super.hurtServer(level, source, amount);
	}

	/** A hit in water makes it discharge on the next tick, with a bang and a shock cube. */
	private void dischargeWhenHit(ServerLevel level) {
		if (this.hurtCooldown <= 0 && isInWater() && !isInWall()) {
			// Counted down before it is checked, so 2 fires on the next tick (the original's 1 never fired).
			this.zapTimer = 2;
			SoundEvent blast = BuiltInRegistries.SOUND_EVENT.getValue(ShockCubeIds.Sounds.ENTITY_ELECTRIC_BLAST);
			if (blast != null) {
				Sounds.playAt(this, blast, SoundSource.BLOCKS, 3.0F, 1.0F);
			}
			BuiltInRegistries.ENTITY_TYPE.getOptional(ShockCubeIds.Entities.SHOCK_CUBE).ifPresent(type -> {
				var cube = type.spawn(level, blockPosition(), EntitySpawnReason.MOB_SUMMONED);
				if (cube != null) {
					cube.setDeltaMovement(0.0, 0.0, 0.0);
				}
			});
		}
		this.hurtCooldown = HURT_COOLDOWN;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return StincarpModule.HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return StincarpModule.DEATH_SOUND;
	}

	// --- bucket -----------------------------------------------------------------------

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		return Bucketable.bucketMobPickup(player, hand, this).orElseGet(() -> super.mobInteract(player, hand));
	}

	@Override
	public boolean fromBucket() { return this.fromBucket; }

	@Override
	public void setFromBucket(boolean fromBucket) { this.fromBucket = fromBucket; }

	@Override
	public void saveToBucketTag(ItemStack bucket) {
		Bucketable.saveDefaultDataToBucketTag(this, bucket);
	}

	@Override
	public void loadFromBucketTag(CompoundTag tag) {
		Bucketable.loadDefaultDataFromBucketTag(this, tag);
	}

	@Override
	public ItemStack getBucketItemStack() {
		return new ItemStack(StincarpModule.bucket);
	}

	@Override
	public SoundEvent getPickupSound() {
		return SoundEvents.BUCKET_FILL_FISH;
	}

	@Override
	public boolean requiresCustomPersistence() {
		return super.requiresCustomPersistence() || this.fromBucket;
	}

	@Override
	public boolean removeWhenFarAway(double distanceSquared) {
		return !this.fromBucket && !hasCustomName();
	}

	// --- movement and spawning --------------------------------------------------------

	@Override
	public boolean checkSpawnObstruction(LevelReader level) {
		return level.isUnobstructed(this);
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	/** Swims toward its target in water, pitching up and down; on land it barely flops along. */
	private class SwimControl extends MoveControl {
		SwimControl() {
			super(StincarpEntity.this);
		}

		@Override
		public void tick() {
			StincarpEntity fish = StincarpEntity.this;
			if (fish.isInWater()) {
				fish.setDeltaMovement(fish.getDeltaMovement().add(0.0, 0.005, 0.0));
			}
			if (this.operation != Operation.MOVE_TO || fish.getNavigation().isDone()) {
				fish.setSpeed(0.0F);
				fish.setYya(0.0F);
				fish.setZza(0.0F);
				return;
			}
			double dx = this.wantedX - fish.getX();
			double dy = this.wantedY - fish.getY();
			double dz = this.wantedZ - fish.getZ();
			float heading = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
			float speed = (float) (this.speedModifier * fish.getAttributeValue(Attributes.MOVEMENT_SPEED));
			fish.setYRot(rotlerp(fish.getYRot(), heading, 10.0F));
			fish.yBodyRot = fish.getYRot();
			fish.yHeadRot = fish.getYRot();
			if (fish.isInWater()) {
				fish.setSpeed((float) fish.getAttributeValue(Attributes.MOVEMENT_SPEED));
				float pitch = -(float) (Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG);
				fish.setXRot(rotlerp(fish.getXRot(), Mth.clamp(Mth.wrapDegrees(pitch), -85.0F, 85.0F), 5.0F));
				fish.setZza(Mth.cos(fish.getXRot() * Mth.DEG_TO_RAD) * speed);
				fish.setYya((float) (speed * dy));
			} else {
				fish.setSpeed(speed * 0.05F);
			}
		}
	}

	// --- saving -----------------------------------------------------------------------

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Texture", this.entityData.get(TEXTURE));
		output.putInt("ZapTimer", this.zapTimer);
		output.putInt("DryTimer", this.dryTimer);
		output.putInt("HurtCooldown", this.hurtCooldown);
		output.putBoolean("FromBucket", this.fromBucket);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setTexture(Mth.clamp(input.getIntOr("Texture", PLAIN), 0, TEXTURES.length - 1));
		this.zapTimer = input.getIntOr("ZapTimer", nextZapInterval());
		this.dryTimer = input.getIntOr("DryTimer", TICKS_BEFORE_DRYING);
		this.hurtCooldown = input.getIntOr("HurtCooldown", HURT_COOLDOWN);
		this.fromBucket = input.getBooleanOr("FromBucket", false);
	}

	// --- GeckoLib ---------------------------------------------------------------------

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(Animations.idleWalk(this, "idle", "swim"));
		controllers.add(Animations.actions(this, "shock"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
