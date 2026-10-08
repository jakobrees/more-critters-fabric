package com.morecritters.fabric.module.armossillo;

import com.morecritters.fabric.core.ServerScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A fleeting flash of light (left behind by glowing ooze). One tick after it appears it turns into a
 * full-strength light block, which goes out a quarter of a second later. Unbreakable, intangible,
 * replaceable by anything but another glow, and not pickable.
 */
public class GlowBlock extends Block {
	/** Ticks between the glow appearing and turning into light. */
	private static final int IGNITE_DELAY = 1;
	/** Ticks the light block it becomes stays lit. */
	private static final int LIGHT_DURATION = 5;
	private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

	public GlowBlock(Properties properties) {
		super(properties);
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
	protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
		return !context.getItemInHand().is(this.asItem());
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return ItemStack.EMPTY;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		level.scheduleTick(pos, this, IGNITE_DELAY);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		lightUpBriefly(level, pos);
	}

	/** Becomes a light block, then clears it again unless something else took its place. */
	private static void lightUpBriefly(ServerLevel level, BlockPos pos) {
		level.setBlock(pos, Blocks.LIGHT.defaultBlockState(), Block.UPDATE_ALL);
		ServerScheduler.runLater(LIGHT_DURATION, () -> {
			if (level.getBlockState(pos).is(Blocks.LIGHT)) {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			}
		});
	}
}
