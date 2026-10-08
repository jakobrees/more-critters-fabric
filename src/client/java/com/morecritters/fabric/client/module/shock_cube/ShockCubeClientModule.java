package com.morecritters.fabric.client.module.shock_cube;

import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.client.core.GeoItemRenderers;
import com.morecritters.fabric.client.core.SpriteParticle;
import com.morecritters.fabric.module.shock_cube.ShockCubeModule;
import com.morecritters.fabric.module.shock_cube.TaserItem;
import com.morecritters.fabric.module.shock_cube.TazegunItem;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

/** Shock cube renderers, the thunderball, the taser and tazegun models, and the zap particles. */
public final class ShockCubeClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(ShockCubeModule.SHOCK_CUBE, context -> new CritterRenderer<>(context, "shock_cube", 0.0F, 2.0F));
		EntityRendererRegistry.register(ShockCubeModule.SHOCK_CUBE_SMALL, context -> new CritterRenderer<>(context, "shock_cube", 0.0F, 1.0F));
		ModelLayerRegistry.registerModelLayer(ThunderballRenderer.LAYER, ThunderballRenderer.Model::createBodyLayer);
		EntityRendererRegistry.register(ShockCubeModule.THUNDERBALL, ThunderballRenderer::new);

		GeoItemRenderers.register((TaserItem) ShockCubeModule.TASER, "taser", "item/taser");
		GeoItemRenderers.register((TazegunItem) ShockCubeModule.TAZEGUN, "tazegun", "item/tazegun");

		SpriteParticle.register(ShockCubeModule.ZAP, SpriteParticle.Settings.of(0.2F, 2.0F, 10, 0.0F, false, 1.0, 11, 1).fullBright());
		SpriteParticle.register(ShockCubeModule.ZAP_SPARK, SpriteParticle.Settings.of(0.2F, 1.0F, 6, 0.04F, true, 1.0, 4, 2)
			.fullBright().spin(0.6F, -0.02F));
	}
}
