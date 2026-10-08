package com.morecritters.fabric.module.corpse_crew;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.ServerScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A barnacle-crusted corpse grown onto the ghost ships, on floors, walls or ceilings. While a
 * player stands in it, something in it may snap shut (one chance in twenty each tick): a quarter
 * second later, if a player is still there, whatever set it off is bitten for 4.
 */
public class CorpseBarnacleBlock extends Block implements SimpleWaterloggedBlock {
	/** 1 while the barnacle snaps. The blockstate files call it "blockstate". */
	public static final IntegerProperty SNAPPING = IntegerProperty.create("blockstate", 0, 1);
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<AttachFace> FACE = FaceAttachedHorizontalDirectionalBlock.FACE;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	private static final VoxelShape FLOOR = box(2.0, 0.0, 2.0, 14.0, 5.0, 14.0);
	private static final VoxelShape CEILING = box(2.0, 11.0, 2.0, 14.0, 16.0, 14.0);
	private static final VoxelShape WALL_NORTH = box(2.0, 2.0, 11.0, 14.0, 14.0, 16.0);
	private static final VoxelShape WALL_SOUTH = box(2.0, 2.0, 0.0, 14.0, 14.0, 5.0);
	private static final VoxelShape WALL_EAST = box(0.0, 2.0, 2.0, 5.0, 14.0, 14.0);
	private static final VoxelShape WALL_WEST = box(11.0, 2.0, 2.0, 16.0, 14.0, 14.0);

	private static final int SNAP_ODDS = 20;
	private static final int SNAP_TICKS = 5;
	private static final float BITE_DAMAGE = 4.0F;
	private static final ResourceKey<DamageType> BITING = ResourceKey.create(Registries.DAMAGE_TYPE, MoreCritters.id("biting"));

	public CorpseBarnacleBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(FACING, Direction.NORTH).setValue(FACE, AttachFace.WALL).setValue(WATERLOGGED, false).setValue(SNAPPING, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, FACE, WATERLOGGED, SNAPPING);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACE)) {
			case FLOOR -> FLOOR;
			case CEILING -> CEILING;
			case WALL -> switch (state.getValue(FACING)) {
				case EAST -> WALL_EAST;
				case WEST -> WALL_WEST;
				case SOUTH -> WALL_SOUTH;
				default -> WALL_NORTH;
			};
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

	/** On a floor or ceiling it faces the way the player looks; on a wall it faces out from it. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		boolean inWater = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
		Direction clicked = context.getClickedFace();
		BlockState state = this.defaultBlockState().setValue(WATERLOGGED, inWater);
		if (clicked.getAxis() == Direction.Axis.Y) {
			return state.setValue(FACE, clicked == Direction.DOWN ? AttachFace.CEILING : AttachFace.FLOOR)
				.setValue(FACING, context.getHorizontalDirection());
		}
		return state.setValue(FACE, AttachFace.WALL).setValue(FACING, clicked);
	}

	/**
	 * Holds on to a sturdy block behind it (judged by its facing alone, as in the original), or
	 * else to the ceiling or floor it is attached to.
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

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean isPrecise) {
		if (!(level instanceof ServerLevel serverLevel)) return;
		if (!playerInside(level, pos)) {
			setSnapping(level, pos, 0);
			return;
		}
		if (!CorpseCrewMember.isCreativeOrSpectator(entity) && Mth.nextInt(level.getRandom(), 1, SNAP_ODDS) == 1) {
			setSnapping(level, pos, 1);
			ServerScheduler.runLater(SNAP_TICKS, () -> {
				setSnapping(level, pos, 0);
				if (playerInside(level, pos)) {
					entity.hurtServer(serverLevel, serverLevel.damageSources().source(BITING), BITE_DAMAGE);
				}
			});
		}
	}

	/** A player in the 1.5-block box the original measured, centred on the block's lower corner. */
	private static boolean playerInside(Level level, BlockPos pos) {
		return !level.getEntitiesOfClass(Player.class, AABB.ofSize(Vec3.atLowerCornerOf(pos), 1.5, 1.5, 1.5)).isEmpty();
	}

	private static void setSnapping(Level level, BlockPos pos, int snapping) {
		BlockState state = level.getBlockState(pos);
		if (state.hasProperty(SNAPPING) && state.getValue(SNAPPING) != snapping) {
			level.setBlock(pos, state.setValue(SNAPPING, snapping), Block.UPDATE_ALL);
		}
	}
}
