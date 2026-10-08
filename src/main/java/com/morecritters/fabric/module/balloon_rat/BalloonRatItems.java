package com.morecritters.fabric.module.balloon_rat;

import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.BalloonRatIds;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

/**
 * Cupcakes (a balloon rat treat), the poisoned cupcakes that set a soldier balloon rat's poison,
 * and the toxin bladders they are baked with.
 */
public final class BalloonRatItems {
	public static Item CUPCAKE, CUPCAKE_STAGNATION, CUPCAKE_MUSCLE_ACHE, CUPCAKE_HALLUCINAZIUM;
	public static Item TOXIN_BLADDER_STAGNATION, TOXIN_BLADDER_MUSCLE_ACHE, TOXIN_BLADDER_HALLUCINAZIUM;

	private static final int CUPCAKE_STACK = 16;
	private static final FoodProperties CUPCAKE_FOOD = new FoodProperties.Builder().nutrition(6).saturationModifier(0.6F).build();
	private static final FoodProperties TOXIC_CUPCAKE_FOOD = new FoodProperties.Builder().nutrition(4).saturationModifier(0.3F).build();

	static void register() {
		CUPCAKE = Registration.item(BalloonRatIds.Items.CUPCAKE, new Item.Properties().stacksTo(CUPCAKE_STACK).food(CUPCAKE_FOOD));
		CUPCAKE_STAGNATION = toxicCupcake(BalloonRatIds.Items.CUPCAKE_STAGNATION, BalloonRatEffects.STAGNATION);
		CUPCAKE_MUSCLE_ACHE = toxicCupcake(BalloonRatIds.Items.CUPCAKE_MUSCLE_ACHE, BalloonRatEffects.MUSCLE_ACHE);
		CUPCAKE_HALLUCINAZIUM = toxicCupcake(BalloonRatIds.Items.CUPCAKE_HALLUCINAZIUM, BalloonRatEffects.HALLUCINAZIUM);

		TOXIN_BLADDER_STAGNATION = toxinBladder(BalloonRatIds.Items.TOXIN_BLADDER_STAGNATION);
		TOXIN_BLADDER_MUSCLE_ACHE = toxinBladder(BalloonRatIds.Items.TOXIN_BLADDER_MUSCLE_ACHE);
		TOXIN_BLADDER_HALLUCINAZIUM = toxinBladder(BalloonRatIds.Items.TOXIN_BLADDER_HALLUCINAZIUM);
	}

	private static Item toxicCupcake(Identifier id, Holder<MobEffect> poison) {
		return Registration.item(id, properties -> new ToxicCupcakeItem(poison, properties),
			Tooltips.describe(new Item.Properties().stacksTo(CUPCAKE_STACK).food(TOXIC_CUPCAKE_FOOD), id.getPath(), 1));
	}

	private static Item toxinBladder(Identifier id) {
		return Registration.item(id, Tooltips.describe(new Item.Properties(), id.getPath(), 1));
	}

	private BalloonRatItems() {}
}
