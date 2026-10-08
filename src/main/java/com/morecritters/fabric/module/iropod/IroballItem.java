package com.morecritters.fabric.module.iropod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Places an iroball (spiked or plain) against the clicked face of a block. */
public class IroballItem extends Item {
	private final boolean sturdy;

	public IroballItem(boolean sturdy, Properties properties) {
		super(properties);
		this.sturdy = sturdy;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		context.getItemInHand().shrink(1);
		if (context.getLevel() instanceof ServerLevel level) {
			IroballEntity ball = IropodModule.IROBALL.spawn(level, context.getClickedPos().relative(context.getClickedFace()),
				EntitySpawnReason.MOB_SUMMONED);
			if (ball != null) ball.setSturdy(sturdy);
		}
		return InteractionResult.SUCCESS;
	}
}
