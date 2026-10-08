package com.morecritters.fabric.module.ship_fittings;

import com.morecritters.fabric.core.ServerScheduler;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A ship's wheel that works as a redstone dial: each right-click turns it one notch to the right and raises its
 * signal by one (up to 15); sneaking with empty hands turns it back down. It can be waterlogged.
 */
public class ShipWheelBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
	/** Which spin the wheel shows: 0 idle, 1 turned up, 2 turned down (3 is unused, as in the original). */
	public static final IntegerProperty ANIMATION = IntegerProperty.create("animation", 0, 3);
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	static final int SPIN_UP = 1, SPIN_DOWN = 2;
	private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

	public ShipWheelBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(ANIMATION, 0).setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ShipWheelBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof ShipWheelBlockEntity wheel) {
			boolean down = player.isShiftKeyDown();
			if (wheel.turn(down ? -1 : 1)) {
				level.playSound(null, pos, ShipFittingsModule.SHIP_WHEEL_SPIN_SOUND, SoundSource.BLOCKS, 1.0F, 1.0F);
				this.spin(serverLevel, pos, down ? SPIN_DOWN : SPIN_UP);
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Resets the spin animation, then starts the new one a tick later so it plays from the beginning. */
	private void spin(ServerLevel level, BlockPos pos, int animation) {
		level.setBlock(pos, level.getBlockState(pos).setValue(ANIMATION, 0), Block.UPDATE_ALL);
		level.updateNeighborsAt(pos, this);
		ServerScheduler.runLater(1, () -> {
			BlockState now = level.getBlockState(pos);
			if (now.is(this)) level.setBlock(pos, now.setValue(ANIMATION, animation), Block.UPDATE_ALL);
		});
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(pos) instanceof ShipWheelBlockEntity wheel ? wheel.power() : 0;
	}

	/** Drawn by its GeckoLib block entity renderer. */
	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return state.getFluidState().isEmpty();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ANIMATION, FACING, WATERLOGGED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		boolean inWater = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(WATERLOGGED, inWater);
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
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		if (state.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
	}

	/** There is no loot table for the wheel; like the original, it drops itself. */
	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		List<ItemStack> drops = super.getDrops(state, params);
		return drops.isEmpty() ? List.of(new ItemStack(this)) : drops;
	}
}
