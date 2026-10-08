package com.morecritters.fabric.client.module.custodian;

import com.geckolib.renderer.layer.builtin.TextureLayerGeoLayer;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.client.core.SpriteParticle;
import com.morecritters.fabric.module.custodian.CustodianModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Rendering for both custodians (with their glowing eye) and the laser particle. */
public final class CustodianClientModule implements ClientModule {
	private static final Identifier GLOW = MoreCritters.id("textures/entities/custodian_glow.png");

	@Override
	public void registerClient() {
		EntityRendererRegistry.register(CustodianModule.CUSTODIAN, context -> {
			var renderer = new CritterRenderer<>(context, "custodian", 0.6F, 1.2F);
			return renderer.withRenderLayer(new TextureLayerGeoLayer<>(renderer, GLOW, RenderTypes::eyes));
		});
		EntityRendererRegistry.register(CustodianModule.ANCIENT_CUSTODIAN, context -> {
			var renderer = new CritterRenderer<>(context, "ancient_custodian", 0.6F, 1.2F);
			return renderer.withRenderLayer(new TextureLayerGeoLayer<>(renderer, GLOW, RenderTypes::eyes));
		});

		// Eight frames, one tick each, full bright, hanging in place.
		SpriteParticle.register(CustodianModule.LASER_PARTICLE, SpriteParticle.Settings.of(0.2F, 2.0F, 7, 0.0F, false, 0.0, 8, 1).fullBright());
	}
}
