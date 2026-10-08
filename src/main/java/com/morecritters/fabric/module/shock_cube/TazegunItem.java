package com.morecritters.fabric.module.shock_cube;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The tazegun fires a thunderball every five seconds. Once nearly worn out it refuses to fire
 * and instead reloads from a bottle o' electricity in the inventory, if there is one.
 */
public class TazegunItem extends ElectricToolItem {
	/** Damage beyond which the gun needs a reload (of 250 durability). */
	private static final int SPENT = 248;
	private static final int COOLDOWN = 100;
	private static final double BALL_DAMAGE = 3.0;
	private static final int BALL_KNOCKBACK = 1;
	private static final float BALL_SPEED = 3.0F;

	TazegunItem(Properties properties) {
		super(properties, "1", "2");
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack gun = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			if (gun.getDamageValue() > SPENT) {
				reloadOrRefuse(player, gun);
			} else {
				playAnimation(player, gun, "1");
				player.getCooldowns().addCooldown(gun, COOLDOWN);
				play(player, ShockCubeModule.TAZEGUN_SHOOT_SOUND);
				ThunderballProjectile.fire(player, BALL_DAMAGE, BALL_KNOCKBACK, BALL_SPEED);
			}
		}
		return super.use(level, player, hand);
	}

	private void reloadOrRefuse(Player player, ItemStack gun) {
		if (player.getInventory().contains(stack -> stack.is(ShockCubeModule.BOTTLE_OF_ELECTRICITY))) {
			player.getInventory().clearOrCountMatchingItems(stack -> stack.is(ShockCubeModule.BOTTLE_OF_ELECTRICITY), false, 1, player.inventoryMenu.getCraftSlots());
			gun.setDamageValue(0);
			play(player, ShockCubeModule.TAZEGUN_RELOAD_SOUND);
		} else {
			playAnimation(player, gun, "2");
			play(player, ShockCubeModule.TAZEGUN_REFUSE_SOUND);
		}
	}

	private static void play(Player player, SoundEvent sound) {
		player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
	}
}
