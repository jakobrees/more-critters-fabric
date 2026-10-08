package com.morecritters.fabric.module.shock_cube;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** A bottled shock cube. Drinking it electrocutes you for 100 seconds and gives the bottle back. */
public class BottleOfElectricityItem extends Item {
	private static final int ELECTROCUTE_TICKS = 2000;

	BottleOfElectricityItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity drinker) {
		ItemStack rest = super.finishUsingItem(stack, level, drinker);
		if (!level.isClientSide()) {
			level.playSound(null, drinker.blockPosition(), ShockCubeModule.ELECTRIC_BOTTLE_DRINK_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
			drinker.addEffect(new MobEffectInstance(ShockCubeModule.ELECTROCUTED, ELECTROCUTE_TICKS, 0, false, false));
		}
		ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
		if (rest.isEmpty()) return bottle;
		if (drinker instanceof Player player && !player.hasInfiniteMaterials()) {
			if (!player.getInventory().add(bottle)) player.spawnAtLocation((net.minecraft.server.level.ServerLevel) player.level(), bottle);
		}
		return rest;
	}
}
