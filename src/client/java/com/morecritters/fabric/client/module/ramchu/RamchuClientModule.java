package com.morecritters.fabric.client.module.ramchu;

import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.ramchu.RamchuModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Rendering for the ramchu and its fry. */
public final class RamchuClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(RamchuModule.RAMCHU, context -> new CritterRenderer<>(context, "ramchu", 0.4F, 1.0F));
		EntityRendererRegistry.register(RamchuModule.RAMCHU_FRY, context -> new CritterRenderer<>(context, "ramchu_fry", 0.2F, 1.0F));
	}
}
