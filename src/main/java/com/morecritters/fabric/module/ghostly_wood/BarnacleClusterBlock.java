package com.morecritters.fabric.module.ghostly_wood;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * A flat patch of barnacles on a floor, wall or ceiling, above or under water. Each placed cluster
 * picks one of its looks at random, and it falls off when the block holding it goes.
 */
public class BarnacleClusterBlock extends FaceMountedBlock implements SimpleWaterloggedBlock {
	/** Which texture the cluster shows; the original only ever picks 0, 2 or 3. */
	public static final IntegerProperty LOOK = IntegerProperty.create("blockstate", 0, 3);
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	private static final int[] PLACED_LOOKS = {0, 2, 3};

	public BarnacleClusterBlock(Properties properties) {
		super(Block.box(0.0, 0.0, 14.0, 16.0, 16.0, 16.0), properties);
		registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false).setValue(LOOK, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(WATERLOGGED, LOOK);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		boolean inWater = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
		return super.getStateForPlacement(context).setValue(WATERLOGGED, inWater);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		// Only on a fresh placement: the original re-rolled on its own look change too, to the same effect.
		if (!oldState.is(this)) {
			int look = PLACED_LOOKS[level.getRandom().nextInt(PLACED_LOOKS.length)];
			level.setBlock(pos, state.setValue(LOOK, look), Block.UPDATE_ALL);
		}
	}

	/**
	 * Held by the sturdy face of the block behind it, or of the block above (ceiling) or below (floor).
	 * As in the original, the block behind its facing holds it even when it lies on a floor or ceiling.
	 */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		BlockPos behind = pos.relative(facing.getOpposite());
		if (level.getBlockState(behind).isFaceSturdy(level, behind, facing)) {
			return true;
		}
		return switch (state.getValue(FACE)) {
			case CEILING -> level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), Direction.DOWN);
			case FLOOR -> level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
			case WALL -> false;
		};
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		if (state.getValue(WATERLOGGED)) {
			ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		if (!state.canSurvive(level, pos)) {
			return Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}
}
