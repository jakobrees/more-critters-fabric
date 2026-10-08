package com.morecritters.fabric.module.dripper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A thin decorative mask hung on a wall. When placed it picks one of ten faces at random.
 * It needs a block behind it (anything but air or another mask) and breaks when that goes.
 */
public class DripstoneWallMaskBlock extends Block implements SimpleWaterloggedBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	/** Which face is carved: 0 only before placement settles, then 1..10. */
	public static final IntegerProperty FACE = IntegerProperty.create("blockstate", 0, 10);
	private static final int FIRST_FACE = 1, LAST_FACE = 10;

	private static final VoxelShape AGAINST_SOUTH = Block.box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0);
	private static final VoxelShape AGAINST_WEST = Block.box(0.0, 0.0, 0.0, 1.0, 16.0, 16.0);
	private static final VoxelShape AGAINST_EAST = Block.box(15.0, 0.0, 0.0, 16.0, 16.0, 16.0);
	private static final VoxelShape AGAINST_NORTH = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 1.0);

	public DripstoneWallMaskBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false).setValue(FACE, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED, FACE);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			case NORTH -> AGAINST_SOUTH;
			case EAST -> AGAINST_WEST;
			case WEST -> AGAINST_EAST;
			default -> AGAINST_NORTH;
		};
	}

	@Override
	protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return state.getFluidState().isEmpty();
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		boolean inWater = context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER);
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(WATERLOGGED, inWater);
	}

	/** Hangs from the block behind it, which must not be air or another mask. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockState wall = level.getBlockState(pos.relative(state.getValue(FACING).getOpposite()));
		return !wall.isAir() && !wall.is(this);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		if (state.getValue(WATERLOGGED)) {
			ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		if (!state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
		return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
	}

	/** A freshly placed mask carves a random face. */
	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (oldState.is(this)) return;
		int face = level.getRandom().nextIntBetweenInclusive(FIRST_FACE, LAST_FACE);
		level.setBlock(pos, state.setValue(FACE, face), Block.UPDATE_ALL);
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
