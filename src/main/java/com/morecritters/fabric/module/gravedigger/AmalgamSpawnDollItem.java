package com.morecritters.fabric.module.gravedigger;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * A creative-only doll: used on a block, it is consumed and an Amalgam appears against the
 * clicked face.
 */
public class AmalgamSpawnDollItem extends Item {
	public AmalgamSpawnDollItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		// Consumed even in creative, as in the original.
		context.getItemInHand().shrink(1);
		if (context.getLevel() instanceof ServerLevel level) {
			GravediggerModule.AMALGAM.spawn(level, context.getClickedPos().relative(context.getClickedFace()), EntitySpawnReason.MOB_SUMMONED);
		}
		return InteractionResult.SUCCESS;
	}
}
