package com.morecritters.fabric.client.module.bunbug;

import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.bunbug.BunbugModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Rendering for the bunbug and its grub. */
public final class BunbugClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(BunbugModule.BUNBUG, context -> new CritterRenderer<>(context, "bunbug", 0.4F, 1.2F));
		EntityRendererRegistry.register(BunbugModule.BABY_BUNBUG, context -> new CritterRenderer<>(context, "bunbug_baby", 0.2F, 1.0F));
	}
}
