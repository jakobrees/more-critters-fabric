package com.morecritters.fabric.module.ghostly_wood;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Planks of the ghost ship. A player walking on them earns the "walk on ghostly planks" advancement.
 * The nested slab and stairs do the same; as in the original, only the petrified slab and stairs
 * use them, the plain ghostly slab and stairs do not count.
 */
public class GhostlyPlanksBlock extends Block {
	private static final Identifier WALK_ADVANCEMENT = MoreCritters.id("walk_on_ghostly_planks");

	public GhostlyPlanksBlock(Properties properties) {
		super(properties);
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		super.stepOn(level, pos, state, entity);
		Advancements.award(entity, WALK_ADVANCEMENT);
	}

	/** A slab that awards the walking advancement. */
	public static class Slab extends SlabBlock {
		public Slab(Properties properties) {
			super(properties);
		}

		@Override
		public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
			super.stepOn(level, pos, state, entity);
			Advancements.award(entity, WALK_ADVANCEMENT);
		}
	}

	/** Stairs that award the walking advancement. */
	public static class Stairs extends StairBlock {
		public Stairs(BlockState base, Properties properties) {
			super(base, properties);
		}

		@Override
		public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
			super.stepOn(level, pos, state, entity);
			Advancements.award(entity, WALK_ADVANCEMENT);
		}
	}
}
