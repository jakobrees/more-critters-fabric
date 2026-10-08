package com.morecritters.fabric.client.module.gravedigger;

import com.geckolib.renderer.GeoBlockRenderer;
import com.morecritters.fabric.client.ClientModule;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.client.core.GeoItemRenderers;
import com.morecritters.fabric.module.gravedigger.GravediggerJarItem;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import com.morecritters.fabric.client.core.SpriteParticle;
import com.morecritters.fabric.module.gravedigger.GravediggerModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

/** Rendering for the gravedigger, the Amalgam, the gravedigger jar and the reaper particles. */
public final class GravediggerClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(GravediggerModule.GRAVEDIGGER, context -> new CritterRenderer<>(context, "gravedigger", 0.5F, 1.0F));
		EntityRendererRegistry.register(GravediggerModule.AMALGAM, AmalgamRenderer::new);
		// The jar is glass: drawn translucent, in the world and in hand.
		BlockEntityRenderers.register(GravediggerModule.JAR_BLOCK_ENTITY, context -> new GeoBlockRenderer<>(context, new GravediggerJarModel()) {
			@Override
			public RenderType getRenderType(BlockEntityRenderState renderState, Identifier texture) {
				return RenderTypes.entityTranslucent(texture);
			}
		});
		GeoItemRenderers.register((GravediggerJarItem) GravediggerModule.JAR_ITEM, "gravedigger_jar", "block/gravedigger_jar");

		// 16 frames, two ticks each, rising gently; the mad reaper is half the size.
		SpriteParticle.register(GravediggerModule.REAPER_PARTICLE,
			SpriteParticle.Settings.of(0.2F, 3.0F, 30, -0.02F, true, 1.0, 16, 2).translucentSheet());
		SpriteParticle.register(GravediggerModule.MAD_REAPER_PARTICLE,
			SpriteParticle.Settings.of(0.2F, 1.5F, 30, -0.02F, true, 1.0, 16, 2).translucentSheet());
	}
}
