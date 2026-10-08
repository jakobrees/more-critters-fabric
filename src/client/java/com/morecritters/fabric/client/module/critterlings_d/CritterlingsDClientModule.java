package com.morecritters.fabric.client.module.critterlings_d;

import com.geckolib.renderer.layer.builtin.TextureLayerGeoLayer;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.critterlings_d.CritterlingsDModule;
import com.morecritters.fabric.module.critterlings_d.StalkEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** Renderers for the flarg, stalk (with its glow layer), piranheed and mangotrice, and the flarg flame particle. */
public final class CritterlingsDClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(CritterlingsDModule.FLARG, context -> new CritterRenderer<>(context, "flarg", 0.2F, 1.0F));
		EntityRendererRegistry.register(CritterlingsDModule.STALK, context -> {
			CritterRenderer<StalkEntity> renderer = new CritterRenderer<>(context, "stalk", 0.2F, 1.1F);
			renderer.withRenderLayer(new TextureLayerGeoLayer<>(renderer, MoreCritters.id("textures/entities/stalk_glow.png"), RenderTypes::eyes));
			return renderer;
		});
		EntityRendererRegistry.register(CritterlingsDModule.PIRANHEED, context -> new CritterRenderer<>(context, "piranheed", 0.2F, 1.0F));
		EntityRendererRegistry.register(CritterlingsDModule.MANGOTRICE, context -> new CritterRenderer<>(context, "mangotrice", 0.2F, 1.1F));
		FlargFlameParticle.register(CritterlingsDModule.FLARG_FLAME);
	}
}
