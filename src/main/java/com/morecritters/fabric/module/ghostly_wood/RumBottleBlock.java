package com.morecritters.fabric.module.ghostly_wood;

import com.morecritters.fabric.ids.CorpseGearIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Rum poured out on a floor: a slick puddle. Anything walking through it slips and is flung
 * the way the pourer was facing, with a slip sound (one time in a hundred, a big one).
 * Breaking it gives nothing back.
 */
public class RumBottleBlock extends Block {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);
	private static final double SLIP_SPEED = 1.0;
	private static final double SLIP_LIFT = 0.3;
	private static final int BIG_SLIP_CHANCE = 100;

	public RumBottleBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** Lies only on a block that fills its space (one that occludes). */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.below()).canOcclude();
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		if (!state.canSurvive(level, pos)) {
			return Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
		super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
		if (!entity.onGround() || entity.getDeltaMovement().horizontalDistanceSqr() <= 1.0E-6) {
			return;
		}
		Direction away = state.getValue(FACING).getOpposite(); // the puddle faces back at its pourer
		entity.setDeltaMovement(new Vec3(away.getStepX() * SLIP_SPEED, SLIP_LIFT, away.getStepZ() * SLIP_SPEED));
		if (!level.isClientSide()) {
			boolean bigSlip = level.getRandom().nextInt(BIG_SLIP_CHANCE) == 0;
			playSound(level, pos, bigSlip ? CorpseGearIds.Sounds.BLOCK_RUM_BIG_SLIP : CorpseGearIds.Sounds.BLOCK_RUM_SLIP);
		}
	}

	/** The slip sounds belong to corpse_gear; looked up when needed, silent if it is not loaded. */
	private static void playSound(Level level, BlockPos pos, Identifier soundId) {
		BuiltInRegistries.SOUND_EVENT.getOptional(soundId)
			.ifPresent(sound -> level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F));
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
