package com.morecritters.fabric.client.module.blubberfish;

import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.blubberfish.BlubberfishModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Rendering for the blubberfish and its fry. */
public final class BlubberfishClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(BlubberfishModule.BLUBBERFISH, context -> new CritterRenderer<>(context, "blubberfish", 0.4F, 1.0F));
		EntityRendererRegistry.register(BlubberfishModule.BLUBBERFISH_FRY, context -> new CritterRenderer<>(context, "blubberfish_fry", 0.2F, 1.0F));
	}
}
