package com.morecritters.fabric.module.bomb_jelly;

import com.morecritters.fabric.core.Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;

/** Launches a jelly torpedo the way the player looks, only underwater; five seconds' cooldown. */
public class JellyTorpedoItem extends Item {
	private static final int COOLDOWN_TICKS = 100;

	public JellyTorpedoItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!player.isInWater()) {
			return InteractionResult.PASS;
		}
		ItemStack stack = player.getItemInHand(hand);
		player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
		if (level instanceof ServerLevel serverLevel) {
			player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
			Sounds.playAt(player, BombJellyModule.TORPEDO_SPAWN_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
			BlockPos launch = BlockPos.containing(player.getX(), player.getY() + 1.0, player.getZ());
			JellyTorpedoEntity torpedo = BombJellyModule.JELLY_TORPEDO.spawn(serverLevel, launch, EntitySpawnReason.MOB_SUMMONED);
			if (torpedo != null) {
				torpedo.setYRot(player.getYRot());
				torpedo.setYBodyRot(player.getYRot());
				torpedo.setYHeadRot(player.getYRot());
				torpedo.setXRot(player.getXRot());
			}
		}
		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}
}
