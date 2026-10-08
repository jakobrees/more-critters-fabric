package com.morecritters.fabric.module.treeplet;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;

/** A sharp eerie-birch splinter: right-click throws one, which slows whatever it hits. */
public class EerieDartItem extends Item {
	public EerieDartItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			SplinterProjectile.throwFrom(player);
			level.playSound(null, player.blockPosition(), TreepletModule.DART_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
			stack.consume(1, player);
		}
		player.swing(hand, SwingAnimation.DEFAULT, true);
		return InteractionResult.SUCCESS;
	}
}
