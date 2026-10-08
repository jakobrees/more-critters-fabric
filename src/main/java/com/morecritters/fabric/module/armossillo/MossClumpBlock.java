package com.morecritters.fabric.module.armossillo;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import com.morecritters.fabric.ids.ArmossilloIds;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
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
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A small clump of moss, the armossillo's nest. Bone meal "hatches" it: the clump vanishes, a baby
 * armossillo appears in its place, and the nearest player within ten blocks earns the breeding
 * advancement. Waterloggable; faces the side it was placed against.
 */
public class MossClumpBlock extends Block implements SimpleWaterloggedBlock, BonemealableBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 10.0, 13.0);
	/** Edge length of the box around the clump searched for the player who gets the advancement. */
	private static final double HATCH_WITNESS_RANGE = 20.0;
	private static final Identifier BREED_ADVANCEMENT = MoreCritters.id("breed_armossillo");

	public MossClumpBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
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

	/** Placed on a floor or ceiling it faces north; placed against a wall it faces away from it. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		boolean inWater = context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER);
		Direction face = context.getClickedFace();
		Direction facing = face.getAxis() == Direction.Axis.Y ? Direction.NORTH : face;
		return this.defaultBlockState().setValue(FACING, facing).setValue(WATERLOGGED, inWater);
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

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
		return true;
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		hatch(level, pos);
	}

	/** The clump turns into a baby armossillo, witnessed by the nearest player. */
	private static void hatch(ServerLevel level, BlockPos pos) {
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		BuiltInRegistries.ENTITY_TYPE.getValue(ArmossilloIds.Entities.BABY_ARMOSSILLO).spawn(level, pos, EntitySpawnReason.MOB_SUMMONED);
		Player witness = nearestWitness(level, pos);
		if (witness != null) Advancements.award(witness, BREED_ADVANCEMENT);
	}

	private static @Nullable Player nearestWitness(ServerLevel level, BlockPos pos) {
		Vec3 centre = Vec3.atCenterOf(pos);
		AABB area = AABB.ofSize(centre, HATCH_WITNESS_RANGE, HATCH_WITNESS_RANGE, HATCH_WITNESS_RANGE);
		return level.getEntitiesOfClass(Player.class, area).stream()
			.min(Comparator.comparingDouble(player -> player.distanceToSqr(centre)))
			.orElse(null);
	}
}
