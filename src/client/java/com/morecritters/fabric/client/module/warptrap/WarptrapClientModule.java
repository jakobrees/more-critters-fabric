package com.morecritters.fabric.client.module.warptrap;

import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.module.warptrap.WarptrapModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Rendering for the warptrap. */
public final class WarptrapClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(WarptrapModule.WARPTRAP, WarptrapRenderer::new);
	}
}
