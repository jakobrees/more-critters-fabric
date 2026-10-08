package com.morecritters.fabric.client.module.kelpire;

import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.kelpire.KelpireModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Rendering for the kelpire. */
public final class KelpireClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(KelpireModule.KELPIRE, context -> new CritterRenderer<>(context, "kelpire", 0.5F, 1.2F));
	}
}
