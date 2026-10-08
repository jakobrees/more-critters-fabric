package com.morecritters.fabric.module.ship_fittings;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A thin ectometal rod, 4 pixels thick, placed along the clicked face's axis. Jolly rogers hang from it. */
public class EctometalScrewBlock extends RotatedPillarBlock {
	private static final VoxelShape ALONG_X = Block.box(0.0, 6.0, 6.0, 16.0, 10.0, 10.0);
	private static final VoxelShape ALONG_Y = Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);
	private static final VoxelShape ALONG_Z = Block.box(6.0, 6.0, 0.0, 10.0, 10.0, 16.0);

	public EctometalScrewBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(AXIS)) {
			case X -> ALONG_X;
			case Y -> ALONG_Y;
			case Z -> ALONG_Z;
		};
	}

	@Override
	protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return true;
	}
}
