package com.morecritters.fabric.module.treeplet;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * A lobbed clump of black resin (nothing in the original fires it). A creature it hits is slowed; where it lands
 * on a block, a resin puddle spreads.
 */
public class ResinPieceProjectile extends AbstractArrow implements ItemSupplier {
	public ResinPieceProjectile(EntityType<? extends ResinPieceProjectile> type, Level level) {
		super(type, level);
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
		if (!this.level().isClientSide() && hit.getEntity() instanceof LivingEntity target) {
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2, false, false));
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (this.level() instanceof ServerLevel level) {
			ResinPuddleEntity puddle = TreepletModule.RESIN_PUDDLE.spawn(level, hit.getBlockPos().above(), EntitySpawnReason.MOB_SUMMONED);
			if (puddle != null) puddle.setDeltaMovement(0.0, 0.0, 0.0);
		}
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		return new ItemStack(TreepletModule.BLACK_RESIN_CLUMP);
	}

	@Override
	public ItemStack getItem() {
		return new ItemStack(TreepletModule.BLACK_RESIN_CLUMP);
	}
}
