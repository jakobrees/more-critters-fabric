package com.morecritters.fabric.module.ghostly_wood;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A ragged sail panel, two pixels thin. Panels stack into a hanging flag: a panel with another under
 * it shows one of the upper rags (looks 0 to 2), the lowest panel one of the frayed bottom rags
 * (looks 4 to 6). Each re-picks its look when the panel below appears or goes.
 */
public class TatteredFlagBlock extends Block implements SimpleWaterloggedBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	/** Which rag texture the panel shows; 0-3 are upper panels, 4-7 bottom panels. 3 and 7 are never picked. */
	public static final IntegerProperty LOOK = IntegerProperty.create("blockstate", 0, 7);
	private static final int FIRST_BOTTOM_LOOK = 4;
	private static final int PICKED_LOOKS = 3;

	private static final VoxelShape NORTH_SOUTH = Block.box(0.0, 0.0, 7.0, 16.0, 16.0, 9.0);
	private static final VoxelShape EAST_WEST = Block.box(7.0, 0.0, 0.0, 9.0, 16.0, 16.0);

	public TatteredFlagBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false).setValue(LOOK, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED, LOOK);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		boolean inWater = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(WATERLOGGED, inWater);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(FACING).getAxis() == Direction.Axis.X ? EAST_WEST : NORTH_SOUTH;
	}

	/** Neighbouring panels hide the faces between them. */
	@Override
	protected boolean skipRendering(BlockState state, BlockState neighborState, Direction direction) {
		return neighborState.is(this) || super.skipRendering(state, neighborState, direction);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		// Only on a fresh placement: the original re-rolled on its own look change too, to the same effect.
		if (!oldState.is(this)) {
			pickLook(state, level, pos);
		}
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
		super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
		boolean isBottomLook = state.getValue(LOOK) >= FIRST_BOTTOM_LOOK;
		if (isBottomLook == hasPanelBelow(level, pos)) {
			pickLook(state, level, pos);
		}
	}

	private boolean hasPanelBelow(Level level, BlockPos pos) {
		return level.getBlockState(pos.below()).is(this);
	}

	/** An upper rag over another panel, a bottom rag otherwise; the original rolled 0-2 or 4-6. */
	private void pickLook(BlockState state, Level level, BlockPos pos) {
		int first = hasPanelBelow(level, pos) ? 0 : FIRST_BOTTOM_LOOK;
		level.setBlock(pos, state.setValue(LOOK, first + level.getRandom().nextInt(PICKED_LOOKS)), Block.UPDATE_ALL);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		if (state.getValue(WATERLOGGED)) {
			ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
}
