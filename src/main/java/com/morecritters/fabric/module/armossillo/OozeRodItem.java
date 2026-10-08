package com.morecritters.fabric.module.armossillo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;

/**
 * A rod of glowing ooze. Swung from the main hand it is flung from above the thrower's head
 * along their gaze as a {@link FlyingOozeRodEntity}, which lights its path.
 */
public class OozeRodItem extends Item {
	private static final int THROW_COOLDOWN = 50;
	private static final double THROW_SPEED = 3.0;

	public OozeRodItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		// The original only reacts while the rod is in the main hand.
		if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
		ItemStack stack = player.getItemInHand(hand);
		player.swing(hand, SwingAnimation.DEFAULT, true);
		player.getCooldowns().addCooldown(stack, THROW_COOLDOWN);
		if (level instanceof ServerLevel serverLevel) {
			throwRod(serverLevel, player);
		}
		if (!player.hasInfiniteMaterials()) {
			stack.shrink(1);
		}
		return InteractionResult.SUCCESS;
	}

	private static void throwRod(ServerLevel level, Player player) {
		BlockPos above = BlockPos.containing(player.getX(), player.getY() + player.getBbHeight(), player.getZ());
		FlyingOozeRodEntity rod = ArmossilloItems.FLYING_OOZE_ROD.spawn(level, above, EntitySpawnReason.MOB_SUMMONED);
		if (rod != null) {
			rod.setDeltaMovement(player.getLookAngle().scale(THROW_SPEED));
		}
	}
}
