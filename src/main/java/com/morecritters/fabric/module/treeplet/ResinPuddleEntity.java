package com.morecritters.fabric.module.treeplet;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.TextureVariants;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A puddle of black resin a fleeing treeplet leaves behind. It dries up through nine stages over 45 ticks. Creatures
 * standing in it are slowed, except undead and arthropods (sped up) and treeplets, which skid along it.
 */
public class ResinPuddleEntity extends PathfinderMob implements GeoEntity, TextureVariants {
	private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(ResinPuddleEntity.class, EntityDataSerializers.INT);
	private static final int STAGES = 9, TICKS_PER_STAGE = 5;

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private int stageTimer = TICKS_PER_STAGE;

	public ResinPuddleEntity(EntityType<? extends ResinPuddleEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 10.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.KNOCKBACK_RESISTANCE, 2.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(STAGE, 1);
	}

	@Override
	public String textureName() {
		return "resin_puddle" + this.entityData.get(STAGE);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level() instanceof ServerLevel level) {
			dryUp();
			if (!this.isRemoved()) stickToFeet(level);
		}
		this.setDeltaMovement(new Vec3(0.0, -2.0, 0.0));
	}

	private void dryUp() {
		if (--this.stageTimer > 0) return;
		this.stageTimer = TICKS_PER_STAGE;
		int stage = this.entityData.get(STAGE);
		if (stage >= STAGES) this.discard();
		else this.entityData.set(STAGE, stage + 1);
	}

	private void stickToFeet(ServerLevel level) {
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(this.position(), this.position()).inflate(1.0))) {
			if (entity instanceof ResinPuddleEntity) continue;
			if (entity instanceof TreepletEntity) {
				Vec3 look = entity.getLookAngle();
				entity.push(0.2 * look.x, 0.0, 0.2 * look.z);
			} else if (entity.is(EntityTypeTags.UNDEAD) || entity.is(EntityTypeTags.ARTHROPOD)) {
				entity.addEffect(new MobEffectInstance(MobEffects.SPEED, 10, 1, false, false));
			} else {
				entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10, 2, false, false));
			}
		}
	}

	/** The original's immunity list: fire, falls, cactus, drowning, lightning, explosions, tridents, anvils, dragon breath, withering. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean immune = source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_DROWNING)
			|| source.is(DamageTypeTags.IS_LIGHTNING) || source.is(DamageTypeTags.IS_EXPLOSION)
			|| source.is(DamageTypes.CACTUS) || source.is(DamageTypes.TRIDENT) || source.is(DamageTypes.FALLING_ANVIL)
			|| source.is(DamageTypes.DRAGON_BREATH) || source.is(DamageTypes.WITHER) || source.is(DamageTypes.WITHER_SKULL);
		return !immune && super.hurtServer(level, source, amount);
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		RawAnimation spread = RawAnimation.begin().thenPlay("1");
		controllers.add(new AnimationController<ResinPuddleEntity>("spread", 2, test -> test.setAndContinue(spread)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}
}
