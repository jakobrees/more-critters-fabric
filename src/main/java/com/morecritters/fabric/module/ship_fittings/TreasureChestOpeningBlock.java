package com.morecritters.fabric.module.ship_fittings;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * An unlocked treasure chest whose lid is about to come off. Every tick it counts down; at zero it spills the
 * treasure and becomes the open chest. It cannot be broken or opened meanwhile.
 */
public class TreasureChestOpeningBlock extends TreasureChestBlock {
	public TreasureChestOpeningBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return InteractionResult.PASS;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		level.scheduleTick(pos, this, 1);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		super.tick(state, level, pos, random);
		if (level.getBlockEntity(pos) instanceof TreasureChestBlockEntity chest) {
			chest.tickOpening(level);
		}
		level.scheduleTick(pos, this, 1);
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(ShipFittingsModule.TREASURE_CHEST);
	}
}
