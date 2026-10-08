package com.morecritters.fabric.module.corpse_gear;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * What the crew's thrown things share: they fly like arrows and look like their item, never stay stuck in a
 * target or in the ground (they break on landing), and knock a creature back by their own fixed strength.
 */
public abstract class GearProjectile extends AbstractArrow implements ItemSupplier {
	protected GearProjectile(EntityType<? extends GearProjectile> type, Level level) {
		super(type, level);
	}

	/** Thrown by a creature: starts at its eyes, owned by it. */
	protected GearProjectile(EntityType<? extends GearProjectile> type, LivingEntity thrower, Level level, ItemStack item) {
		super(type, thrower, level, item, null);
	}

	/** The item this projectile is and looks like. */
	protected abstract Item item();

	/** The original's knockback strength (vanilla arrows only get knockback from enchantments). */
	protected int knockback() {
		return 0;
	}

	/**
	 * Moves, then runs the projectile's own tick ({@link #flightTick}) and only then drops it if it landed. The original
	 * ran its "while flying" procedure on the landing tick too, before discarding.
	 */
	@Override
	public void tick() {
		super.tick();
		if (!this.isRemoved()) this.flightTick();
		if (this.isInGround()) this.discard();
	}

	/** Each tick after moving, including the tick it lands in. */
	protected void flightTick() {
	}

	@Override
	protected void doKnockback(LivingEntity target, DamageSource source) {
		super.doKnockback(target, source);
		if (this.knockback() <= 0) return;
		double resistance = Math.max(0.0, 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
		Vec3 push = this.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize().scale(this.knockback() * 0.6 * resistance);
		if (push.lengthSqr() > 0.0) target.push(push.x, 0.1, push.z);
	}

	/** Nothing stays stuck in the target. */
	@Override
	protected void doPostHurtEffects(LivingEntity target) {
		super.doPostHurtEffects(target);
		target.setArrowCount(target.getArrowCount() - 1);
	}

	/** In water or a bubble column, where bottles and biscuits sink away. */
	protected boolean isInWaterBlock() {
		BlockState state = this.level().getBlockState(this.blockPosition());
		return state.is(Blocks.WATER) || state.is(Blocks.BUBBLE_COLUMN);
	}

	/** The original placed block-hit effects at the hit block's corner, not its centre. */
	protected static Vec3 cornerOf(BlockPos pos) {
		return Vec3.atLowerCornerOf(pos);
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		return new ItemStack(this.item());
	}

	@Override
	public ItemStack getItem() {
		return new ItemStack(this.item());
	}
}
