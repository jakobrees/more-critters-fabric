package com.morecritters.fabric.module.critterling_system;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A party popper on a stick. When it gets a redstone signal it pops: a bang and fifty bits of
 * confetti shooting up. {@link #POPPED} is the original's {@code animation} property (0 idle,
 * 1 popping), which the block entity's animation follows.
 */
public class ConfettiPopperBlock extends WaterloggableBlock implements EntityBlock {
	public static final BooleanProperty POPPED = BooleanProperty.create("popped");
	private static final VoxelShape SHAPE = box(6, 0, 6, 10, 16, 10);
	private static final int CONFETTI_COUNT = 50;

	public ConfettiPopperBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(POPPED, false).setValue(WATERLOGGED, false));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ConfettiPopperBlockEntity(pos, state);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
		super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
		if (!(level instanceof ServerLevel serverLevel)) return;
		if (level.getBestNeighborSignal(pos) > 0) {
			pop(serverLevel, pos, state);
		} else if (state.getValue(POPPED)) {
			level.setBlock(pos, state.setValue(POPPED, false), Block.UPDATE_ALL);
		}
	}

	/** As in the original, every neighbour update while powered pops it again. */
	private static void pop(ServerLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state.setValue(POPPED, true), Block.UPDATE_ALL);
		level.playSound(null, pos, CritterlingSystemModule.CONFETTI_POP_SOUND, SoundSource.BLOCKS, 1.0F, 1.0F);
		level.sendParticles(CritterlingSystemModule.CONFETTI_PARTICLE, true, false,
			pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, CONFETTI_COUNT, 0.0, 1.0, 0.0, 0.1);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(POPPED);
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

	/** There is no loot table for the popper; like the original, it drops itself. */
	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		List<ItemStack> drops = super.getDrops(state, params);
		return drops.isEmpty() ? List.of(new ItemStack(this)) : drops;
	}
}
