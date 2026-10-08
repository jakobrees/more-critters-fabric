package com.morecritters.fabric.module.wandering_collector;

import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.ids.WanderingCollectorIds;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;

/** Registers the wandering collector himself: his entity, spawn egg, trade menu and recipe unlocks. */
public final class WanderingCollector {
	public static EntityType<WanderingCollectorEntity> ENTITY;
	public static MenuType<WanderingCollectorMenu> MENU;

	static void register() {
		// The original registers him as a monster; kept so he counts the same way.
		ENTITY = Registration.livingEntity(WanderingCollectorIds.Entities.WANDERING_COLLECTOR,
			EntityType.Builder.of(WanderingCollectorEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(8).updateInterval(3),
			WanderingCollectorEntity.createAttributes());
		Registration.spawnEgg(WanderingCollectorIds.Items.WANDERING_COLLECTOR_SPAWN_EGG, ENTITY);

		MENU = Registry.register(BuiltInRegistries.MENU, WanderingCollectorIds.Menus.WANDERING_TRADER_GUI, WanderingCollectorMenu.createType());
		WanderingCollectorMenu.register();

		RecipeUnlocks.register();
	}

	private WanderingCollector() {}
}
