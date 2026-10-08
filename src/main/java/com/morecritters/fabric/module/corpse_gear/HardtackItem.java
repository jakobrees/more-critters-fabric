package com.morecritters.fabric.module.corpse_gear;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;

/** Hardtack, too hard to eat whole: right-click throws a biscuit (plain or infested) that may stun what it hits. */
public class HardtackItem extends Item {
	private final boolean infested;

	public HardtackItem(Properties properties, boolean infested) {
		super(properties);
		this.infested = infested;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			ThrownHardtack.throwFrom(player, this.infested);
			stack.consume(1, player);
		}
		player.swing(hand, SwingAnimation.DEFAULT, true);
		return InteractionResult.SUCCESS;
	}
}
