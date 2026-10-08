package com.morecritters.fabric.module.bunbug;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Eating one grants the "eat a bunbug burger" advancement. */
final class BunbugBurgerItem extends Item {
	BunbugBurgerItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
		ItemStack remainder = super.finishUsingItem(stack, level, eater);
		Advancements.award(eater, MoreCritters.id("eat_bunbug_burger"));
		return remainder;
	}
}
