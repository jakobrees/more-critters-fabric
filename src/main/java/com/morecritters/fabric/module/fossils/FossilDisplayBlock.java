package com.morecritters.fabric.module.fossils;

import com.morecritters.fabric.core.Drops;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A wooden display stand, empty or showing one fossil. Right-clicking the empty stand with a
 * fossil puts it on show (one fossil is used up); right-clicking a full stand with an empty
 * hand takes the fossil back. Breaking a full stand drops the fossil (loot table) and the stand.
 */
public class FossilDisplayBlock extends Block {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	/** {@code minecraft:fossils}: every fossil a stand can show. */
	private static final TagKey<Item> FOSSILS = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("fossils"));

	private static final VoxelShape NORTH = box(0.5, 0.0, 6.0, 15.5, 16.0, 16.0);
	private static final VoxelShape EAST = box(0.0, 0.0, 0.5, 10.0, 16.0, 15.5);
	private static final VoxelShape WEST = box(6.0, 0.0, 0.5, 16.0, 16.0, 15.5);
	private static final VoxelShape SOUTH = box(0.5, 0.0, 0.0, 15.5, 16.0, 10.0);

	/** The fossil on show, or null for the empty stand. */
	private final @Nullable Identifier fossil;

	public FossilDisplayBlock(Properties properties, @Nullable Identifier fossil) {
		super(properties);
		this.fossil = fossil;
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (this.fossil == null) {
			putOnShow(state, level, pos, player);
		} else if (player.getMainHandItem().isEmpty()) {
			takeBack(state, level, pos, player);
		}
		return InteractionResult.SUCCESS;
	}

	private static void putOnShow(BlockState state, Level level, BlockPos pos, Player player) {
		ItemStack held = player.getMainHandItem();
		if (!held.is(FOSSILS)) return;
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		Block display = Fossils.displayFor(held);
		if (display == null) return;
		level.setBlock(pos, display.defaultBlockState().setValue(FACING, state.getValue(FACING)), Block.UPDATE_ALL);
		if (!player.hasInfiniteMaterials()) held.shrink(1);
	}

	private void takeBack(BlockState state, Level level, BlockPos pos, Player player) {
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Fossils.item(this.fossil)));
		level.setBlock(pos, FossilsModule.EMPTY_DISPLAY.defaultBlockState().setValue(FACING, state.getValue(FACING)), Block.UPDATE_ALL);
	}

	/** The loot table of a full stand drops only its fossil; the stand itself comes from here. */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (this.fossil != null && level instanceof ServerLevel serverLevel && !player.hasInfiniteMaterials()) {
			Drops.dropSingles(serverLevel, Vec3.atCenterOf(pos), FossilsModule.EMPTY_DISPLAY, 1);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(FossilsModule.EMPTY_DISPLAY);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			case NORTH -> NORTH;
			case EAST -> EAST;
			case WEST -> WEST;
			default -> SOUTH;
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

	@Override
	protected int getLightDampening(BlockState state) {
		return 0;
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
