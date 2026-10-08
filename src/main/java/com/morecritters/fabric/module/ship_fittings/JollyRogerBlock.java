package com.morecritters.fabric.module.ship_fittings;

import com.morecritters.fabric.ids.GhostlyWoodIds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.block.Blocks;
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
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Corpse Crew's tattered flag, hung on the side of a block. Hung on a fish bone pole or an ectometal screw it
 * shows the large flag. Right-clicking it changes how it waves (three looping animations in turn).
 */
public class JollyRogerBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
	/** 1 when the large flag shows (hung on a pole or screw). */
	public static final IntegerProperty LARGE = IntegerProperty.create("blockstate", 0, 1);
	/** Which waving animation plays: 0, 1 or 2 (3 is unused, as in the original). */
	public static final IntegerProperty ANIMATION = IntegerProperty.create("animation", 0, 3);
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	private static final int WAVES = 3;

	public JollyRogerBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(LARGE, 0).setValue(ANIMATION, 0).setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new JollyRogerBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		int animation = state.getValue(ANIMATION);
		if (!level.isClientSide() && animation < WAVES) {
			level.setBlock(pos, state.setValue(ANIMATION, (animation + 1) % WAVES), Block.UPDATE_ALL);
		}
		return InteractionResult.SUCCESS;
	}

	/** The block the flag hangs from: behind it, opposite the way it faces. */
	private static BlockPos supportOf(BlockPos pos, BlockState state) {
		return pos.relative(state.getValue(FACING).getOpposite());
	}

	/** A fish bone pole (from the ghostly wood module) or an ectometal screw. */
	private static boolean isPole(BlockState support) {
		if (support.is(ShipFittingsModule.ECTOMETAL_SCREW)) return true;
		Block fishBonePole = BuiltInRegistries.BLOCK.getValue(GhostlyWoodIds.Blocks.FISH_BONE_POLE);
		return fishBonePole != Blocks.AIR && support.is(fishBonePole);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockPos supportPos = supportOf(pos, state);
		BlockState support = level.getBlockState(supportPos);
		return support.isFaceSturdy(level, supportPos, state.getValue(FACING)) || isPole(support);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		this.showLargeFlagOnPole(state, level, pos);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbour, @Nullable Orientation orientation,
			boolean movedByPiston) {
		super.neighborChanged(state, level, pos, neighbour, orientation, movedByPiston);
		this.showLargeFlagOnPole(state, level, pos);
	}

	private void showLargeFlagOnPole(BlockState state, Level level, BlockPos pos) {
		int large = isPole(level.getBlockState(supportOf(pos, state))) ? 1 : 0;
		if (state.getValue(LARGE) != large) {
			level.setBlock(pos, state.setValue(LARGE, large), Block.UPDATE_ALL);
		}
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		if (state.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		if (!state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
		return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
	}

	/** Drawn by its GeckoLib block entity renderer. */
	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	/** A pole-thin slab along the flag, reaching 6 pixels further into the next block when large. */
	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		int reach = state.getValue(LARGE) == 1 ? 22 : 16;
		return switch (state.getValue(FACING)) {
			case NORTH -> Block.box(6.0, 0.0, 6.0, 10.0, 16.0, reach);
			case EAST -> Block.box(16 - reach, 0.0, 6.0, 10.0, 16.0, 10.0);
			case WEST -> Block.box(6.0, 0.0, 6.0, reach, 16.0, 10.0);
			default -> Block.box(6.0, 0.0, 16 - reach, 10.0, 16.0, 10.0);
		};
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return state.getFluidState().isEmpty();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ANIMATION, FACING, WATERLOGGED, LARGE);
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

	/** There is no loot table for the flag; like the original, it drops itself. */
	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		List<ItemStack> drops = super.getDrops(state, params);
		return drops.isEmpty() ? List.of(new ItemStack(this)) : drops;
	}
}
