package com.morecritters.fabric.client.module.snowflake_spider;

import com.geckolib.renderer.layer.builtin.TextureLayerGeoLayer;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.client.core.SpriteParticle;
import com.morecritters.fabric.module.snowflake_spider.SnowflakeSpiderEntity;
import com.morecritters.fabric.module.snowflake_spider.SnowflakeSpiderModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** Rendering for the snowflake spider (with its glowing markings), its webs, its web sacks and the brittle heart. */
public final class SnowflakeSpiderClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(SnowflakeSpiderModule.SNOWFLAKE_SPIDER, context -> {
			CritterRenderer<SnowflakeSpiderEntity> renderer = new CritterRenderer<>(context, "snowflake_spider", 0.5F, 1.0F);
			renderer.withRenderLayer(new TextureLayerGeoLayer<>(renderer, MoreCritters.id("textures/entities/snowflake_spider_glow.png"), RenderTypes::eyes));
			return renderer;
		});
		EntityRendererRegistry.register(SnowflakeSpiderModule.WEB_ENTITY, WebEntityRenderer::new);
		EntityRendererRegistry.register(SnowflakeSpiderModule.WEB_SACK_PROJECTILE, ThrownItemRenderer::new);
		// Values from the original BrittleHeartParticle: 0.2 size doubled, ~40 ticks, light gravity, no collision.
		SpriteParticle.register(SnowflakeSpiderModule.BRITTLE_HEART, SpriteParticle.Settings.of(0.2F, 2.0F, 40, 0.04F, false, 1.0, 1, 0).lifetimeSpread(20));
	}
}
