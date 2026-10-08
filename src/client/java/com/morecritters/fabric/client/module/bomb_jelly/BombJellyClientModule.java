package com.morecritters.fabric.client.module.bomb_jelly;

import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.bomb_jelly.BombJellyModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;

/** Rendering for the bomb_jelly module. */
public final class BombJellyClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(BombJellyModule.BOMB_JELLY_SMALL, context -> new CritterRenderer<>(context, "bomb_jelly", 0.5F, 1.0F));
		EntityRendererRegistry.register(BombJellyModule.BOMB_JELLY_MEDIUM, context -> new CritterRenderer<>(context, "bomb_jelly_medium", 0.5F, 1.0F));
		EntityRendererRegistry.register(BombJellyModule.BOMB_JELLY_LARGE, context -> new CritterRenderer<>(context, "bomb_jelly_large", 0.5F, 1.0F));
		// The unsized jelly becomes a sized one on its first server tick; it is never seen.
		EntityRendererRegistry.register(BombJellyModule.BOMB_JELLY, NoopRenderer::new);
		EntityRendererRegistry.register(BombJellyModule.JELLY_TORPEDO, context -> new CritterRenderer<>(context, "jelly_torpedo", 0.5F, 1.0F));
	}
}
