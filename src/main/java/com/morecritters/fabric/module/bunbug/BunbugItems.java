package com.morecritters.fabric.module.bunbug;

import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.BunbugIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

import java.util.HashMap;
import java.util.Map;

/** The bunbug's food items. Crusts come in every decoration the bug itself can wear. */
final class BunbugItems {
	static Item rawMeat, cookedMeat, burger, caviar, eggs;
	/** Crust items keyed by {@code icing * 100 + sprinkles * 10 + berries}. */
	private static final Map<Integer, Item> CRUSTS = new HashMap<>();

	static void register() {
		rawMeat = food(BunbugIds.Items.RAW_BUNBUG_MEAT, 2, 0.1F);
		cookedMeat = food(BunbugIds.Items.COOKED_BUNBUG_MEAT, 6, 0.5F);
		burger = Registration.item(BunbugIds.Items.BUNBUG_BURGER, BunbugBurgerItem::new, foodProperties(10, 0.8F));
		caviar = Registration.item(BunbugIds.Items.BUNBUG_CAVIAR, foodProperties(6, 0.6F).stacksTo(1).usingConvertsTo(Items.BOWL));
		eggs = Registration.item(BunbugIds.Items.BUNBUG_EGGS, BunbugEggsItem::new, new Item.Properties());

		crust(0, 0, 0, BunbugIds.Items.BUNBUG_CRUST, 2, 0.2F, 0);
		crust(1, 0, 0, BunbugIds.Items.BUNBUG_CRUST_ICED_SUGAR, 5, 0.3F, 2);
		crust(2, 0, 0, BunbugIds.Items.BUNBUG_CRUST_ICED_CHOCOLATE, 5, 0.3F, 2);
		crust(1, 1, 0, BunbugIds.Items.BUNBUG_CRUST_ICED_SUGAR_SPRINKLED, 6, 0.3F, 3);
		crust(2, 1, 0, BunbugIds.Items.BUNBUG_CRUST_ICED_CHOCOLATE_SPRINKLED, 6, 0.3F, 3);
		crust(1, 1, 1, BunbugIds.Items.BUNBUG_CRUST_ICED_SUGAR_SPRINKLED_SWEET_BERRIES, 8, 0.3F, 4);
		crust(1, 1, 2, BunbugIds.Items.BUNBUG_CRUST_ICED_SUGAR_SPRINKLED_GLOW_BERRIES, 6, 0.3F, 4);
		crust(1, 1, 3, BunbugIds.Items.BUNBUG_CRUST_ICED_SUGAR_SPRINKLED_BOUNCEBERRIES, 8, 0.3F, 4);
		crust(2, 1, 1, BunbugIds.Items.BUNBUG_CRUST_ICED_CHOCOLATE_SPRINKLED_SWEET_BERRIES, 8, 0.3F, 4);
		crust(2, 1, 2, BunbugIds.Items.BUNBUG_CRUST_ICED_CHOCOLATE_SPRINKLED_GLOW_BERRIES, 6, 0.3F, 4);
		crust(2, 1, 3, BunbugIds.Items.BUNBUG_CRUST_ICED_CHOCOLATE_SPRINKLED_BOUNCEBERRIES, 8, 0.3F, 4);
		// Not shed by any bunbug; an anniversary item with its own recipe.
		Registration.item(BunbugIds.Items.BUNBUG_CRUST_STRAWBERRY_SPRINKLED_CANDLE,
			Tooltips.describe(foodProperties(6, 0.3F).rarity(Rarity.EPIC), "bunbug_crust_strawberry_sprinkled_candle", 5));
	}

	static Item crustFor(int icing, int sprinkles, int berries) {
		return CRUSTS.getOrDefault(icing * 100 + sprinkles * 10 + berries, CRUSTS.get(0));
	}

	private static void crust(int icing, int sprinkles, int berries, Identifier id, int nutrition, float saturation, int tooltipLines) {
		Item item = Registration.item(id, Tooltips.describe(foodProperties(nutrition, saturation), id.getPath(), tooltipLines));
		CRUSTS.put(icing * 100 + sprinkles * 10 + berries, item);
	}

	private static Item food(Identifier id, int nutrition, float saturation) {
		return Registration.item(id, foodProperties(nutrition, saturation));
	}

	private static Item.Properties foodProperties(int nutrition, float saturation) {
		return new Item.Properties().food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build());
	}

	private BunbugItems() {}
}
