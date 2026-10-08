package com.morecritters.fabric.module.iropod;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.TextureVariants;
import java.util.Comparator;
import java.util.List;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A rolled-up iropod shell used as a ball. Walking into it kicks it the way you are looking; once it has
 * been airborne for two ticks it slams into whatever it touches (10 damage, 20 for the spiked one). Hitting
 * it does no damage but knocks it away from the attacker. Right-click it with an empty hand to pick it up.
 */
public class IroballEntity extends PathfinderMob implements GeoEntity, TextureVariants {
	private static final EntityDataAccessor<Boolean> STURDY = SynchedEntityData.defineId(IroballEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> MOTION = SynchedEntityData.defineId(IroballEntity.class, EntityDataSerializers.INT);
	private static final int IDLE = 0, ROLLING = 1, FLYING = 2;
	private static final RawAnimation[] MOTION_ANIMATIONS = {
		RawAnimation.begin().thenLoop("idle"), RawAnimation.begin().thenLoop("walk"), RawAnimation.begin().thenLoop("fly")};
	private static final double TOUCH_RANGE = 0.5;
	private static final double KICK_HORIZONTAL = 2.0;
	private static final int AIRBORNE_TICKS_TO_HIT = 2;
	private static final float HIT_DAMAGE = 10.0F;
	private static final float STURDY_HIT_DAMAGE = 20.0F;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private int airborneTicks;

	public IroballEntity(EntityType<? extends IroballEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 10.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(STURDY, false);
		builder.define(MOTION, IDLE);
	}

	public boolean sturdy() {
		return entityData.get(STURDY);
	}

	public void setSturdy(boolean sturdy) {
		entityData.set(STURDY, sturdy);
	}

	@Override
	public String textureName() {
		return sturdy() ? "sturdy_iroball" : "iroball";
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!(level() instanceof ServerLevel level)) return;
		entityData.set(MOTION, getDeltaMovement().horizontalDistanceSqr() <= 1.0E-6 ? IDLE : onGround() ? ROLLING : FLYING);
		for (LivingEntity kicker : touching()) {
			lookAt(EntityAnchorArgument.Anchor.EYES, kicker.position());
			Vec3 look = kicker.getLookAngle();
			setDeltaMovement(look.x * KICK_HORIZONTAL, look.y, look.z * KICK_HORIZONTAL);
		}
		airborneTicks = onGround() ? 0 : airborneTicks + 1;
		if (airborneTicks >= AIRBORNE_TICKS_TO_HIT) {
			for (LivingEntity target : touching()) {
				Sounds.playAt(this, IropodModule.IROBALL_HIT_SOUND);
				target.hurtServer(level, level.damageSources().generic(), sturdy() ? STURDY_HIT_DAMAGE : HIT_DAMAGE);
			}
		}
	}

	/** Living things (other than iroballs) touching the ball, nearest first. */
	private List<LivingEntity> touching() {
		Vec3 center = position();
		return level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(TOUCH_RANGE),
				entity -> entity != this && !(entity instanceof IroballEntity))
			.stream().sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(center))).toList();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isImmuneTo(source)) return false;
		Entity attacker = source.getEntity();
		if (attacker == null) return super.hurtServer(level, source, amount);
		// Hits never hurt the ball; they knock it away from whoever struck it.
		lookAt(EntityAnchorArgument.Anchor.EYES, attacker.position());
		Sounds.playAt(this, IropodModule.IROBALL_HURT_SOUND);
		setDeltaMovement(getLookAngle().scale(-amount / 2.0));
		return false;
	}

	private static boolean isImmuneTo(DamageSource source) {
		return source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.FALL) || source.is(DamageTypes.CACTUS)
			|| source.is(DamageTypes.DROWN) || source.is(DamageTypes.LIGHTNING_BOLT) || source.is(DamageTypes.FALLING_ANVIL)
			|| source.is(DamageTypes.DRAGON_BREATH) || source.is(DamageTypes.WITHER) || source.is(DamageTypes.WITHER_SKULL)
			|| source.getDirectEntity() instanceof AbstractThrownPotion || source.getDirectEntity() instanceof AreaEffectCloud;
	}

	/** An empty hand picks the ball back up as its item. */
	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		super.mobInteract(player, hand);
		if (player.getMainHandItem().isEmpty()) {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(sturdy() ? IropodModule.spikedIroball : IropodModule.iroballItem));
			if (!level().isClientSide()) discard();
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Sturdy", sturdy());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setSturdy(input.getBooleanOr("Sturdy", false));
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.GENERIC_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.GENERIC_DEATH;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<IroballEntity>(Animations.MOVEMENT, 0,
			test -> test.setAndContinue(MOTION_ANIMATIONS[entityData.get(MOTION)])));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animations;
	}
}
