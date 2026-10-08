package com.morecritters.fabric.module.ghostly_wood;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * A square pole along one axis that can stand in water, like a vanilla chain but of any thickness:
 * the giant chain (12 pixels) and the fish bone pole (4 pixels).
 */
public class PoleBlock extends ChainBlock {
	private final Map<Direction.Axis, VoxelShape> shapes;

	public PoleBlock(double thickness, Properties properties) {
		super(properties);
		this.shapes = Shapes.rotateAllAxis(Block.cube(thickness, thickness, 16.0));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapes.get(state.getValue(AXIS));
	}
}
