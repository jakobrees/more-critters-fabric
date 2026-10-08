package com.morecritters.fabric.module.nightshroom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * A creative-only doll: used on a block, it is consumed and its creature appears against the
 * clicked face.
 */
public class SpawnDollItem extends Item {
	private final EntityType<? extends Mob> creature;

	public SpawnDollItem(EntityType<? extends Mob> creature, Properties properties) {
		super(properties);
		this.creature = creature;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		// Consumed even in creative, as in the original.
		context.getItemInHand().shrink(1);
		if (context.getLevel() instanceof ServerLevel level) {
			Mob mob = this.creature.spawn(level, context.getClickedPos().relative(context.getClickedFace()), EntitySpawnReason.MOB_SUMMONED);
			if (mob != null) mob.setDeltaMovement(0.0, 0.0, 0.0);
		}
		return InteractionResult.SUCCESS;
	}
}
