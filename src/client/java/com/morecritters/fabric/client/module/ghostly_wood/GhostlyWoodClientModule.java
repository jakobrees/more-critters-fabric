package com.morecritters.fabric.client.module.ghostly_wood;

import com.morecritters.fabric.client.ClientModule;

/**
 * Rendering for the ghostly_wood module. Nothing to register: the module has no entities,
 * particles or screens, and 26.3 picks each block's cutout layer from its textures.
 */
public final class GhostlyWoodClientModule implements ClientModule {
	@Override
	public void registerClient() {
	}
}
