package com.morecritters.fabric.client.module.shimmerwing;

import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.client.core.SpriteParticle;
import com.morecritters.fabric.module.shimmerwing.ShimmerwingModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Rendering for the shimmerworm, the shimmerwing and their shimmer particles. */
public final class ShimmerwingClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(ShimmerwingModule.SHIMMERWORM, context -> new CritterRenderer<>(context, "shimmerworm", 0.4F, 1.0F));
		EntityRendererRegistry.register(ShimmerwingModule.SHIMMERWING, context -> new CritterRenderer<>(context, "shimmerwing", 0.5F, 1.0F));
		// Four frames, two ticks each, over a six-tick life.
		SpriteParticle.register(ShimmerwingModule.SHIMMER, SpriteParticle.Settings.of(0.2F, 2.0F, 6, 0.04F, true, 1.0, 4, 2)
			.fullBright().spin(0.2F, -0.02F));
		SpriteParticle.register(ShimmerwingModule.HEAL_SHIMMER, SpriteParticle.Settings.of(0.2F, 1.0F, 6, 0.04F, true, 1.0, 4, 2)
			.fullBright().spin(0.6F, -0.02F));
	}
}
