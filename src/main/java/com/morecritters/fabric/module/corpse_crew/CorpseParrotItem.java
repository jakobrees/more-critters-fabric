package com.morecritters.fabric.module.corpse_crew;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

/**
 * A corpse parrot to keep. Used on a block it is consumed, and a parrot tamed to the user
 * appears against the clicked face in a puff of hearts.
 */
public class CorpseParrotItem extends Item {
	public CorpseParrotItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		context.getItemInHand().shrink(1);
		if (context.getLevel() instanceof ServerLevel level) {
			Vec3 spot = spawnSpot(context.getClickedPos(), context.getClickedFace());
			TamedCorpseParrotEntity parrot = CorpseCrewModule.TAMED_CORPSE_PARROT.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (parrot != null) {
				parrot.snapTo(spot);
				if (context.getPlayer() != null) parrot.tame(context.getPlayer());
				level.addFreshEntity(parrot);
			}
			level.sendParticles(ParticleTypes.HEART, spot.x, spot.y, spot.z, 5, 0.5, 0.5, 0.5, 1.0);
		}
		return InteractionResult.SUCCESS;
	}

	/** Where the original's summon command put the parrot, relative to the clicked block's corner. */
	private static Vec3 spawnSpot(BlockPos pos, Direction face) {
		Vec3 offset = switch (face) {
			case UP -> new Vec3(0.5, 1.0, 0.5);
			case DOWN -> new Vec3(0.5, -1.0, 0.5);
			case NORTH -> new Vec3(0.5, 0.0, -1.0);
			case SOUTH -> new Vec3(0.5, 0.0, 1.5);
			case WEST -> new Vec3(-1.0, 0.0, 0.5);
			case EAST -> new Vec3(1.5, 0.0, 0.5);
		};
		return Vec3.atLowerCornerOf(pos).add(offset);
	}
}
