package com.morecritters.fabric.client.module.armossillo;

/** Client side of the armossillo module's blocks. */
final class ArmossilloBlocksClient {
	/**
	 * Nothing to register: Fabric API for 26.x has no block render-layer map, and the game picks
	 * cutout or translucent from the textures' alpha (the models' NeoForge {@code render_type} is ignored).
	 */
	static void register() {
	}

	private ArmossilloBlocksClient() {}
}
