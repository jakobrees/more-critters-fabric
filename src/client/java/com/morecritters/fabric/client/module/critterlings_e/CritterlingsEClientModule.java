package com.morecritters.fabric.client.module.critterlings_e;

import com.geckolib.renderer.layer.builtin.TextureLayerGeoLayer;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.critterlings_e.CritterlingsEModule;
import com.morecritters.fabric.module.critterlings_e.LightflyEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** Rendering for the fresnoid, the cobble and the glowing lightfly. */
public final class CritterlingsEClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(CritterlingsEModule.FRESNOID, context -> new CritterRenderer<>(context, "fresnoid", 0.2F, 1.0F));
		EntityRendererRegistry.register(CritterlingsEModule.COBBLE, context -> new LookingRenderer<>(context, "cobble", 0.2F, 1.1F));
		EntityRendererRegistry.register(CritterlingsEModule.LIGHTFLY, context -> {
			LookingRenderer<LightflyEntity> renderer = new LookingRenderer<>(context, "lightfly", 0.0F, 0.3F);
			renderer.withRenderLayer(new TextureLayerGeoLayer<>(renderer, MoreCritters.id("textures/entities/lightfly_glow.png"), RenderTypes::eyes));
			return renderer;
		});
	}
}
