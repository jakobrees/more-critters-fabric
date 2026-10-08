package com.morecritters.fabric.module.corpse_gear;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A pearl fired by the corpse tank. Wherever it lands, on a creature or a block, it bursts in an explosion
 * (power 3) that hurts but breaks no blocks.
 */
public class FlyingPearlProjectile extends GearProjectile {
	private static final float BLAST_POWER = 3.0F;
	/** The tank fires its pearls with knockback 2; nothing else fires them. */
	private static final int KNOCKBACK = 2;

	public FlyingPearlProjectile(EntityType<? extends FlyingPearlProjectile> type, Level level) {
		super(type, level);
	}

	@Override
	protected Item item() {
		return CorpseGearModule.PEARL;
	}

	@Override
	protected int knockback() {
		return KNOCKBACK;
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		burst(this.position());
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		burst(cornerOf(hit.getBlockPos()));
	}

	private void burst(Vec3 at) {
		if (!this.level().isClientSide()) {
			this.level().explode(null, at.x, at.y, at.z, BLAST_POWER, Level.ExplosionInteraction.NONE);
		}
	}
}
