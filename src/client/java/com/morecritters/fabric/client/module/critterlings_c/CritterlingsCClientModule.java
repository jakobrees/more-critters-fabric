package com.morecritters.fabric.client.module.critterlings_c;

import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.critterlings_c.CritterlingsCModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Rendering for the mothkid, gillmunch, dominic and olmer. */
public final class CritterlingsCClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(CritterlingsCModule.MOTHKID, MothkidRenderer::new);
		EntityRendererRegistry.register(CritterlingsCModule.GILLMUNCH, context -> new CritterRenderer<>(context, "gillmunch", 0.3F, 1.0F));
		EntityRendererRegistry.register(CritterlingsCModule.DOMINIC, context -> new CritterRenderer<>(context, "dominic", 0.2F, 1.0F));
		EntityRendererRegistry.register(CritterlingsCModule.OLMER, context -> new CritterRenderer<>(context, "olmer", 0.2F, 1.0F));
	}
}
