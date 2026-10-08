package com.morecritters.fabric.module.wandering_collector;

import com.morecritters.fabric.ids.CorpseGearIds;
import com.morecritters.fabric.ids.ShipFittingsIds;
import com.morecritters.fabric.ids.NightshroomIds;
import com.morecritters.fabric.ids.GhostlyWoodIds;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.ids.ArmossilloIds;
import com.morecritters.fabric.ids.AvoiderIds;
import com.morecritters.fabric.ids.BlubberfishIds;
import com.morecritters.fabric.ids.BombJellyIds;
import com.morecritters.fabric.ids.BouncelizardIds;
import com.morecritters.fabric.ids.BunbugIds;
import com.morecritters.fabric.ids.CorpseCrewIds;
import com.morecritters.fabric.ids.CreeblossomIds;
import com.morecritters.fabric.ids.CustodianIds;
import com.morecritters.fabric.ids.DripperIds;
import com.morecritters.fabric.ids.GravediggerIds;
import com.morecritters.fabric.ids.IropodIds;
import com.morecritters.fabric.ids.KelpireIds;
import com.morecritters.fabric.ids.MightshroomIds;
import com.morecritters.fabric.ids.NauticrawlIds;
import com.morecritters.fabric.ids.NervoidIds;
import com.morecritters.fabric.ids.ShadeletIds;
import com.morecritters.fabric.ids.ShimmerwingIds;
import com.morecritters.fabric.ids.ShockCubeIds;
import com.morecritters.fabric.ids.ShriekbatIds;
import com.morecritters.fabric.ids.SnowflakeSpiderIds;
import com.morecritters.fabric.ids.TreepletIds;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

/**
 * Teaches players the mod's recipes as soon as they carry an ingredient: holding raw bunbug meat
 * unlocks the bunbug cooking recipes, and so on. The original (RecipeGiverProcedure) checked
 * every player every tick; this checks once a second. Items are looked up by id, so ingredients
 * of modules that are not ported are never found, and recipes that do not exist are skipped by
 * {@link ServerPlayer#awardRecipesByKey}.
 */
final class RecipeUnlocks {
	private static final int CHECK_INTERVAL = 20;

	/** Holding any of {@code items} unlocks {@code recipes}. */
	private record Unlock(List<Identifier> items, List<ResourceKey<Recipe<?>>> recipes) {}

	private static final List<Unlock> UNLOCKS = new ArrayList<>();

	static {
		unlock(List.of(BunbugIds.Items.BUNBUG_EGGS), "bunbug_caviar_crf");
		unlock(List.of(BunbugIds.Items.RAW_BUNBUG_MEAT), "bunbug_cook_1", "bunbug_cook_2", "bunbug_cook_3", "critter_kebab_crf");
		unlock(List.of(BunbugIds.Items.COOKED_BUNBUG_MEAT), "bunbug_burger_crf");
		unlock(List.of(BlubberfishIds.Items.RAW_BLUBBERFISH), "blubberfish_cook_1", "blubberfish_cook_2", "blubberfish_cook_3", "critter_kebab_crf");
		unlock(List.of(SnowflakeSpiderIds.Items.FREEZING_STRING), "coldstone_crf", "coldstone_bricks_crf", "coldstone_chiseled_crf", "freezing_cobweb_crf", "coldstone_crf_11", "websack_crf");
		unlock(List.of(SnowflakeSpiderIds.Blocks.COLDSTONE), "coldstone_stairs_crf_1", "coldstone_slab_crf_2", "coldstone_wall_crf_1");
		unlock(List.of(SnowflakeSpiderIds.Blocks.COLDSTONE_BRICKS), "coldstone_bricks_stairs_crf_1", "coldstone_bricks_slab_crf_2", "coldstone_bricks_wall_crf_1", "coldstone_crf_5");
		unlock(List.of(SnowflakeSpiderIds.Blocks.POLISHED_COLDSTONE), "coldstone_polished_stairs_crf_1", "coldstone_polished_slab_crf_2", "coldstone_polished_wall_crf_1");
		unlock(List.of(ShriekbatIds.Items.SHRIEKBAT_WING), "shriekbomb_crf_1", "shriekbat_soup_crf");
		unlock(List.of(CreeblossomIds.Items.BLOSSOMBUSH_SEED), "blossombush_crf");
		unlock(List.of(ShockCubeIds.Items.BOTTLEO_ELECTRICITY), "taser_crf", "electric_blossombush_crf");
		unlock(List.of(ArmossilloIds.Items.STURDY_SHELLS), "sturdy_shell_block_crf", "sturdy_chestplate_crf", "biting_shield_crf");
		unlock(List.of(ArmossilloIds.Blocks.STURDY_SHELL_BLOCK), "sturdy_shell_crf");
		unlock(List.of(MightshroomIds.Blocks.VITA_SHROOM), "life_stew_crf", "purgatorial_mix_crf");
		unlock(List.of(MightshroomIds.Blocks.MORI_SHROOM), "death_stew_crf", "purgatorial_mix_crf");
		unlock(List.of(NightshroomIds.Items.ANCIENT_BONE), "ancient_skeleton_crf", "fungal_staff_crf");
		unlock(List.of(NightshroomIds.Items.ANCIENT_SKELETON_ITEM), "ancient_skeleton_exhibit_crf");
		unlock(List.of(BouncelizardIds.Items.BOUNCEBERRY), "bounceberry_jam_crf");
		unlock(List.of(BouncelizardIds.Items.BOUNCEBERRY_JAM), "bounceberry_sandwich_crf");
		unlock(List.of(BouncelizardIds.Blocks.BOUNCELIZARD_EGG), "cooked_bouncelizard_egg_crf");
		unlock(List.of(vanilla(Items.IRON_NUGGET)), "fossil_display_crf");
		unlock(List.of(AvoiderIds.Items.AVOIDER_TAIL), "booster_pump_crf", "jelly_torpedo_crf");
		unlock(List.of(BombJellyIds.Items.EXPLOSIVE_JELLY), "jelly_torpedo_crf");
		unlock(List.of(IropodIds.Items.MOLDED_SHELL), "iropod_helmet_crf");
		unlock(List.of(NauticrawlIds.Items.SHELL_PIECES), "nautical_helmet_crf", "nautical_axe_crf", "nauticrawl_shell_crf", "sails_crf");
		unlock(List.of(ShimmerwingIds.Items.END_DUST), "chrysalis_crf");
		unlock(List.of(vanilla(Items.SUGAR)), "tooth_melter_crf", "sprinkles_crf");
		unlock(List.of(vanilla(Items.HONEYCOMB)), "sprinkles_crf");
		unlock(List.of(TreepletIds.Items.EERIE_BARK), "eerie_log_crf_1", "eerie_wood_crf_1", "eerie_dart_crf");
		unlock(List.of(TreepletIds.Blocks.BLACK_RESIN_BLOCK), "black_resin_crf");
		unlock(List.of(TreepletIds.Items.BLACK_RESIN_CLUMP), "black_resin_block_crf", "black_resin_cook");
		unlock(List.of(TreepletIds.Items.BLACK_RESIN_BRICK), "resin_brick_crf_1");
		unlock(List.of(TreepletIds.Blocks.BLACK_RESIN_BRICKS), "black_resin_stairs_crf_1", "black_resin_slab_crf_1", "black_resin_wall_crf_1", "black_resin_chiseled_crf_1");
		unlock(List.of(NervoidIds.Items.LOST_NERVE), "lost_nerve_cook_1", "lost_nerve_cook_2", "lost_nerve_cook_3");
		unlock(List.of(NervoidIds.Items.COOKED_NERVE), "nerval_salad_crf", "popped_mix_crf");
		unlock(List.of(GhostlyWoodIds.Blocks.GHOSTLY_LOG, GhostlyWoodIds.Blocks.GHOSTLY_WOOD, GhostlyWoodIds.Blocks.STRIPPED_GHOSTLY_LOG, GhostlyWoodIds.Blocks.STRIPPED_GHOSTLY_WOOD), "ghostly_crf_1", "ghostly_crf_2", "ghostly_crf_6");
		unlock(List.of(GhostlyWoodIds.Blocks.GHOSTLY_PLANKS), "ghostly_crf_10", "ghostly_crf_12", "ghostly_crf_15", "ghostly_crf_17", "ghostly_crf_19", "ghostly_crf_25", "ghostly_crf_26", "ghostly_crf_28", "mosaic_crf", "wet_crf", "petrified_planks_crf");
		unlock(List.of(GhostlyWoodIds.Blocks.FISH_BONE_BLOCK), "fish_bone_crf_1");
		unlock(List.of(GhostlyWoodIds.Items.FISH_BONE), "fish_bone_crf_2", "fish_bone_crf_3", "fish_bone_crf_6");
		unlock(List.of(CorpseCrewIds.Items.TATTERED_CLOTH), "fish_bone_crf_6", "pirate_armor_crf_1", "pirate_armor_crf_3", "pirate_armor_crf_4", "pirate_armor_crf_5", "tattered_flag_crf");
		unlock(List.of(ShipFittingsIds.Items.CANNON_BALL), "cold_cannon_ball_crf", "fire_cannon_ball_crf", "slime_cannon_ball_crf", "electric_cannon_ball_crf", "combusting_cannon_ball_crf");
		unlock(List.of(ShadeletIds.Items.SHARK_TOOTH), "tooth_syringe_crf_2");
		unlock(List.of(CorpseGearIds.Items.HARDTACK), "hardtack_piece_crf");
		unlock(List.of(CorpseGearIds.Items.HARDTACK_PIECE), "hardtack_crf");
		unlock(List.of(CorpseCrewIds.Items.CAPTAINS_HEART), "corpse_parrot_crf");
		unlock(List.of(GravediggerIds.Items.GRAVEDIGGER_APPENDAGE), "grave_brush_crf");
		unlock(List.of(ArmossilloIds.Items.GLOWING_OOZE), "ooze_block_crf", "cut_ooze_crf", "ooze_crf", "ooze_rod_crf");
		unlock(List.of(DripperIds.Items.DRIPPER_REMAINS), "wall_mask_crf", "slashklub_crf");
		unlock(List.of(CustodianIds.Items.SCULK_ESSENCE), "custodian_core_crf");
		unlock(List.of(KelpireIds.Items.KELPIRE_ROLL_PIECE), "kelpire_roll_crf");
		unlock(List.of(NauticrawlIds.Items.NAUTICRAWL_TENTACLE), "nauticrawl_ramen_crf");
		unlock(List.of(vanilla(Items.DRIED_KELP)), "kelp_carpet_crf");
		unlock(List.of(GhostlyWoodIds.Items.ECTOMETAL), "ectometal_block_crf", "ectometal_nail_crf", "ectometal_screw_crf", "ectometal_railing_crf", "giant_chain_crf");
		unlock(List.of(GhostlyWoodIds.Blocks.ECTOMETAL_BLOCK), "ectometal_crf");
		unlock(List.of(GhostlyWoodIds.Blocks.WET_GHOSTLY_PLANKS), "mosaic_crf_3");
		unlock(List.of(GhostlyWoodIds.Blocks.PETRIFIED_GHOSTLY_PLANKS), "mosaic_crf_2", "petrified_planks_craft_2", "petrified_planks_craft_3", "petrified_planks_craft_4");
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(RecipeUnlocks::tick);
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % CHECK_INTERVAL != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			for (Unlock unlock : UNLOCKS) {
				if (carriesAny(player.getInventory(), unlock.items())) {
					player.awardRecipesByKey(unlock.recipes());
				}
			}
		}
	}

	private static boolean carriesAny(Inventory inventory, List<Identifier> items) {
		for (Identifier id : items) {
			Item item = BuiltInRegistries.ITEM.getValue(id);
			if (item != Items.AIR && inventory.contains(stack -> stack.is(item))) {
				return true;
			}
		}
		return false;
	}

	private static void unlock(List<Identifier> items, String... recipes) {
		List<ResourceKey<Recipe<?>>> keys = new ArrayList<>();
		for (String recipe : recipes) {
			keys.add(ResourceKey.create(Registries.RECIPE, MoreCritters.id(recipe)));
		}
		UNLOCKS.add(new Unlock(items, List.copyOf(keys)));
	}

	private static Identifier vanilla(Item item) {
		return BuiltInRegistries.ITEM.getKey(item);
	}

	private RecipeUnlocks() {}
}
