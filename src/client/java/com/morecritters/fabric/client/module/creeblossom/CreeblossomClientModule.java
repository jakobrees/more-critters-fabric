package com.morecritters.fabric.client.module.creeblossom;

import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.client.core.SpriteParticle;
import com.morecritters.fabric.module.creeblossom.CreeblossomModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Rendering for the creeblossom and its petal, burst and leaf particles. */
public final class CreeblossomClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(CreeblossomModule.CREEBLOSSOM, context -> new CritterRenderer<>(context, "creeblossom", 0.4F, 1.0F));
		// Petals: five frames, one tick each, rising over a four-tick life.
		SpriteParticle.register(CreeblossomModule.BLOSSOM_PARTICLE, SpriteParticle.Settings.of(0.2F, 2.0F, 4, -0.02F, true, 1.0, 5, 1));
		// The burst: sixteen frames, one tick each, translucent.
		SpriteParticle.register(CreeblossomModule.BLOSSOM_EXPLOSION, SpriteParticle.Settings.of(0.2F, 4.0F, 15, 0.0F, true, 1.0, 16, 1).translucentSheet());
		// Leaves drift down where they are spawned; the original ignores the spawn velocity.
		SpriteParticle.register(CreeblossomModule.RECRUITED_LEAF, SpriteParticle.Settings.of(0.2F, 1.3F, 40, 0.02F, true, 0.0, 1, 0)
			.lifetimeSpread(20).spin(0.4F, -0.01F));
	}
}
