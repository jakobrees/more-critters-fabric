package com.morecritters.fabric.core;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/**
 * The original mod's tooltips live in the language file as {@code item.more_critters.<name>.description_N}.
 * They are attached as the item's default lore component, which is what vanilla reads tooltips from now.
 */
public final class Tooltips {
	/** Adds {@code count} description lines (numbered from 0) to the item's properties. */
	public static Item.Properties describe(Item.Properties properties, String itemName, int count) {
		List<Component> lines = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			// Explicit style so the lore's default purple italics do not apply; the strings carry their own colours.
			lines.add(Component.translatable("item.more_critters." + itemName + ".description_" + i)
				.withStyle(style -> style.withItalic(false).withColor(ChatFormatting.WHITE)));
		}
		return properties.component(DataComponents.LORE, new ItemLore(lines));
	}

	private Tooltips() {}
}
