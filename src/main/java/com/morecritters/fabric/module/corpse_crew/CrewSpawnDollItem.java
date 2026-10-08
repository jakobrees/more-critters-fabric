package com.morecritters.fabric.module.corpse_crew;

import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * A creative-only doll of one crew member: used on a block, it is consumed and that member
 * appears against the clicked face.
 */
public class CrewSpawnDollItem extends Item {
	private final Supplier<EntityType<?>> member;

	public CrewSpawnDollItem(Supplier<EntityType<?>> member, Properties properties) {
		super(properties);
		this.member = member;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		// Consumed even in creative, as in the original.
		context.getItemInHand().shrink(1);
		if (context.getLevel() instanceof ServerLevel level) {
			this.member.get().spawn(level, context.getClickedPos().relative(context.getClickedFace()), EntitySpawnReason.MOB_SUMMONED);
		}
		return InteractionResult.SUCCESS;
	}
}
