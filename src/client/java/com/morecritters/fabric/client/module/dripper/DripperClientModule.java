package com.morecritters.fabric.client.module.dripper;

import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.dripper.DripperModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Rendering for the dripper and the Slashklub's slash. */
public final class DripperClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(DripperModule.DRIPPER, context -> new CritterRenderer<>(context, "dripper", 0.5F, 1.0F));
		EntityRendererRegistry.register(DripperModule.SLASH_EFFECT, SlashEffectRenderer::new);
	}
}
