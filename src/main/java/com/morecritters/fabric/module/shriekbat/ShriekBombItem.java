package com.morecritters.fabric.module.shriekbat;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Thrown at once on use, then a one-second cooldown. */
public class ShriekBombItem extends Item {
	private static final int COOLDOWN = 20;

	public ShriekBombItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			ShriekbombProjectile.throwFrom(player);
			stack.consume(1, player);
			player.getCooldowns().addCooldown(stack, COOLDOWN);
		}
		return InteractionResult.SUCCESS;
	}
}
