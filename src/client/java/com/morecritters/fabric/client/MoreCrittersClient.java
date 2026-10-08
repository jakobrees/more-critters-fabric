package com.morecritters.fabric.client;

import net.fabricmc.api.ClientModInitializer;

public final class MoreCrittersClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientModules.registerAll();
	}
}
