package com.morecritters.fabric.module.snowflake_spider;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;

/**
 * A sack of freezing web, thrown the moment it is used. It is its own ammunition: throwing
 * one uses up a web sack (from the hands, else from the inventory) unless in creative, and the
 * thrower then waits a second before the next throw.
 */
public class WebSackItem extends Item {
	private static final int USE_DURATION = 20;
	private static final int COOLDOWN_TICKS = 20;

	public WebSackItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.BOW;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user) {
		return USE_DURATION;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!player.hasInfiniteMaterials() && findAmmo(player).isEmpty()) return InteractionResult.FAIL;
		player.startUsingItem(hand);
		return InteractionResult.SUCCESS;
	}

	/** Throws on the first tick of use, then stops using. */
	@Override
	public void onUseTick(Level level, LivingEntity user, ItemStack stack, int ticksRemaining) {
		if (level.isClientSide() || !(user instanceof ServerPlayer player)) return;
		ItemStack ammo = findAmmo(player);
		if (player.hasInfiniteMaterials() || !ammo.isEmpty()) {
			// Before the sack is used up: an emptied stack no longer names its item.
			player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
			WebSackProjectile sack = WebSackProjectile.shoot(player);
			if (player.hasInfiniteMaterials()) {
				sack.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
			} else {
				ammo.shrink(1);
			}
		}
		player.releaseUsingItem();
	}

	private ItemStack findAmmo(Player player) {
		ItemStack held = ProjectileWeaponItem.getHeldProjectile(player, this::isAmmo);
		if (!held.isEmpty()) return held;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (isAmmo(stack)) return stack;
		}
		return ItemStack.EMPTY;
	}

	private boolean isAmmo(ItemStack stack) {
		return stack.is(SnowflakeSpiderModule.WEB_SACK);
	}
}
