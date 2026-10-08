package com.morecritters.fabric.module.kelpire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A plate of five kelpire rolls, four around one in the middle. Each right-click hands the
 * player a piece and takes a roll off the plate; the middle one goes last, with the block.
 */
public class KelpireRollsBlock extends Block implements SimpleWaterloggedBlock {
	/** 0 = fresh plate, 2..5 = rolls eaten (the original skips 1, which looks like 0). */
	public static final IntegerProperty BITES = IntegerProperty.create("blockstate", 0, 5);
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	private static final int LAST_BITE = 5;
	private static final VoxelShape MIDDLE = box(5.5, 0.0, 5.5, 10.5, 4.0, 10.5);
	private static final VoxelShape ROLL_A = box(11.0, 0.0, 1.0, 16.0, 4.0, 6.0);
	private static final VoxelShape ROLL_B = box(0.0, 0.0, 10.0, 5.0, 4.0, 15.0);
	private static final VoxelShape ROLL_C = box(10.0, 0.0, 11.0, 15.0, 4.0, 16.0);
	private static final VoxelShape ROLL_D = box(1.0, 0.0, 0.0, 6.0, 4.0, 5.0);

	/** The outer rolls in the order they are eaten, by facing (copied from the original's shapes). */
	private static VoxelShape[] eatingOrder(Direction facing) {
		return switch (facing) {
			case NORTH -> new VoxelShape[] {ROLL_A, ROLL_C, ROLL_D, ROLL_B};
			case EAST -> new VoxelShape[] {ROLL_C, ROLL_B, ROLL_A, ROLL_D};
			case WEST -> new VoxelShape[] {ROLL_D, ROLL_A, ROLL_B, ROLL_C};
			default -> new VoxelShape[] {ROLL_B, ROLL_D, ROLL_C, ROLL_A};
		};
	}

	public KelpireRollsBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false).setValue(BITES, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED, BITES);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		VoxelShape[] rolls = eatingOrder(state.getValue(FACING));
		int eaten = Math.max(0, state.getValue(BITES) - 1);
		VoxelShape shape = MIDDLE;
		for (int i = eaten; i < rolls.length; i++) {
			shape = Shapes.or(shape, rolls[i]);
		}
		return shape;
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
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		boolean inWater = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
		return this.defaultBlockState()
			.setValue(FACING, context.getHorizontalDirection().getOpposite())
			.setValue(WATERLOGGED, inWater);
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
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		if (state.getValue(WATERLOGGED)) {
			ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
	}

	/** Hands over a piece and eats a roll off the plate; the last piece clears the block. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		givePiece(player);
		int bites = state.getValue(BITES);
		if (bites >= LAST_BITE) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		} else {
			level.setBlock(pos, state.setValue(BITES, Math.max(bites, 1) + 1), Block.UPDATE_ALL);
		}
		return InteractionResult.SUCCESS;
	}

	private static void givePiece(Player player) {
		player.getInventory().placeItemBackInInventory(new ItemStack(KelpireModule.ROLL_PIECE), Prediction.SERVER_ONLY);
	}
}
