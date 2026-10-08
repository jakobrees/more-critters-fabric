package com.morecritters.fabric.client.module.shriekbat;

import com.geckolib.renderer.layer.builtin.TextureLayerGeoLayer;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterModel;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.client.core.SpriteParticle;
import com.morecritters.fabric.module.shriekbat.EchoEntity;
import com.morecritters.fabric.module.shriekbat.ShriekbatEntity;
import com.morecritters.fabric.module.shriekbat.ShriekbatModule;
import com.morecritters.fabric.module.shriekbat.TesterShriekEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** Rendering for the shriekbat (with its glowing eyes), the echoes, the thrown bomb and the shriek particle. */
public final class ShriekbatClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(ShriekbatModule.SHRIEKBAT, context -> {
			CritterRenderer<ShriekbatEntity> renderer = new CritterRenderer<>(context, "shriekbat", 0.4F, 1.0F);
			renderer.withRenderLayer(new TextureLayerGeoLayer<>(renderer, MoreCritters.id("textures/entities/shriekbat_glow.png"), RenderTypes::eyes));
			return renderer;
		});
		EntityRendererRegistry.register(ShriekbatModule.ECHO, context -> echoRenderer(context, 1.0F));
		EntityRendererRegistry.register(ShriekbatModule.LARGE_ECHO, context -> echoRenderer(context, 2.0F));
		EntityRendererRegistry.register(ShriekbatModule.TESTER_SHRIEK, context -> {
			CritterRenderer<TesterShriekEntity> renderer = new CritterRenderer<>(context, new CritterModel<>("tester_shriek"), 0.0F, 2.0F);
			renderer.withRenderLayer(new TextureLayerGeoLayer<>(renderer, MoreCritters.id("textures/entities/tester_shriek.png"), RenderTypes::eyes));
			return renderer;
		});
		EntityRendererRegistry.register(ShriekbatModule.SHRIEKBOMB_PROJECTILE, ThrownItemRenderer::new);
		// Eleven frames, one tick each, over a ten-tick life, seven times the usual size.
		SpriteParticle.register(ShriekbatModule.SHRIEK_PARTICLE, SpriteParticle.Settings.of(0.2F, 7.0F, 10, 0.0F, true, 1.0, 11, 1)
			.fullBright().spin(0.2F, 0.0F));
	}

	/** Echoes use the tester shriek's model and glow layer. */
	private static CritterRenderer<EchoEntity> echoRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context, float scale) {
		CritterRenderer<EchoEntity> renderer = new CritterRenderer<>(context, new CritterModel<>("tester_shriek"), 0.0F, scale);
		renderer.withRenderLayer(new TextureLayerGeoLayer<>(renderer, MoreCritters.id("textures/entities/tester_shriek.png"), RenderTypes::eyes));
		return renderer;
	}
}
