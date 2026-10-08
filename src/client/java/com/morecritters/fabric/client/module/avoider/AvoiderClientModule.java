package com.morecritters.fabric.client.module.avoider;

import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.avoider.AvoiderModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Rendering for the avoider and its fry. */
public final class AvoiderClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(AvoiderModule.AVOIDER, context -> new CritterRenderer<>(context, "avoider", 0.5F, 1.0F));
		EntityRendererRegistry.register(AvoiderModule.AVOIDER_FRY, context -> new CritterRenderer<>(context, "avoider_fry", 0.2F, 1.0F));
	}
}
