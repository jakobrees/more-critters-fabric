package com.morecritters.fabric.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Shared set-up for the game tests. Every test runs in the 8×8×8 empty structure of the Fabric game
 * test API; positions are relative to its corner, and {@link #floor} gives it a stone floor at y 0.
 */
public final class TestScenes {
	/** Stone across the bottom layer, so mobs spawned at y 1 stand on it. */
	public static void floor(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(x, 0, z, Blocks.STONE);
			}
		}
	}

	/** The player uses the item in its main hand on {@code face} of the block at {@code pos}, like a right-click. */
	public static InteractionResult useItemOn(GameTestHelper helper, Player player, BlockPos pos, Direction face) {
		BlockPos absolute = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(face, 0.5), face, absolute, false);
		return player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
	}

	private TestScenes() {}
}
