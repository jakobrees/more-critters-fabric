package com.morecritters.fabric.module.wandering_collector;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.TextureVariants;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * What both carrybugs share: a sturdy, never-despawning pack bug in one of ten biome colours
 * that slowly heals itself and kicks off anyone who has not yet earned its trust (the
 * {@code gain_access_to_carrybug} advancement, granted by trading with the wandering collector).
 */
public abstract class CarrybugBaseEntity extends PathfinderMob implements GeoEntity, TextureVariants {
	/** The biome colour: plains, desert, savanna, badlands, tundra, swamp, jungle, ocean, taiga or cave. */
	private static final EntityDataAccessor<String> VARIANT = SynchedEntityData.defineId(CarrybugBaseEntity.class, EntityDataSerializers.STRING);
	private static final Identifier TRUST_ADVANCEMENT = MoreCritters.id("gain_access_to_carrybug");
	/** One tick in this many, a hurt carrybug gives itself a burst of regeneration. */
	private static final int HEAL_CHANCE = 500;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);

	protected CarrybugBaseEntity(EntityType<? extends CarrybugBaseEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			.add(Attributes.MAX_HEALTH, 35.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 16.0)
			.add(Attributes.STEP_HEIGHT, 2.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new RandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(2, new PanicGoal(this, 1.2));
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(4, new FloatGoal(this));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(VARIANT, "plains");
	}

	public String variant() {
		return this.entityData.get(VARIANT);
	}

	public void setVariant(String variant) {
		this.entityData.set(VARIANT, variant);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("Variant", this.variant());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.setVariant(input.getStringOr("Variant", "plains"));
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level().isClientSide()) {
			return;
		}
		if (this.getHealth() < this.getMaxHealth() && this.random.nextInt(HEAL_CHANCE) == 0) {
			this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 2, false, true));
		}
		Entity passenger = this.getFirstPassenger();
		if (passenger != null && !isTrusted(passenger)) {
			kickOff(passenger);
		}
	}

	/** Only players who have earned the carrybug advancement may stay on its back. */
	private static boolean isTrusted(Entity passenger) {
		if (!(passenger instanceof ServerPlayer player)) {
			return false;
		}
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(TRUST_ADVANCEMENT);
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	private void kickOff(Entity passenger) {
		passenger.stopRiding();
		if (passenger instanceof Player player) {
			player.sendOverlayMessage(Component.literal("Trade with the Wandering Collector more in order to ride the Carrybug"));
		}
		Sounds.playAt(this, WanderingCollectorModule.CARRYBUG_KICK_SOUND);
		this.triggerAnim(Animations.ACTIONS, "kick1");
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		return new Vec3(0.0, dimensions.height() * 0.75 + 0.5 + (passenger instanceof Player ? 0.25 : 0.0), 0.0);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return WanderingCollectorModule.CARRYBUG_IDLE_SOUND;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(WanderingCollectorModule.CARRYBUG_STEP_SOUND, 0.15F, 1.0F);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return WanderingCollectorModule.CARRYBUG_HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return WanderingCollectorModule.CARRYBUG_DEATH_SOUND;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(Animations.idleWalk(this, "idle", "ride_walk"));
		controllers.add(Animations.actions(this, "kick1"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
