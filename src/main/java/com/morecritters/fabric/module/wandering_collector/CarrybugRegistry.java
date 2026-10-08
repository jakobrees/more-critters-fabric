package com.morecritters.fabric.module.wandering_collector;

import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.ids.WanderingCollectorIds;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/** The carrybug half of the module: both carrybug entities, their spawn egg and the chest menu. */
public final class CarrybugRegistry {
	public static EntityType<CarrybugEntity> CARRYBUG;
	public static EntityType<CarrybugNoSaddleEntity> CARRYBUG_NO_SADDLE;
	public static MenuType<CarrybugMenu> MENU;

	static void register() {
		// Registered as monsters like the original; they never despawn either way.
		CARRYBUG = Registration.livingEntity(WanderingCollectorIds.Entities.CARRYBUG,
			EntityType.Builder.of(CarrybugEntity::new, MobCategory.MONSTER).sized(2.0F, 3.0F).clientTrackingRange(8).updateInterval(3),
			CarrybugBaseEntity.createAttributes());
		CARRYBUG_NO_SADDLE = Registration.livingEntity(WanderingCollectorIds.Entities.CARRYBUG_NO_SADDLE,
			EntityType.Builder.of(CarrybugNoSaddleEntity::new, MobCategory.MONSTER).sized(2.0F, 3.0F).clientTrackingRange(8).updateInterval(3),
			CarrybugBaseEntity.createAttributes());

		Registration.spawnEgg(WanderingCollectorIds.Items.CARRYBUG_SPAWN_EGG, CARRYBUG);

		MENU = Registry.register(BuiltInRegistries.MENU, WanderingCollectorIds.Menus.CARRYBUG_GUI,
			new MenuType<>(CarrybugMenu::new, FeatureFlags.VANILLA_SET));
	}

	private CarrybugRegistry() {}
}
