package com.morecritters.fabric.module.bouncelizard;

import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.ids.BouncelizardIds;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

/** Bounceberries and the food made from them and from bouncelizard eggs. Berries, jam and sandwiches put a spring in your step. */
public final class BouncelizardItems {
	private static final int JUMP_BOOST_TICKS = 200;

	public static Item bounceberry;

	static void register() {
		// Jump Boost II, edible even when full.
		bounceberry = Registration.item(BouncelizardIds.Items.BOUNCEBERRY, new Item.Properties()
			.food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3F).alwaysEdible().build(), jumpBoost(1)));
		// Jump Boost III, drunk from a bottle that is handed back.
		Registration.item(BouncelizardIds.Items.BOUNCEBERRY_JAM, new Item.Properties().stacksTo(16)
			.food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.5F).build(),
				Consumables.defaultDrink().onConsume(jumpBoostEffect(2)).build())
			.usingConvertsTo(Items.GLASS_BOTTLE));
		// Jump Boost III.
		Registration.item(BouncelizardIds.Items.BOUNCEBERRY_SANDWICH, new Item.Properties()
			.food(new FoodProperties.Builder().nutrition(10).saturationModifier(0.8F).build(), jumpBoost(2)));
		Registration.item(BouncelizardIds.Items.COOKED_BOUNCELIZARD_EGG, new Item.Properties()
			.food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.8F).build()));
	}

	private static Consumable jumpBoost(int amplifier) {
		return Consumables.defaultFood().onConsume(jumpBoostEffect(amplifier)).build();
	}

	private static ApplyStatusEffectsConsumeEffect jumpBoostEffect(int amplifier) {
		return new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, JUMP_BOOST_TICKS, amplifier, false, true));
	}

	static boolean isBounceberry(ItemStack stack) {
		return stack.is(bounceberry);
	}

	private BouncelizardItems() {}
}
