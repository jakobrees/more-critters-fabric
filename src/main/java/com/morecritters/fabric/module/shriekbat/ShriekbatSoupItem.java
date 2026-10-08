package com.morecritters.fabric.module.shriekbat;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Shriekbat soup: half an hour of Shriek Resistance. */
public class ShriekbatSoupItem extends Item {
	private static final int RESISTANCE_TICKS = 36_000;

	public ShriekbatSoupItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!level.isClientSide()) entity.addEffect(new MobEffectInstance(ShriekbatModule.SHRIEK_RESISTANCE, RESISTANCE_TICKS, 0, false, true));
		return super.finishUsingItem(stack, level, entity);
	}
}
