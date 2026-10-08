package com.morecritters.fabric.module.armossillo;

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
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A glowing bulb on a stem that hangs from any block above it and breaks when that block goes.
 * Goobulbs stack downwards into a vine: each one picks its look from whether goobulbs hang above
 * and below it. Bright (light 15), intangible, instabreak, waterloggable.
 */
public class GoobulbBlock extends Block implements SimpleWaterloggedBlock {
	/** The look, named after the original's {@code blockstate} property. */
	public static final IntegerProperty STAGE = IntegerProperty.create("blockstate", 0, 3);
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	/** Lowest of a vine: the bulb on a stem reaching up. */
	private static final int TIP = 0;
	/** Between two goobulbs: bare stem. */
	private static final int MIDDLE = 1;
	/** Topmost of a vine: stem under a cap. */
	private static final int TOP = 2;
	/** On its own: bulb, stem and cap. */
	private static final int SINGLE = 3;

	static final int LIGHT = 15;

	private static final VoxelShape BULB = Block.box(4.0, 2.0, 4.0, 12.0, 10.0, 12.0);
	private static final VoxelShape CAP = Block.box(5.0, 13.0, 5.0, 11.0, 16.0, 11.0);
	private static final VoxelShape TIP_SHAPE = Shapes.or(BULB, Block.box(7.0, 10.0, 7.0, 9.0, 16.0, 9.0));
	private static final VoxelShape MIDDLE_SHAPE = Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0);
	private static final VoxelShape TOP_SHAPE = Shapes.or(Block.box(7.0, 0.0, 7.0, 9.0, 13.0, 9.0), CAP);
	private static final VoxelShape SINGLE_SHAPE = Shapes.or(BULB, Block.box(7.0, 10.0, 7.0, 9.0, 13.0, 9.0), CAP);

	public GoobulbBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, TIP).setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(WATERLOGGED, STAGE);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(STAGE)) {
			case MIDDLE -> MIDDLE_SHAPE;
			case TOP -> TOP_SHAPE;
			case SINGLE -> SINGLE_SHAPE;
			default -> TIP_SHAPE;
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
	protected int getLightDampening(BlockState state) {
		return 0;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		boolean inWater = context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER);
		return this.defaultBlockState().setValue(WATERLOGGED, inWater);
	}

	/** Hangs from whatever is above, as long as it is not air. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return !level.getBlockState(pos.above()).isAir();
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

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		matchNeighbours(level, pos, state);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
		super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
		matchNeighbours(level, pos, state);
	}

	/** Switches to the look that fits the goobulbs above and below. */
	private void matchNeighbours(Level level, BlockPos pos, BlockState state) {
		int stage = stageFor(level.getBlockState(pos.above()).is(this), level.getBlockState(pos.below()).is(this));
		if (state.getValue(STAGE) != stage) {
			level.setBlock(pos, state.setValue(STAGE, stage), Block.UPDATE_ALL);
		}
	}

	private static int stageFor(boolean goobulbAbove, boolean goobulbBelow) {
		if (goobulbAbove) return goobulbBelow ? MIDDLE : TIP;
		return goobulbBelow ? TOP : SINGLE;
	}
}
