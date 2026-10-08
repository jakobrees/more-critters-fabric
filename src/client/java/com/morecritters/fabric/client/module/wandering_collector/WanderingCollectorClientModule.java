package com.morecritters.fabric.client.module.wandering_collector;

import com.morecritters.fabric.client.ClientModule;

/** Renderers and screens for the wandering collector and his carrybugs. */
public final class WanderingCollectorClientModule implements ClientModule {
	@Override
	public void registerClient() {
		WanderingCollectorClient.register();

		CarrybugClient.register();
	}
}
