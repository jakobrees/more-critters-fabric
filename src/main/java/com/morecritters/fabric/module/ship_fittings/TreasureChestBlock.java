package com.morecritters.fabric.module.ship_fittings;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
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
 * A locked treasure chest. Right-clicked with a treasure key in the main hand it unlocks (the key is used up) and
 * turns into the opening chest, keeping its contents. Without a key it refuses; a creative player may look inside.
 */
public class TreasureChestBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);
	/** Ticks the lid takes to come off after unlocking. */
	private static final int UNLOCK_TICKS = 20;

	public TreasureChestBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TreasureChestBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
		ItemStack key = player.getMainHandItem();
		if (key.is(ShipFittingsModule.TREASURE_KEY)) {
			unlock(serverLevel, pos, state);
			key.shrink(1);
		} else if (player.hasInfiniteMaterials()) {
			if (level.getBlockEntity(pos) instanceof TreasureChestBlockEntity chest) player.openMenu(chest);
			level.playSound(null, pos, ShipFittingsModule.CHEST_OPEN_SOUND, SoundSource.BLOCKS, 1.0F, 1.0F);
		} else {
			player.sendOverlayMessage(Component.literal("This Chest is locked"));
			level.playSound(null, pos, ShipFittingsModule.CHEST_REFUSE_SOUND, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
		return InteractionResult.SUCCESS;
	}

	/** Swaps in the opening chest with the same contents and starts the countdown to the lid coming off. */
	private static void unlock(ServerLevel level, BlockPos pos, BlockState state) {
		TreasureChestBlockEntity locked = level.getBlockEntity(pos) instanceof TreasureChestBlockEntity chest ? chest : null;
		level.setBlock(pos, ShipFittingsModule.TREASURE_CHEST_OPENING.defaultBlockState().setValue(FACING, state.getValue(FACING)), Block.UPDATE_ALL);
		if (level.getBlockEntity(pos) instanceof TreasureChestBlockEntity opening) {
			if (locked != null) opening.takeContentsOf(locked);
			opening.startCountdown(UNLOCK_TICKS);
		}
		level.playSound(null, pos, ShipFittingsModule.CHEST_UNLOCK_SOUND, SoundSource.BLOCKS, 1.0F, 1.0F);
		Vec3 centre = Vec3.atCenterOf(pos);
		level.sendParticles(new DustParticleOptions(0xFF0000, 1.0F), true, false, centre.x, centre.y, centre.z, 5, 0.5, 0.2, 0.5, 1.0);
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
		return true;
	}
}
