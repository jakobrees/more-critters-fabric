package com.morecritters.fabric.module.bouncelizard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A bounceberry bush, with or without berries. Right-click a full bush to pick two berries; breaking it by hand
 * also yields two. An empty bush regrows its berries on one random tick in twenty, or at once with bone meal.
 * Bushes can stand in water, and picking one (with the pick-block key) gives an empty bush.
 */
public class BounceberryBushBlock extends Block implements BonemealableBlock, SimpleWaterloggedBlock {
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	private static final int BERRIES_PER_PICK = 2;
	private final boolean hasBerries;

	public BounceberryBushBlock(Properties properties, boolean hasBerries) {
		super(properties);
		this.hasBerries = hasBerries;
		registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(WATERLOGGED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER));
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		if (state.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
	}

	/** Neighbouring bushes merge into one hedge: faces between two bushes of a kind are not drawn. */
	@Override
	protected boolean skipRendering(BlockState state, BlockState neighbour, Direction direction) {
		return neighbour.is(this) || super.skipRendering(state, neighbour, direction);
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return state.getFluidState().isEmpty();
	}

	@Override
	protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(BouncelizardModule.EMPTY_BUSH);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!this.hasBerries) return InteractionResult.PASS;
		level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
		level.setBlock(pos, BouncelizardModule.EMPTY_BUSH.withPropertiesOf(state), 3);
		dropBerries(level, pos);
		return InteractionResult.SUCCESS;
	}

	@Override
	public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
		super.playerDestroy(level, player, pos, state, blockEntity, tool);
		if (this.hasBerries && !player.hasInfiniteMaterials()) dropBerries(level, pos);
	}

	private static void dropBerries(Level level, BlockPos pos) {
		if (level.isClientSide()) return;
		for (int i = 0; i < BERRIES_PER_PICK; i++) popResource(level, pos, new ItemStack(BouncelizardItems.bounceberry));
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!this.hasBerries && random.nextInt(20) == 0) regrow(level, pos, state);
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
		return !this.hasBerries;
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		regrow(level, pos, state);
	}

	private static void regrow(ServerLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, BouncelizardModule.BUSH.withPropertiesOf(state), 3);
	}
}
