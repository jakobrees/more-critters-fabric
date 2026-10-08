package com.morecritters.fabric.module.balloon_rat;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A cupcake baked with a toxin bladder: eating it gives ten seconds of that poison. */
final class ToxicCupcakeItem extends Item {
	private static final int POISON_TICKS = 200;

	private final Holder<MobEffect> poison;

	ToxicCupcakeItem(Holder<MobEffect> poison, Properties properties) {
		super(properties);
		this.poison = poison;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
		ItemStack remainder = super.finishUsingItem(stack, level, eater);
		if (!level.isClientSide()) {
			eater.addEffect(new MobEffectInstance(poison, POISON_TICKS, 0, false, true));
		}
		return remainder;
	}
}
