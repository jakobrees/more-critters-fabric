package com.morecritters.fabric.client.module.iropod;

import com.geckolib.renderer.layer.builtin.TextureLayerGeoLayer;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterModel;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.iropod.IropodEntity;
import com.morecritters.fabric.module.iropod.IropodModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** Rendering for both iropods (with the original's full-bright glow layer), the iroball and the worn iropod helmet. */
public final class IropodClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(IropodModule.IROPOD, context -> glowing(context, new CritterModel<>("iropod")));
		EntityRendererRegistry.register(IropodModule.BLACK_IROPOD, context -> glowing(context, new CritterModel<>("iropod_black")));
		EntityRendererRegistry.register(IropodModule.IROBALL, context -> new CritterRenderer<>(context, "iroball", 0.5F, 1.0F));
		IropodHelmetModel.register();
	}

	private static <T extends IropodEntity> CritterRenderer<T> glowing(EntityRendererProvider.Context context, CritterModel<T> model) {
		CritterRenderer<T> renderer = new CritterRenderer<>(context, model, 0.5F, 1.0F);
		renderer.withRenderLayer(new TextureLayerGeoLayer<>(renderer, MoreCritters.id("textures/entities/iropod_glow.png"), RenderTypes::eyes));
		return renderer;
	}
}
