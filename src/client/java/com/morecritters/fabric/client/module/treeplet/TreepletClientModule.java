package com.morecritters.fabric.client.module.treeplet;

import com.geckolib.renderer.layer.builtin.TextureLayerGeoLayer;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.client.core.SpriteParticle;
import com.morecritters.fabric.module.treeplet.TreepletModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.LivingEntity;
import com.geckolib.animatable.GeoAnimatable;

/** Rendering for the treeplet and treeplings (with glowing eyes), the resin puddle, the darts and resin pieces. */
public final class TreepletClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(TreepletModule.TREEPLET, glowingEyes("treeplet", "treeplet_eye"));
		EntityRendererRegistry.register(TreepletModule.TREEPLING_TOP, glowingEyes("treepling_top", "treepling_top_eyes"));
		EntityRendererRegistry.register(TreepletModule.TREEPLING_MIDDLE, glowingEyes("treepling_middle", "treepling_eyes"));
		EntityRendererRegistry.register(TreepletModule.TREEPLING_BOTTOM, glowingEyes("treepling_bottom", "treepling_bottom_eyes"));
		EntityRendererRegistry.register(TreepletModule.RESIN_PUDDLE, context -> new CritterRenderer<>(context, "resin_puddle", 0.0F, 1.0F));
		EntityRendererRegistry.register(TreepletModule.SPLINTER, ThrownItemRenderer::new);
		EntityRendererRegistry.register(TreepletModule.RESIN_PIECE, ThrownItemRenderer::new);
		// Values from the original ResinParticle: 0.2 size doubled, 4 ticks, 0.6 gravity, collides, 5 frames of 2 ticks.
		SpriteParticle.register(TreepletModule.RESIN, SpriteParticle.Settings.of(0.2F, 2.0F, 4, 0.6F, true, 1.0, 5, 2).spin(0.02F, 0.0F));
	}

	private static <T extends LivingEntity & GeoAnimatable> EntityRendererProvider<T> glowingEyes(String name, String eyes) {
		return context -> {
			CritterRenderer<T> renderer = new CritterRenderer<>(context, name, 0.5F, 1.0F);
			renderer.withRenderLayer(new TextureLayerGeoLayer<>(renderer, MoreCritters.id("textures/entities/" + eyes + ".png"), RenderTypes::eyes));
			return renderer;
		};
	}
}
