package com.morecritters.fabric.module.critterling_system;

import com.morecritters.fabric.core.Sounds;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;

/**
 * Right-clicking a critterling (or a critter eater) with an empty critterling sack in the main hand
 * puts it in the sack: the creature vanishes and the sack becomes the filled sack for its kind and
 * rarity ({@link CritterlingSackItem#sackFor}). The original's {@code CritterlingSackRightclickMobProcedure}.
 */
final class CritterlingCatching {
	static void register() {
		UseEntityCallback.EVENT.register((player, level, hand, target, hit) -> tryCatch(player, level, hand, target));
	}

	private static InteractionResult tryCatch(Player player, Level level, InteractionHand hand, Entity target) {
		if (hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().is(CritterlingSystemModule.CRITTERLING_SACK)) {
			return InteractionResult.PASS;
		}
		CritterlingSackItem sack = CritterlingSackItem.sackFor(target);
		if (sack == null) return InteractionResult.PASS;
		if (!level.isClientSide()) {
			Sounds.playAt(target, CritterlingSystemModule.SACK_PICK_UP_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
			target.discard();
			player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(sack));
		}
		return InteractionResult.SUCCESS;
	}

	private CritterlingCatching() {}
}
