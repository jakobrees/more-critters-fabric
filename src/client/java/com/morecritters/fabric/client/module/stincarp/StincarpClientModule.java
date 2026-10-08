package com.morecritters.fabric.client.module.stincarp;

import com.geckolib.renderer.layer.builtin.TextureLayerGeoLayer;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterModel;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.stincarp.StincarpEntity;
import com.morecritters.fabric.module.stincarp.StincarpModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** Rendering for the stincarp: the fish plus its glowing stripes, drawn like eyes (full bright). */
public final class StincarpClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(StincarpModule.STINCARP, context -> {
			// The original draws the stincarp with its "stingcarp" model; "stincarp.geo.json" is an older model it ships unused.
			CritterRenderer<StincarpEntity> renderer = new CritterRenderer<>(context, new CritterModel<>("stingcarp", "stingcarp", "stincarp"), 0.5F, 1.2F);
			renderer.withRenderLayer(new TextureLayerGeoLayer<>(renderer, MoreCritters.id("textures/entities/stincarp_glow.png"), RenderTypes::eyes));
			return renderer;
		});
	}
}
