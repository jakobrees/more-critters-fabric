package com.morecritters.fabric.module.critterling_system;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A wooden table with a sunken top; using it opens the {@link EvolutionTableMenu}. */
public class EvolutionTableBlock extends Block {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	private static final Component TITLE = Component.translatable("gui.more_critters.evolution_table_gui.label_evolution_table");

	/** The table top with a rim around a sunken centre; the rim pieces swap with the facing. */
	private static final VoxelShape NORTH_SOUTH = Shapes.or(box(0, 0, 0, 16, 11, 16),
		box(0, 11, 0, 16, 16, 4), box(0, 11, 12, 16, 16, 16), box(12, 11, 4, 16, 16, 12), box(0, 11, 4, 4, 16, 12));
	private static final VoxelShape EAST_WEST = Shapes.or(box(0, 0, 0, 16, 11, 16),
		box(12, 11, 0, 16, 16, 16), box(0, 11, 0, 4, 16, 16), box(4, 11, 12, 12, 16, 16), box(4, 11, 0, 12, 16, 4));
	private static final Map<Direction, VoxelShape> SHAPES = Map.of(
		Direction.NORTH, NORTH_SOUTH, Direction.SOUTH, NORTH_SOUTH, Direction.EAST, EAST_WEST, Direction.WEST, EAST_WEST);

	public EvolutionTableBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			player.openMenu(state.getMenuProvider(level, pos));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
		return new SimpleMenuProvider((containerId, inventory, player) ->
			new EvolutionTableMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)), TITLE);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return true;
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
}
