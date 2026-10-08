package com.morecritters.fabric.module.nightshroom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Packed-up ancient skeleton bones: used on a block, they are laid out against the clicked face.
 * A skeleton placed this way can be picked up again.
 */
public class AncientSkeletonItem extends Item {
	public AncientSkeletonItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		// Used up even in creative and even if the skeleton does not fit, as in the original.
		context.getItemInHand().shrink(1);
		if (context.getLevel() instanceof ServerLevel level) {
			AncientSkeletonEntity skeleton = NightshroomModule.ANCIENT_SKELETON.spawn(level,
				context.getClickedPos().relative(context.getClickedFace()), EntitySpawnReason.SPAWN_ITEM_USE);
			if (skeleton != null) skeleton.markPlaced();
		}
		return InteractionResult.SUCCESS;
	}
}
