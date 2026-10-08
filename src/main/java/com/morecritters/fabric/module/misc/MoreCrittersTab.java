package com.morecritters.fabric.module.misc;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.ids.MiscIds;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * The "More Critters" creative tab: the items of {@link MoreCrittersTabContents}, looked up when the
 * tab is displayed so that every module's items appear without this module depending on them.
 */
final class MoreCrittersTab {
	static void register() {
		CreativeModeTab tab = FabricCreativeModeTab.builder()
			.title(Component.translatable("item_group.more_critters.more_critters"))
			.icon(() -> new ItemStack(BuiltInRegistries.ITEM.getValue(MiscIds.Items.TAB_ICON)))
			.displayItems((parameters, output) -> {
				for (String name : MoreCrittersTabContents.ITEMS) {
					BuiltInRegistries.ITEM.getOptional(MoreCritters.id(name)).ifPresent(output::accept);
				}
			})
			.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MiscIds.Tabs.MORE_CRITTERS, tab);
	}

	private MoreCrittersTab() {}
}
