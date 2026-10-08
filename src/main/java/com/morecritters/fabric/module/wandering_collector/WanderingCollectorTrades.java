package com.morecritters.fabric.module.wandering_collector;

import com.morecritters.fabric.MoreCritters;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What the wandering collector pays for an item put in his trade slot. The original was one
 * 1.7k-line if-chain (TradeProcedure1Procedure); this is the same table as data. Items are named
 * by id and looked up when a trade is priced, so critters from modules that are not ported yet
 * are simply absent. Only items in {@code #minecraft:tradeable} are considered at all.
 */
public final class WanderingCollectorTrades {
	public static final TagKey<Item> TRADEABLE = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("tradeable"));
	public static final TagKey<Item> FOSSILS = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("fossils"));

	private static final Item EMERALD = Items.EMERALD, IRON_NUGGET = Items.IRON_NUGGET, GOLD_INGOT = Items.GOLD_INGOT, DIAMOND = Items.DIAMOND;

	/** Item id → payment, in the order of the original chain. */
	private static final Map<Identifier, ItemStack> PRICES = new LinkedHashMap<>();
	private static final int FOSSIL_PRICE_DIAMONDS = 3;

	static {
		buy("bunbug_eggs", EMERALD, 1);
		buy("raw_bunbug_meat", EMERALD, 1);
		buy("cooked_bunbug_meat", EMERALD, 1);
		buy("sackof_freezing", EMERALD, 3);
		buy("freezing_string", EMERALD, 1);
		buy("freezing_cobweb", EMERALD, 2);
		buy("web_sack", EMERALD, 2);
		buy("shriekbat_wing", EMERALD, 1);
		buy("shriek_bomb", EMERALD, 2);
		buy("shriekbat_soup", EMERALD, 2);
		buy("blossombush_seed", EMERALD, 1);
		buy("blossombush", EMERALD, 3);
		buy("closed_blossombush", EMERALD, 3);
		buy("bouncelizard_egg", EMERALD, 1);
		buy("bounceberry", IRON_NUGGET, 1);
		buy("bounceberry_bush_empty", EMERALD, 1);
		buy("bottleo_electricity", EMERALD, 3);
		buy("tazegun", DIAMOND, 3);
		buy("electric_blossombush", EMERALD, 5);
		buy("closed_electric_blossombush", EMERALD, 5);
		buy("sturdy_shells", EMERALD, 3);
		buy("sturdy_chestplate", EMERALD, 5);
		buy("biting_shield", EMERALD, 5);
		buy("shimmerworm_item", EMERALD, 1);
		buy("shimmering_chrysalis", EMERALD, 2);
		buy("end_dust", EMERALD, 3);
		buy("vita_shroom", EMERALD, 1);
		buy("mori_shroom", EMERALD, 1);
		buy("purgatorial_mixture", EMERALD, 2);
		buy("death_stew", EMERALD, 3);
		buy("life_stew", EMERALD, 5);
		buy("ancient_bone", EMERALD, 2);
		buy("fungal_flesh", EMERALD, 2);
		buy("regenerative_flesh", EMERALD, 3);
		buy("fungal_staff", DIAMOND, 5);
		buy("bunbug_caviar", EMERALD, 2);
		buy("bounceberry_jam", EMERALD, 2);
		buy("cooked_bouncelizard_egg", EMERALD, 3);
		buy("bounceberry_sandwich", EMERALD, 5);
		buy("explosive_jelly", EMERALD, 1);
		buy("small_bomb_jelly_bucket", EMERALD, 1);
		buy("medium_bomb_jelly_bucket", EMERALD, 2);
		buy("large_bomb_jelly_bucket", EMERALD, 3);
		buy("avoider_tail", EMERALD, 1);
		buy("avoider_bucket_bucket", EMERALD, 1);
		buy("booster_pump", EMERALD, 3);
		buy("jelly_torpedo_item", EMERALD, 2);
		buy("molded_shell", EMERALD, 2);
		buy("iropod_bucket_bucket", EMERALD, 1);
		buy("black_iropod_bucket_bucket", EMERALD, 5);
		buy("iropod_helmet_helmet", EMERALD, 3);
		buy("raw_blubberfish", EMERALD, 1);
		buy("cooked_blubberfish", EMERALD, 1);
		buy("blubberfish_bucket_bucket", EMERALD, 1);
		buy("shell_pieces", EMERALD, 1);
		buy("nautical_helmet_helmet", EMERALD, 5);
		buy("nauticrawl_shell", EMERALD, 9);
		buy("blubberfish_fry_bucket_bucket", EMERALD, 1);
		buy("avoider_fry_bucket_bucket", EMERALD, 1);
		buy("nautical_axe", EMERALD, 5);
		buy("stinarp_bucket_bucket", EMERALD, 5);
		buy("toxin_bladder_stagnation", EMERALD, 2);
		buy("toxin_bladder_muscle_ache", EMERALD, 2);
		buy("toxin_bladder_brittleness", EMERALD, 2);
		buy("toxin_bladder_hallucinazium", EMERALD, 2);
		buy("toxin_bladder_asphyxiation", EMERALD, 2);
		buy("tooth_melter", EMERALD, 10);
		buy("eerie_bark", EMERALD, 2);
		buy("eerie_dart", EMERALD, 1);
		buy("black_resin_clump", EMERALD, 2);
		buy("black_resin_brick", EMERALD, 2);
		buy("spinal_fluid_bottle", EMERALD, 5);
		buy("mysterious_virus_bottle", EMERALD, 15);
		buy("lost_nerve", EMERALD, 2);
		buy("cooked_nerve", EMERALD, 2);
		buy("nerval_salad", EMERALD, 5);
		buy("popped_nerval_mixture", EMERALD, 5);
		buy("nervoid_brain", EMERALD, 3);
		buy("decomposing_nervoid_brain", EMERALD, 2);
		buy("rotten_nervoid_brain", EMERALD, 1);
		buy("tattered_jolly_roger", GOLD_INGOT, 5);
		buy("fish_bone", GOLD_INGOT, 1);
		buy("tattered_cloth", GOLD_INGOT, 1);
		buy("cannon_ball", GOLD_INGOT, 1);
		buy("pearl", DIAMOND, 2);
		buy("infused_cannon_ball_cold", EMERALD, 3);
		buy("infused_cannon_ball_fire", EMERALD, 3);
		buy("infused_cannon_ball_slime", EMERALD, 3);
		buy("rum_bottle", GOLD_INGOT, 2);
		buy("cutlass", GOLD_INGOT, 5);
		buy("shark_tooth", GOLD_INGOT, 2);
		buy("tooth_syringe", GOLD_INGOT, 3);
		buy("captains_heart", DIAMOND, 5);
		buy("corpse_parrot_item", DIAMOND, 5);
		buy("hardtack", GOLD_INGOT, 1);
		buy("infested_hardtack", GOLD_INGOT, 2);
		buy("custodian_core", DIAMOND, 5);
		buy("sculk_essence", DIAMOND, 5);
		buy("slashklub", EMERALD, 3);
		buy("dripper_remains", EMERALD, 1);
		buy("dripstone_wall_mask", EMERALD, 2);
		buy("ramchu_bucket_no_oil_bucket", EMERALD, 2);
		buy("ramchu_bucket_no_shell_bucket", EMERALD, 2);
		buy("ramchu_bucket_bucket", EMERALD, 2);
		buy("ramchu_fry_bucket_bucket", EMERALD, 1);
		buy("grave_brush", EMERALD, 3);
		buy("moss_clump", DIAMOND, 1);
		buy("ramchu_oil_bottle", EMERALD, 1);
		buy("ooze_rod", EMERALD, 2);
		buy("glowing_ooze", EMERALD, 1);
		buy("gravedigger_appendage", EMERALD, 1);
		buy("kelpire_roll_piece", EMERALD, 1);
		buy("kelpire_rolls", EMERALD, 5);
		buy("critter_kebab", EMERALD, 3);
		buy("nauticrawl_ramen", EMERALD, 3);
		buy("nauticrawl_tentacle", EMERALD, 3);
		buy("infused_cannon_ball_electric", EMERALD, 3);
		buy("infused_cannon_ball_combusting", EMERALD, 3);
		buy("ectometal", GOLD_INGOT, 1);
		buy("chattering_teeth_item", GOLD_INGOT, 3);
		buy("blubber", EMERALD, 1);
		buy("goobulb", EMERALD, 1);
		buy("iroball_item", EMERALD, 1);
		buy("spiked_iroball", EMERALD, 2);
		buy("birch_snow_cone", EMERALD, 2);
		// Pirate armour fell into the original's catch-all branch, which paid 3 gold.
		for (String piece : new String[] {"pirate_helmet", "pirate_chestplate", "pirate_leggings", "pirate_boots"}) {
			buy(piece, GOLD_INGOT, 3);
		}
	}

	private WanderingCollectorTrades() {}

	/** "bunbug_eggs" names a More Critters item, "minecraft:..." a vanilla one. */
	private static void buy(String item, Item currency, int count) {
		Identifier id = item.contains(":") ? Identifier.parse(item) : MoreCritters.id(item);
		PRICES.putIfAbsent(id, new ItemStack(currency, count));
	}

	/** The payment for {@code offered}, or {@link ItemStack#EMPTY} when the collector does not buy it. */
	public static ItemStack priceFor(ItemStack offered) {
		if (offered.isEmpty() || !offered.is(TRADEABLE)) {
			return ItemStack.EMPTY;
		}
		ItemStack price = PRICES.get(BuiltInRegistries.ITEM.getKey(offered.getItem()));
		if (price != null) {
			return price.copy();
		}
		if (offered.is(FOSSILS)) {
			return new ItemStack(DIAMOND, FOSSIL_PRICE_DIAMONDS);
		}
		return ItemStack.EMPTY;
	}
}
