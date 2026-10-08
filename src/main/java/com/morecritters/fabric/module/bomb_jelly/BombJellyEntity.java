package com.morecritters.fabric.module.bomb_jelly;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Config;
import com.morecritters.fabric.core.Drops;
import com.morecritters.fabric.core.DryingOut;
import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.SwimmingFish;
import com.morecritters.fabric.core.TextureVariants;
import java.util.Comparator;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Bucketable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A bomb jelly of one size. It drifts about in deep water and blows up a moment after anything that
 * is not a sea creature touches it, leaving explosive jelly. Out of water it flops and dries out.
 * It can be scooped up with a water bucket or lit with flint and steel. Its texture pulses.
 */
public class BombJellyEntity extends PathfinderMob implements GeoEntity, TextureVariants, Bucketable {
	private static final EntityDataAccessor<Integer> TEXTURE_FRAME = SynchedEntityData.defineId(BombJellyEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> FROM_BUCKET = SynchedEntityData.defineId(BombJellyEntity.class, EntityDataSerializers.BOOLEAN);

	private static final double SWIM_SPEED = 0.3;
	/** Ticks between being set off and the explosion. */
	private static final int FUSE_TICKS = 15;
	private static final double CONTACT_REACH = 0.5;
	private static final double ADVANCEMENT_RANGE = 15.0;

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final DryingOut air = new DryingOut(this);
	private final BombJellySize size;
	private int frameTimer;
	private boolean falling;
	private boolean fuseLit;

	public BombJellyEntity(EntityType<? extends BombJellyEntity> type, Level level, BombJellySize size) {
		super(type, level);
		this.size = size;
		this.xpReward = size.experience;
		this.frameTimer = size.ticksPerFrame;
		this.moveControl = SwimmingFish.setUp(this);
	}

	public static AttributeSupplier.Builder createAttributes(double maxHealth) {
		return PathfinderMob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, maxHealth)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 16.0)
			.add(Attributes.STEP_HEIGHT, 0.6);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(TEXTURE_FRAME, 1);
		builder.define(FROM_BUCKET, false);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return SwimmingFish.navigation(this, level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new RandomSwimmingGoal(this, 1.0, 40));
	}

	@Override
	protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
		SwimmingFish.swimFaster(this, SWIM_SPEED, input);
		super.travelInWater(input, baseGravity, isFalling, oldY);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level() instanceof ServerLevel level) {
			pulseTexture();
			animateFalling();
			if (this.isInWater() && this.isAlive() && !this.fuseLit && somethingTouches(level)) {
				detonateOnContact(level);
			}
			this.air.tick(level);
		}
	}

	/** Steps through the five texture frames, if the config allows it. */
	private void pulseTexture() {
		if (--this.frameTimer > 0) {
			return;
		}
		this.frameTimer = this.size.ticksPerFrame;
		if (Config.flag("animate_bomb_jelly", true)) {
			this.entityData.set(TEXTURE_FRAME, this.textureFrame() % BombJellySize.TEXTURE_FRAMES + 1);
		}
	}

	/** Out of water and off the ground it plays its fall; landing it squashes, back in water it floats again. */
	private void animateFalling() {
		boolean airborne = !this.isInWater() && !this.onGround();
		if (airborne && !this.falling) {
			this.triggerAnim(Animations.ACTIONS, "fall");
		} else if (this.falling && this.onGround()) {
			this.triggerAnim(Animations.ACTIONS, "land2");
		} else if (this.falling && this.isInWater()) {
			this.stopTriggeredAnim(Animations.ACTIONS, null);
		}
		this.falling = airborne;
	}

	/** Anything within reach that is not a sea creature, a creative player or a spectator. */
	private boolean somethingTouches(ServerLevel level) {
		AABB reach = new AABB(this.position(), this.position()).inflate(CONTACT_REACH);
		return !level.getEntitiesOfClass(Entity.class, reach, this::setsOff).isEmpty();
	}

	private boolean setsOff(Entity entity) {
		return !entity.is(EntityTypeTags.AQUATIC)
			&& !(entity instanceof WaterAnimal)
			&& !(entity instanceof Player player && (player.isCreative() || player.isSpectator()));
	}

	/** The fuse hisses, then the jelly bursts in a cloud of bubbles and leaves explosive jelly behind. */
	private void detonateOnContact(ServerLevel level) {
		this.fuseLit = true;
		this.triggerAnim(Animations.ACTIONS, "explode");
		Sounds.playAt(this, BombJellyModule.EXPLODE_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
		Vec3 at = this.position();
		ServerScheduler.runLater(FUSE_TICKS, () -> {
			if (!this.isAlive()) {
				return;
			}
			level.sendParticles(ParticleTypes.BUBBLE, true, true, at.x, at.y, at.z, 100, 1.0, 1.0, 1.0, 0.0);
			level.explode(null, at.x, at.y, at.z, this.size.contactPower(), Level.ExplosionInteraction.NONE);
			level.getEntitiesOfClass(Player.class, AABB.ofSize(at, ADVANCEMENT_RANGE, ADVANCEMENT_RANGE, ADVANCEMENT_RANGE))
				.stream()
				.min(Comparator.comparingDouble(player -> player.distanceToSqr(at)))
				.ifPresent(player -> Advancements.award(player, MoreCritters.id("explode_bomb_jelly")));
			Drops.dropSingles(level, at, BombJellyModule.explosiveJelly, Drops.randomCount(this, 2, 4));
			this.discard();
		});
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		super.mobInteract(player, hand);
		ItemStack held = player.getMainHandItem();
		if (held.is(Items.WATER_BUCKET)) {
			if (!this.level().isClientSide()) {
				scoopUp(player);
			}
		} else if (held.is(Items.FLINT_AND_STEEL)) {
			if (this.level() instanceof ServerLevel level) {
				ignite(level, player, held);
			}
		}
		// The original answers every right click as handled.
		return InteractionResult.SUCCESS;
	}

	private void scoopUp(Player player) {
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		ItemStack bucket = this.getBucketItemStack();
		this.saveToBucketTag(bucket);
		player.setItemInHand(InteractionHand.MAIN_HAND, bucket);
		Sounds.playAt(this, SoundEvents.BUCKET_FILL_FISH, SoundSource.BLOCKS, 1.0F, 1.0F);
		this.discard();
	}

	/** Lit by hand it goes off with a smaller, fixed bang and leaves nothing. */
	private void ignite(ServerLevel level, Player player, ItemStack flintAndSteel) {
		this.triggerAnim(Animations.ACTIONS, "explode");
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		Sounds.playAt(this, SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
		flintAndSteel.hurtAndBreak(1, player, InteractionHand.MAIN_HAND);
		Vec3 at = this.position();
		ServerScheduler.runLater(FUSE_TICKS, () -> {
			if (this.isAlive()) {
				level.explode(null, at.x, at.y, at.z, this.size.ignitedPower, Level.ExplosionInteraction.NONE);
				this.discard();
			}
		});
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypes.FALL) || source.is(DamageTypes.DROWN)) {
			return false;
		}
		// Stranded on land, it turns to face whoever hits it.
		Entity attacker = source.getEntity();
		if (attacker != null && !this.isInWater() && !this.falling) {
			this.lookAt(EntityAnchorArgument.Anchor.EYES, attacker.position());
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	public SoundEvent getHurtSound(DamageSource source) {
		return BombJellyModule.HURT_SOUND;
	}

	@Override
	public SoundEvent getDeathSound() {
		return BombJellyModule.HURT_SOUND;
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	public boolean checkSpawnObstruction(LevelReader level) {
		return level.isUnobstructed(this);
	}

	@Override
	public boolean fromBucket() {
		return this.entityData.get(FROM_BUCKET);
	}

	@Override
	public void setFromBucket(boolean fromBucket) {
		this.entityData.set(FROM_BUCKET, fromBucket);
	}

	@Override
	public void saveToBucketTag(ItemStack bucket) {
		Bucketable.saveDefaultDataToBucketTag(this, bucket);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void loadFromBucketTag(CompoundTag tag) {
		Bucketable.loadDefaultDataFromBucketTag(this, tag);
	}

	@Override
	public ItemStack getBucketItemStack() {
		return new ItemStack(BombJellyModule.bucketFor(this.size));
	}

	@Override
	public SoundEvent getPickupSound() {
		return SoundEvents.BUCKET_FILL_FISH;
	}

	@Override
	public boolean requiresCustomPersistence() {
		return super.requiresCustomPersistence() || this.fromBucket();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("TextureFrame", this.textureFrame());
		output.putBoolean("FromBucket", this.fromBucket());
		this.air.save(output);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.entityData.set(TEXTURE_FRAME, input.getIntOr("TextureFrame", 1));
		setFromBucket(input.getBooleanOr("FromBucket", false));
		this.air.load(input);
	}

	private int textureFrame() {
		return this.entityData.get(TEXTURE_FRAME);
	}

	@Override
	public String textureName() {
		return this.size.texturePrefix + this.textureFrame();
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		RawAnimation floating = RawAnimation.begin().thenLoop("idle");
		RawAnimation stranded = RawAnimation.begin().thenLoop("land");
		controllers.add(new AnimationController<BombJellyEntity>(Animations.MOVEMENT, 0, (AnimationTest<BombJellyEntity> test) ->
			test.setAndContinue(test.animatable().isInWater() ? floating : stranded)));
		controllers.add(Animations.actions(this, "fall", "land2", "explode"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}
}
