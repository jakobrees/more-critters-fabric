package com.morecritters.fabric.client.module.bouncelizard;

import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.module.bouncelizard.BouncelizardModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Rendering for the bouncelizard. */
public final class BouncelizardClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(BouncelizardModule.BOUNCELIZARD, BouncelizardRenderer::new);
	}
}
