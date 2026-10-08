package com.morecritters.fabric.client.module.armossillo;

import com.morecritters.fabric.client.ClientModule;

/** Rendering for the armossillo module. */
public final class ArmossilloClientModule implements ClientModule {
	@Override
	public void registerClient() {
		ArmossilloBlocksClient.register();
		ArmossilloItemsClient.register();
		ArmossilloEntitiesClient.register();
	}
}
