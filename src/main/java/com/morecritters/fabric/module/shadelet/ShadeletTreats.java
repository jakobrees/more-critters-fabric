package com.morecritters.fabric.module.shadelet;

import com.morecritters.fabric.ids.BalloonRatIds;
import com.morecritters.fabric.ids.BouncelizardIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/** The sweets a shadelet eats and how long a sugar rush each one gives. */
final class ShadeletTreats {
	record Treat(int sugarRushTicks, SoundEvent sound, @Nullable Item leftover) {}

	private ShadeletTreats() {}

	static @Nullable Treat forItem(ItemStack stack) {
		SoundEvent eat = SoundEvents.GENERIC_EAT.value();
		if (stack.is(Items.SWEET_BERRIES) || stack.is(Items.GLOW_BERRIES) || stack.is(Items.SUGAR)) return new Treat(2400, eat, null);
		if (stack.is(Items.COOKIE) || stack.is(Items.HONEYCOMB)) return new Treat(3600, eat, null);
		if (stack.is(Items.CAKE)) return new Treat(6000, eat, null);
		if (stack.is(Items.PUMPKIN_PIE)) return new Treat(4800, eat, null);
		if (stack.is(Items.HONEY_BOTTLE)) return new Treat(3600, SoundEvents.HONEY_DRINK.value(), Items.GLASS_BOTTLE);
		if (stack.is(ShadeletModule.TOOTH_MELTER)) return new Treat(12000, eat, Items.BOWL);
		if (stack.is(BuiltInRegistries.ITEM.getValue(BalloonRatIds.Items.CUPCAKE))) return new Treat(1200, eat, null);
		if (stack.is(BuiltInRegistries.ITEM.getValue(BouncelizardIds.Items.BOUNCEBERRY))) return new Treat(1200, eat, null);
		if (stack.is(BuiltInRegistries.ITEM.getValue(BouncelizardIds.Items.BOUNCEBERRY_JAM))) {
			return new Treat(2400, SoundEvents.GENERIC_DRINK.value(), Items.GLASS_BOTTLE);
		}
		return null;
	}
}
