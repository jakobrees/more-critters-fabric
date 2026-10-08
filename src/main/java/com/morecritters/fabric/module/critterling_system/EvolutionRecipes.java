package com.morecritters.fabric.module.critterling_system;

import com.morecritters.fabric.ids.BombJellyIds;
import com.morecritters.fabric.ids.BlubberfishIds;
import com.morecritters.fabric.ids.BunbugIds;
import com.morecritters.fabric.ids.CritterlingsAIds;
import com.morecritters.fabric.ids.CritterlingsBIds;
import com.morecritters.fabric.ids.CritterlingsCIds;
import com.morecritters.fabric.ids.CritterlingsDIds;
import com.morecritters.fabric.ids.CritterlingsEIds;
import com.morecritters.fabric.ids.NervoidIds;
import com.morecritters.fabric.ids.ShadeletIds;
import com.morecritters.fabric.ids.ShimmerwingIds;
import com.morecritters.fabric.ids.ShriekbatIds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * What the evolution table turns each critterling sack into: a normal sack plus two ingredients
 * becomes the rare sack, a rare sack plus three ingredients the epic one (each also costs one
 * evolite). The original's 1.4k-line {@code EvoTableProcedure1Procedure} as a table. Items are
 * looked up by id when used, so recipes of critterlings or ingredients whose module is missing
 * simply never match.
 */
public final class EvolutionRecipes {
	/** @param ingredients the items for the table's first, second and (rare sacks only) third ingredient slot */
	public record Recipe(Identifier result, List<Identifier> ingredients) {
		public ItemStack resultStack() {
			return stackOf(this.result);
		}

		public ItemStack ingredientStack(int index) {
			return index < this.ingredients.size() ? stackOf(this.ingredients.get(index)) : ItemStack.EMPTY;
		}

		/** Whether the item matches the ingredient wanted in that slot (an unused third slot wants nothing). */
		public boolean matches(int index, ItemStack stack) {
			if (index >= this.ingredients.size()) return true;
			return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(this.ingredients.get(index));
		}
	}

	private static final Map<Identifier, Recipe> BY_SACK = new HashMap<>();

	static {
		// critterlings_a
		evolve(CritterlingsAIds.Items.CRITTERLING_SACK_CUBEFROG, CritterlingsAIds.Items.CRITTERLING_SACK_CUBEFROG_RARE, Items.INK_SAC, Items.HONEYCOMB);
		evolve(CritterlingsAIds.Items.CRITTERLING_SACK_CUBEFROG_RARE, CritterlingsAIds.Items.CRITTERLING_SACK_CUBEFROG_EPIC, Items.SLIME_BALL, Items.QUARTZ, Items.FERMENTED_SPIDER_EYE);
		evolve(CritterlingsAIds.Items.CRITTERLING_SACK_PLAINSWYRM, CritterlingsAIds.Items.CRITTERLING_SACK_PLAINSWYRM_RARE, BombJellyIds.Items.EXPLOSIVE_JELLY, BlubberfishIds.Items.SPRINKLES);
		evolve(CritterlingsAIds.Items.CRITTERLING_SACK_PLAINSWYRM_RARE, CritterlingsAIds.Items.CRITTERLING_SACK_PLAINSWYRM_EPIC, ShadeletIds.Items.SHARK_TOOTH, id(Items.CHORUS_FRUIT), id(Items.BONE));
		evolve(CritterlingsAIds.Items.CRITTERLING_SACK_DUNGER, CritterlingsAIds.Items.CRITTERLING_SACK_DUNGER_RARE, Items.COPPER_INGOT, Items.BEETROOT);
		evolve(CritterlingsAIds.Items.CRITTERLING_SACK_DUNGER_RARE, CritterlingsAIds.Items.CRITTERLING_SACK_DUNGER_EPIC, Items.GLOW_INK_SAC, Items.PRISMARINE_CRYSTALS, Items.AMETHYST_SHARD);
		evolve(CritterlingsAIds.Items.CRITTERLING_SACK_SNEK, CritterlingsAIds.Items.CRITTERLING_SACK_SNEK_RARE, Items.GOLD_INGOT, Items.REDSTONE);
		evolve(CritterlingsAIds.Items.CRITTERLING_SACK_SNEK_RARE, CritterlingsAIds.Items.CRITTERLING_SACK_SNEK_EPIC, ShimmerwingIds.Items.END_DUST, id(Items.CHERRY_LEAVES), id(Items.POPPY));
		// critterlings_b
		evolve(CritterlingsBIds.Items.CRITTERLING_SACK_EXPY, CritterlingsBIds.Items.CRITTERLING_SACK_EXPY_RARE, Items.GLOWSTONE_DUST, Items.QUARTZ);
		evolve(CritterlingsBIds.Items.CRITTERLING_SACK_EXPY_RARE, CritterlingsBIds.Items.CRITTERLING_SACK_EXPY_EPIC, Items.REDSTONE, Items.GLOWSTONE, Items.PHANTOM_MEMBRANE);
		evolve(CritterlingsBIds.Items.CRITTERLING_SACK_SCOWL, CritterlingsBIds.Items.CRITTERLING_SACK_SCOWL_RARE, Items.RABBIT_HIDE, Items.GLOW_BERRIES);
		evolve(CritterlingsBIds.Items.CRITTERLING_SACK_SCOWL_RARE, CritterlingsBIds.Items.CRITTERLING_SACK_SCOWL_EPIC, Items.INK_SAC, Items.PRISMARINE_CRYSTALS, Items.LAPIS_LAZULI);
		evolve(CritterlingsBIds.Items.CRITTERLING_SACK_ROLLBALL, CritterlingsBIds.Items.CRITTERLING_SACK_ROLLBALL_RARE, Items.WOOL.pick(DyeColor.CYAN), Items.STRING);
		evolve(CritterlingsBIds.Items.CRITTERLING_SACK_ROLLBALL_RARE, CritterlingsBIds.Items.CRITTERLING_SACK_ROLLBALL_EPIC, id(Items.BREAD), BunbugIds.Items.RAW_BUNBUG_MEAT, BunbugIds.Items.BUNBUG_CRUST);
		evolve(CritterlingsBIds.Items.CRITTERLING_SACK_OPALCRAB, CritterlingsBIds.Items.CRITTERLING_SACK_OPALCRAB_RARE, Items.GUNPOWDER, Items.FEATHER);
		evolve(CritterlingsBIds.Items.CRITTERLING_SACK_OPALCRAB_RARE, CritterlingsBIds.Items.CRITTERLING_SACK_OPALCRAB_EPIC, Items.CRYING_OBSIDIAN, Items.GLOW_INK_SAC, Items.AMETHYST_SHARD);
		// critterlings_c
		evolve(CritterlingsCIds.Items.CRITTERLING_SACK_MOTHKID, CritterlingsCIds.Items.CRITTERLING_SACK_MOTHKID_RARE, Items.SPIDER_EYE, Items.BONE_MEAL);
		evolve(CritterlingsCIds.Items.CRITTERLING_SACK_MOTHKID_RARE, CritterlingsCIds.Items.CRITTERLING_SACK_MOTHKID_EPIC, Items.HONEYCOMB, Items.BLAZE_POWDER, Items.PINK_PETALS);
		evolve(CritterlingsCIds.Items.CRITTERLING_SACK_GILLMUNCH, CritterlingsCIds.Items.CRITTERLING_SACK_GILLMUNCH_RARE, Items.SLIME_BALL, Items.LAPIS_LAZULI);
		evolve(CritterlingsCIds.Items.CRITTERLING_SACK_GILLMUNCH_RARE, CritterlingsCIds.Items.CRITTERLING_SACK_GILLMUNCH_EPIC, Items.OBSIDIAN, Items.AMETHYST_SHARD, Items.DRAGON_BREATH);
		evolve(CritterlingsCIds.Items.CRITTERLING_SACK_DOMINIC, CritterlingsCIds.Items.CRITTERLING_SACK_DOMINIC_RARE, Items.ROTTEN_FLESH, Items.LAPIS_LAZULI);
		evolve(CritterlingsCIds.Items.CRITTERLING_SACK_DOMINIC_RARE, CritterlingsCIds.Items.CRITTERLING_SACK_DOMINIC_EPIC, NervoidIds.Blocks.ROTTEN_NERVOID_BRAIN, id(Items.FERMENTED_SPIDER_EYE), id(Items.IRON_INGOT));
		evolve(CritterlingsCIds.Items.CRITTERLING_SACK_OLMER, CritterlingsCIds.Items.CRITTERLING_SACK_OLMER_RARE, Items.SCULK, Items.ECHO_SHARD);
		evolve(CritterlingsCIds.Items.CRITTERLING_SACK_OLMER_RARE, CritterlingsCIds.Items.CRITTERLING_SACK_OLMER_EPIC, Items.DIAMOND, Items.GOLDEN_CARROT, Items.GHAST_TEAR);
		// critterlings_d
		evolve(CritterlingsDIds.Items.CRITTERLING_SACK_STALK, CritterlingsDIds.Items.CRITTERLING_SACK_STALK_RARE, Items.CHICKEN, Items.PACKED_ICE);
		evolve(CritterlingsDIds.Items.CRITTERLING_SACK_STALK_RARE, CritterlingsDIds.Items.CRITTERLING_SACK_STALK_EPIC, Items.LAPIS_LAZULI, Items.DIAMOND, Items.NETHER_WART);
		evolve(CritterlingsDIds.Items.CRITTERLING_SACK_FLARG, CritterlingsDIds.Items.CRITTERLING_SACK_FLARG_RARE, Items.BONE_BLOCK, Items.SOUL_SAND);
		evolve(CritterlingsDIds.Items.CRITTERLING_SACK_FLARG_RARE, CritterlingsDIds.Items.CRITTERLING_SACK_FLARG_EPIC, id(Items.DYED_CANDLE.pick(DyeColor.BLACK)), id(Items.NETHER_WART), ShriekbatIds.Items.SHRIEKBAT_WING);
		evolve(CritterlingsDIds.Items.CRITTERLING_SACK_PIRANHEED, CritterlingsDIds.Items.CRITTERLING_SACK_PIRANHEED_RARE, Items.PUMPKIN, Items.PUMPKIN_PIE);
		evolve(CritterlingsDIds.Items.CRITTERLING_SACK_PIRANHEED_RARE, CritterlingsDIds.Items.CRITTERLING_SACK_PIRANHEED_EPIC, id(Items.SPIDER_EYE), NervoidIds.Items.LOST_NERVE, id(Items.SLIME_BALL));
		evolve(CritterlingsDIds.Items.CRITTERLING_SACK_MANGOTRICE, CritterlingsDIds.Items.CRITTERLING_SACK_MANGOTRICE_RARE, Items.DYE.pick(DyeColor.BLUE), Items.GLOWSTONE_DUST);
		evolve(CritterlingsDIds.Items.CRITTERLING_SACK_MANGOTRICE_RARE, CritterlingsDIds.Items.CRITTERLING_SACK_MANGOTRICE_EPIC, Items.JUNGLE_SAPLING, Items.MANGROVE_PROPAGULE, Items.DEAD_BUSH);
		// critterlings_e
		evolve(CritterlingsEIds.Items.CRITTERLING_SACK_FRESNOID, CritterlingsEIds.Items.CRITTERLING_SACK_FRESNOID_RARE, Items.MELON, Items.SWEET_BERRIES);
		evolve(CritterlingsEIds.Items.CRITTERLING_SACK_FRESNOID_RARE, CritterlingsEIds.Items.CRITTERLING_SACK_FRESNOID_EPIC, Items.AMETHYST_SHARD, Items.SKELETON_SKULL, Items.DYE.pick(DyeColor.BLACK));
		evolve(CritterlingsEIds.Items.CRITTERLING_SACK_COBBLE, CritterlingsEIds.Items.CRITTERLING_SACK_COBBLE_RARE, Items.DEEPSLATE, Items.DIAMOND);
		evolve(CritterlingsEIds.Items.CRITTERLING_SACK_COBBLE_RARE, CritterlingsEIds.Items.CRITTERLING_SACK_COBBLE_EPIC, Items.CAKE, Items.SWEET_BERRIES, Items.SUGAR);
	}

	/** The recipe for the sack in the table's sack slot, or null if it does not evolve. */
	public static @Nullable Recipe forSack(ItemStack sack) {
		return sack.isEmpty() ? null : BY_SACK.get(BuiltInRegistries.ITEM.getKey(sack.getItem()));
	}

	private static void evolve(Identifier sack, Identifier result, Item... ingredients) {
		List<Identifier> ids = new ArrayList<>(ingredients.length);
		for (Item ingredient : ingredients) ids.add(id(ingredient));
		BY_SACK.put(sack, new Recipe(result, List.copyOf(ids)));
	}

	private static void evolve(Identifier sack, Identifier result, Identifier... ingredients) {
		BY_SACK.put(sack, new Recipe(result, List.of(ingredients)));
	}

	private static Identifier id(Item item) {
		return BuiltInRegistries.ITEM.getKey(item);
	}

	private static ItemStack stackOf(Identifier item) {
		return BuiltInRegistries.ITEM.getOptional(item).map(ItemStack::new).orElse(ItemStack.EMPTY);
	}

	private EvolutionRecipes() {}
}
