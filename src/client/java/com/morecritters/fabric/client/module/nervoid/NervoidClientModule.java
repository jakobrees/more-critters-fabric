package com.morecritters.fabric.client.module.nervoid;

import com.geckolib.renderer.layer.builtin.TextureLayerGeoLayer;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.client.core.SpriteParticle;
import com.morecritters.fabric.module.nervoid.NervoidEntity;
import com.morecritters.fabric.module.nervoid.NervoidModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** Rendering for the nervoid (with glowing eyes) and its five particles. */
public final class NervoidClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(NervoidModule.NERVOID, context -> {
			CritterRenderer<NervoidEntity> renderer = new CritterRenderer<>(context, "nervoid", 0.5F, 1.0F);
			renderer.withRenderLayer(new TextureLayerGeoLayer<>(renderer, MoreCritters.id("textures/entities/nervoid_eyes.png"), RenderTypes::eyes));
			return renderer;
		});
		// Sixteen frames, one tick each; lit in the original.
		SpriteParticle.register(NervoidModule.END_EXPLOSION, SpriteParticle.Settings.of(0.2F, 6.0F, 15, 0.0F, true, 1.0, 16, 1).fullBright());
		SpriteParticle.register(NervoidModule.SPINAL_FLUID, SpriteParticle.Settings.of(0.2F, 0.3F, 40, 0.3F, true, 1.0, 1, 0));
		SpriteParticle.register(NervoidModule.STINK, SpriteParticle.Settings.of(0.2F, 2.0F, 20, -0.05F, true, 1.0, 8, 2).translucentSheet());
		SpriteParticle.register(NervoidModule.ICE_ON, SpriteParticle.Settings.of(0.2F, 1.0F, 20, 0.0F, true, 1.0, 1, 0));
		SpriteParticle.register(NervoidModule.ICE_OFF, SpriteParticle.Settings.of(0.2F, 1.0F, 20, 0.0F, true, 1.0, 1, 0));
	}
}
