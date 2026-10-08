package com.morecritters.fabric.client.module.critterlings_a;

import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.critterlings_a.CritterlingsAModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Renderers for the cubefrog, plainswyrm (1.2x), dunger and snek (head follows its gaze). */
public final class CritterlingsAClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(CritterlingsAModule.CUBEFROG, context -> new CritterRenderer<>(context, "cubefrog", 0.2F, 1.0F));
		EntityRendererRegistry.register(CritterlingsAModule.PLAINSWYRM, context -> new CritterRenderer<>(context, "plainswyrm", 0.2F, 1.2F));
		EntityRendererRegistry.register(CritterlingsAModule.DUNGER, context -> new CritterRenderer<>(context, "dunger", 0.2F, 1.0F));
		EntityRendererRegistry.register(CritterlingsAModule.SNEK, SnekRenderer::new);
	}
}
