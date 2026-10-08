package com.morecritters.fabric.client.module.wandering_collector;

import com.morecritters.fabric.module.wandering_collector.WanderingCollector;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

/** Client registrations for the wandering collector: his renderer and trade screen. */
final class WanderingCollectorClient {
	static void register() {
		EntityRendererRegistry.register(WanderingCollector.ENTITY, WanderingCollectorRenderer::new);
		MenuScreens.register(WanderingCollector.MENU, WanderingCollectorScreen::new);
	}

	private WanderingCollectorClient() {}
}
