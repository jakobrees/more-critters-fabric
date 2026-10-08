package com.morecritters.fabric.module.misc;

import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The original's loot modifier: pillager outpost and woodland mansion chests also roll
 * {@code minecraft:chests/pillager_outpost_addition} (the mod's music discs and trinkets).
 */
final class OutpostLoot {
	private static final ResourceKey<LootTable> ADDITION =
		ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/pillager_outpost_addition"));
	private static final Set<ResourceKey<LootTable>> TARGETS = Set.of(
		ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/pillager_outpost")),
		ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/woodland_mansion")));

	static void register() {
		LootTableEvents.MODIFY_DROPS.register(OutpostLoot::addOutpostLoot);
	}

	/** Rolls the addition table into the chest's loot, with the chest's own context, like the original modifier. */
	private static void addOutpostLoot(Holder<LootTable> table, LootContext context, List<ItemStack> drops) {
		if (!table.unwrapKey().map(TARGETS::contains).orElse(false)) return;
		LootTable addition = context.getLevel().getServer().reloadableRegistries().getLootTable(ADDITION);
		addition.getRandomItemsRaw(context, drops::add);
	}

	private OutpostLoot() {}
}
