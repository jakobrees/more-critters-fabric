package com.morecritters.fabric.client.module.critterlings_b;

import com.geckolib.renderer.layer.builtin.TextureLayerGeoLayer;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.critterlings_b.CritterlingsBModule;
import com.morecritters.fabric.module.critterlings_b.ExpyEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** Rendering for the expy (with its glow), scowl, rollball and opalcrab (1.2x). */
public final class CritterlingsBClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(CritterlingsBModule.EXPY, context -> {
			CritterRenderer<ExpyEntity> renderer = new CritterRenderer<>(context, "expy", 0.2F, 1.0F);
			renderer.withRenderLayer(new TextureLayerGeoLayer<>(renderer, MoreCritters.id("textures/entities/expy_glow.png"), RenderTypes::eyes));
			return renderer;
		});
		EntityRendererRegistry.register(CritterlingsBModule.SCOWL, context -> new CritterRenderer<>(context, "scowl", 0.2F, 1.0F));
		EntityRendererRegistry.register(CritterlingsBModule.ROLLBALL, context -> new CritterRenderer<>(context, "rollball", 0.2F, 1.0F));
		EntityRendererRegistry.register(CritterlingsBModule.OPALCRAB, context -> new CritterRenderer<>(context, "opalcrab", 0.2F, 1.2F));
	}
}
