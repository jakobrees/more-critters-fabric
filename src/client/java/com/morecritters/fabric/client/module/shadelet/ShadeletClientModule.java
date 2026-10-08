package com.morecritters.fabric.client.module.shadelet;

import com.geckolib.renderer.layer.builtin.TextureLayerGeoLayer;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.shadelet.ShadeletEntity;
import com.morecritters.fabric.module.shadelet.ShadeletModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** Rendering for the shadelet (with its full-bright glow layer, like the original) and the chattering teeth. */
public final class ShadeletClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(ShadeletModule.SHADELET, context -> {
			CritterRenderer<ShadeletEntity> renderer = new CritterRenderer<>(context, "shadelet", 0.3F, 1.1F);
			renderer.withRenderLayer(new TextureLayerGeoLayer<>(renderer, MoreCritters.id("textures/entities/shadelet.png"), RenderTypes::eyes));
			return renderer;
		});
		EntityRendererRegistry.register(ShadeletModule.CHATTERING_TEETH, context -> new CritterRenderer<>(context, "chattering_teeth", 0.4F, 1.0F));
	}
}
