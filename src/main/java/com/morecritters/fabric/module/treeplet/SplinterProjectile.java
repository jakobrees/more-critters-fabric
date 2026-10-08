package com.morecritters.fabric.module.treeplet;

import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * A thrown eerie dart. It trails resin; a creature it hits is slowed for three seconds, one level more for each
 * dart already in it, and the dart is used up.
 */
public class SplinterProjectile extends AbstractArrow implements ItemSupplier {
	private static final int SLOW_TICKS = 60;

	public SplinterProjectile(EntityType<? extends SplinterProjectile> type, Level level) {
		super(type, level);
	}

	/** Thrown from the player's eyes along their view (the original's 0.7 power, 1 damage). */
	static void throwFrom(Player thrower) {
		SplinterProjectile dart = new SplinterProjectile(TreepletModule.SPLINTER, thrower.level());
		dart.setOwner(thrower);
		dart.setPos(thrower.getX(), thrower.getEyeY() - 0.1, thrower.getZ());
		var view = thrower.getViewVector(1.0F);
		dart.shoot(view.x, view.y, view.z, 1.4F, 0.0F);
		dart.setSilent(true);
		dart.setBaseDamage(1.0);
		if (thrower.hasInfiniteMaterials()) dart.pickup = Pickup.CREATIVE_ONLY;
		thrower.level().addFreshEntity(dart);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.level().addParticle(TreepletModule.RESIN, this.getX() + Mth.nextDouble(this.random, -0.2, 0.2),
				this.getY() + Mth.nextDouble(this.random, -0.2, 0.2), this.getZ() + Mth.nextDouble(this.random, -0.2, 0.2), 0.0, 0.0, 0.0);
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (this.level().isClientSide()) return;
		if (hit.getEntity() instanceof LivingEntity target) {
			var slowness = target.getEffect(MobEffects.SLOWNESS);
			int amplifier = slowness == null ? 0 : slowness.getAmplifier() + 1;
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOW_TICKS, amplifier, false, true));
		}
		this.level().playSound(null, this.blockPosition(), TreepletModule.DART_HIT, SoundSource.NEUTRAL, 1.0F, 1.0F);
		this.discard();
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (!this.level().isClientSide()) {
			this.level().playSound(null, this.blockPosition(), TreepletModule.DART_HIT, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		return new ItemStack(TreepletModule.EERIE_DART);
	}

	@Override
	public ItemStack getItem() {
		return new ItemStack(TreepletModule.EERIE_DART);
	}
}
