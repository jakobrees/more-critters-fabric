package com.morecritters.fabric.module.armossillo;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Drops;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A thrown ooze rod. It sails through the air leaving a trail of glow blocks behind it, and
 * the moment there is something solid (or liquid) beneath it, it falls apart into an ooze rod
 * item again.
 */
public class FlyingOozeRodEntity extends PathfinderMob implements GeoEntity {
	/** How far below its feet it looks for ground. */
	private static final double GROUND_PROBE = 0.1;
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

	/** Damage the rod ignores, as in the original. */
	private static final List<ResourceKey<DamageType>> IMMUNE_TO = List.of(
		DamageTypes.IN_FIRE, DamageTypes.FALL, DamageTypes.CACTUS, DamageTypes.DROWN, DamageTypes.LIGHTNING_BOLT,
		DamageTypes.EXPLOSION, DamageTypes.TRIDENT, DamageTypes.FALLING_ANVIL, DamageTypes.DRAGON_BREATH,
		DamageTypes.WITHER, DamageTypes.WITHER_SKULL
	);

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);

	public FlyingOozeRodEntity(EntityType<? extends FlyingOozeRodEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 10.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level() instanceof ServerLevel level) {
			lightPathOrLand(level);
		}
	}

	/** Over open air it leaves a glow block where it is; over anything else it turns back into an item. */
	private void lightPathOrLand(ServerLevel level) {
		BlockPos below = BlockPos.containing(this.getX(), this.getY() - GROUND_PROBE, this.getZ());
		if (level.getBlockState(below).isAir()) {
			level.setBlockAndUpdate(this.blockPosition(), ArmossilloBlocks.GLOW.defaultBlockState());
		} else {
			this.discard();
			Drops.dropSingles(this, ArmossilloItems.OOZE_ROD, 1);
		}
	}

	/** Shrugs off most environmental damage, arrows, potions and players' hits. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		Entity direct = source.getDirectEntity();
		if (direct instanceof AbstractArrow || direct instanceof Player || direct instanceof AbstractThrownPotion || direct instanceof AreaEffectCloud) return false;
		for (ResourceKey<DamageType> immune : IMMUNE_TO) {
			if (source.is(immune)) return false;
		}
		return super.hurtServer(level, source, amount);
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

	// --- GeckoLib: a single looping idle --------------------------------------------

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<FlyingOozeRodEntity>(Animations.MOVEMENT, 0, test -> test.setAndContinue(IDLE)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
