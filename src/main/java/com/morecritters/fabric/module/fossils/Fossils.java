package com.morecritters.fabric.module.fossils;

import com.morecritters.fabric.ids.CritterlingSystemIds;
import com.morecritters.fabric.ids.FossilsIds;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * The seventeen fossils in the original's order: ten critter fossils, the critterlings
 * module's three critterling fossils, four plant fossils. Fossil {@code n} (counting from 1)
 * is shown by {@code fossil_display_n} and is the {@code n}th of fossil ore's drops.
 */
final class Fossils {
	/** The fossils this module registers. */
	static final List<Identifier> OWN_FOSSILS = List.of(
		FossilsIds.Items.CRITTER_FOSSIL_1, FossilsIds.Items.CRITTER_FOSSIL_2, FossilsIds.Items.CRITTER_FOSSIL_3,
		FossilsIds.Items.CRITTER_FOSSIL_4, FossilsIds.Items.CRITTER_FOSSIL_5, FossilsIds.Items.CRITTER_FOSSIL_6,
		FossilsIds.Items.CRITTER_FOSSIL_7, FossilsIds.Items.CRITTER_FOSSIL_8, FossilsIds.Items.CRITTER_FOSSIL_9,
		FossilsIds.Items.CRITTER_FOSSIL_10,
		FossilsIds.Items.PLANT_FOSSIL_1, FossilsIds.Items.PLANT_FOSSIL_2, FossilsIds.Items.PLANT_FOSSIL_3,
		FossilsIds.Items.PLANT_FOSSIL_4);

	static final List<Identifier> ALL_FOSSILS = List.of(
		FossilsIds.Items.CRITTER_FOSSIL_1, FossilsIds.Items.CRITTER_FOSSIL_2, FossilsIds.Items.CRITTER_FOSSIL_3,
		FossilsIds.Items.CRITTER_FOSSIL_4, FossilsIds.Items.CRITTER_FOSSIL_5, FossilsIds.Items.CRITTER_FOSSIL_6,
		FossilsIds.Items.CRITTER_FOSSIL_7, FossilsIds.Items.CRITTER_FOSSIL_8, FossilsIds.Items.CRITTER_FOSSIL_9,
		FossilsIds.Items.CRITTER_FOSSIL_10,
		CritterlingSystemIds.Items.CRITTERLING_FOSSIL_1, CritterlingSystemIds.Items.CRITTERLING_FOSSIL_2,
		CritterlingSystemIds.Items.CRITTERLING_FOSSIL_3,
		FossilsIds.Items.PLANT_FOSSIL_1, FossilsIds.Items.PLANT_FOSSIL_2, FossilsIds.Items.PLANT_FOSSIL_3,
		FossilsIds.Items.PLANT_FOSSIL_4);

	static final List<Identifier> DISPLAYS = List.of(
		FossilsIds.Blocks.FOSSIL_DISPLAY_1, FossilsIds.Blocks.FOSSIL_DISPLAY_2, FossilsIds.Blocks.FOSSIL_DISPLAY_3,
		FossilsIds.Blocks.FOSSIL_DISPLAY_4, FossilsIds.Blocks.FOSSIL_DISPLAY_5, FossilsIds.Blocks.FOSSIL_DISPLAY_6,
		FossilsIds.Blocks.FOSSIL_DISPLAY_7, FossilsIds.Blocks.FOSSIL_DISPLAY_8, FossilsIds.Blocks.FOSSIL_DISPLAY_9,
		FossilsIds.Blocks.FOSSIL_DISPLAY_10, FossilsIds.Blocks.FOSSIL_DISPLAY_11, FossilsIds.Blocks.FOSSIL_DISPLAY_12,
		FossilsIds.Blocks.FOSSIL_DISPLAY_13, FossilsIds.Blocks.FOSSIL_DISPLAY_14, FossilsIds.Blocks.FOSSIL_DISPLAY_15,
		FossilsIds.Blocks.FOSSIL_DISPLAY_16, FossilsIds.Blocks.FOSSIL_DISPLAY_17);

	static Item item(Identifier id) {
		return BuiltInRegistries.ITEM.getValue(id);
	}

	/** The display that shows the held fossil, or null when the stack is not one of the seventeen. */
	static @Nullable Block displayFor(ItemStack stack) {
		for (int i = 0; i < ALL_FOSSILS.size(); i++) {
			Identifier fossil = ALL_FOSSILS.get(i);
			if (BuiltInRegistries.ITEM.containsKey(fossil) && stack.is(item(fossil))) {
				return BuiltInRegistries.BLOCK.getValue(DISPLAYS.get(i));
			}
		}
		return null;
	}

	private Fossils() {}
}
