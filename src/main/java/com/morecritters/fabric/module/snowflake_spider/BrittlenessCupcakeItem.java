package com.morecritters.fabric.module.snowflake_spider;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A cupcake brewed with a brittleness toxin bladder: eating it gives ten seconds of brittleness. */
final class BrittlenessCupcakeItem extends Item {
	private static final int BRITTLENESS_TICKS = 200;

	BrittlenessCupcakeItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
		ItemStack remainder = super.finishUsingItem(stack, level, eater);
		if (!level.isClientSide()) {
			eater.addEffect(new MobEffectInstance(SnowflakeSpiderModule.BRITTLENESS, BRITTLENESS_TICKS, 0, false, true));
		}
		return remainder;
	}
}
