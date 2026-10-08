package com.morecritters.fabric.module.stincarp;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A cupcake brewed with a toxin bladder: eating it gives ten seconds of asphyxiation. */
final class PoisonedCupcakeItem extends Item {
	private static final int ASPHYXIATION_TICKS = 200;

	PoisonedCupcakeItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
		ItemStack remainder = super.finishUsingItem(stack, level, eater);
		if (!level.isClientSide()) {
			eater.addEffect(new MobEffectInstance(StincarpModule.ASPHYXIATION, ASPHYXIATION_TICKS, 0, false, true));
		}
		return remainder;
	}
}
