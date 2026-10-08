package com.morecritters.fabric.client.module.wandering_collector;

import com.morecritters.fabric.client.core.CritterModel;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.wandering_collector.CarrybugRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

/** Renderers for both carrybugs (one shared model; the texture names the colour and saddle) and the chest screen. */
final class CarrybugClient {
	static void register() {
		EntityRendererRegistry.register(CarrybugRegistry.CARRYBUG,
			context -> new CritterRenderer<>(context, new CritterModel<>("carrybug", "carrybug", "carrybug_plains"), 0.7F, 1.0F));
		EntityRendererRegistry.register(CarrybugRegistry.CARRYBUG_NO_SADDLE,
			context -> new CritterRenderer<>(context, new CritterModel<>("carrybug", "carrybug", "carrybug_plains_nosaddle"), 0.7F, 1.0F));
		MenuScreens.register(CarrybugRegistry.MENU, CarrybugScreen::new);
	}

	private CarrybugClient() {}
}
