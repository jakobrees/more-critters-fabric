package com.morecritters.fabric.module.fossils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Sets up the ancient skeleton exhibit next to the clicked face. The item is used up even
 * when the exhibit cannot be placed, as in the original.
 */
public class AncientSkeletonExhibitItem extends Item {
	public AncientSkeletonExhibitItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		context.getItemInHand().shrink(1);
		if (context.getLevel() instanceof ServerLevel level) {
			BlockPos at = context.getClickedPos().relative(context.getClickedFace());
			FossilsModule.ANCIENT_SKELETON_EXHIBIT.spawn(level, at, EntitySpawnReason.SPAWN_ITEM_USE);
		}
		return InteractionResult.SUCCESS;
	}
}
