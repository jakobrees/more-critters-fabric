package com.morecritters.fabric.module.shimmerwing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A cocoon hanging off the side of a chorus plant; on a random tick, one time in fifty, a shimmerworm hatches out. */
public class ShimmeringChrysalisBlock extends Block {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	private static final int HATCH_CHANCE = 50;

	public ShimmeringChrysalisBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			case NORTH -> Shapes.or(box(4.8, 1.8, 9.8, 11.2, 9.2, 16.2), box(4.8, 8.8, 9.8, 11.2, 11.2, 16.2));
			case EAST -> Shapes.or(box(-0.2, 1.8, 4.8, 6.2, 9.2, 11.2), box(-0.2, 8.8, 4.8, 6.2, 11.2, 11.2));
			case WEST -> Shapes.or(box(9.8, 1.8, 4.8, 16.2, 9.2, 11.2), box(9.8, 8.8, 4.8, 16.2, 11.2, 11.2));
			default -> Shapes.or(box(4.8, 1.8, -0.2, 11.2, 9.2, 6.2), box(4.8, 8.8, -0.2, 11.2, 11.2, 6.2));
		};
	}

	@Override
	protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	/** Needs a chorus plant or flower beside it. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockState neighbour = level.getBlockState(pos.relative(side));
			if (neighbour.is(Blocks.CHORUS_PLANT) || neighbour.is(Blocks.CHORUS_FLOWER)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		return state.canSurvive(level, pos) ? state : Blocks.AIR.defaultBlockState();
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (random.nextInt(HATCH_CHANCE) == 0) {
			hatch(level, pos);
		}
	}

	private void hatch(ServerLevel level, BlockPos pos) {
		level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(this.defaultBlockState()));
		ShimmerwingModule.SHIMMERWORM.spawn(level, pos, EntitySpawnReason.MOB_SUMMONED);
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
	}
}
