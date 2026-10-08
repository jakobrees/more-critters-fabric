package com.morecritters.fabric.client.module.misc;

import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.client.core.SpriteParticle;
import com.morecritters.fabric.client.core.SpriteParticle.Settings;
import com.morecritters.fabric.module.misc.MiscModule;
import com.morecritters.fabric.module.misc.MiscParticles;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/**
 * Rendering for the anniveteran, the worn party hat and the shared particles. Particle values are copied from the
 * original particle classes: size, scale, lifetime, gravity, physics, speed, frames, ticks per frame.
 * A frame count of 1 with 0 ticks is a single still sprite.
 */
public final class MiscClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(MiscModule.ANNIVETERAN, context -> new CritterRenderer<>(context, "anniveteran", 0.5F, 1.2F));
		PartyHatModel.register();

		SpriteParticle.register(MiscParticles.LOCKED_HEART, Settings.of(0.2F, 2.0F, 40, 0.04F, false, 1.0, 1, 0).lifetimeSpread(20));
		SpriteParticle.register(MiscParticles.DROWN_BUBBLE, Settings.of(0.2F, 2.0F, 23, 0.04F, true, 1.0, 12, 2));
		SpriteParticle.register(MiscParticles.BONE_DEBRIS, Settings.of(0.2F, 1.0F, 20, 0.5F, true, 1.0, 1, 0).randomSprite().spin(1.0F, -0.02F));
		SpriteParticle.register(MiscParticles.BITE, Settings.of(0.2F, 5.0F, 6, 0.0F, false, 0.0, 7, 1).fullBright());
		SpriteParticle.register(MiscParticles.ANGEL, Settings.of(0.2F, 1.5F, 30, -0.02F, true, 1.0, 16, 2).translucentSheet());
		SpriteParticle.register(MiscParticles.STUN_STAR, Settings.of(0.2F, 2.0F, 10, -0.2F, true, 0.0, 11, 1).fullBright());
		SpriteParticle.register(MiscParticles.YELLOW_STRIPE, Settings.of(0.2F, 2.0F, 4, -0.2F, true, 1.0, 5, 1).fullBright());
		SpriteParticle.register(MiscParticles.BLACK_STRIPE, Settings.of(0.2F, 2.0F, 4, -0.2F, true, 1.0, 5, 1).fullBright());
		SpriteParticle.register(MiscParticles.XP, Settings.of(0.2F, 1.4F, 1, 0.0F, false, 0.0, 1, 0).translucentSheet());
		SpriteParticle.register(MiscParticles.XPRARE, Settings.of(0.2F, 2.0F, 1, 0.0F, false, 0.0, 1, 0).translucentSheet());
		SpriteParticle.register(MiscParticles.XPEPIC, Settings.of(0.2F, 1.4F, 1, 0.0F, false, 0.0, 1, 0).translucentSheet());
		SpriteParticle.register(MiscParticles.EPIC_PARTICLE, Settings.of(0.2F, 0.2F, 18, -0.01F, true, 1.0, 19, 1).translucentSheet());
		SpriteParticle.register(MiscParticles.DASH, Settings.of(0.2F, 12.0F, 8, 0.0F, true, 1.0, 9, 1));
		SpriteParticle.register(MiscParticles.BLOOD_BUBBLE, Settings.of(0.2F, 1.0F, 12, 0.0F, true, 1.0, 1, 0).lifetimeSpread(4));
		SpriteParticle.register(MiscParticles.BLOOD_BUBBLE_POP, Settings.of(0.2F, 1.0F, 1, 0.0F, true, 1.0, 1, 0));
		SpriteParticle.register(MiscParticles.HEAL_PLUS, Settings.of(0.2F, 2.0F, 5, -0.02F, false, 1.0, 6, 1).translucentSheet());
		SpriteParticle.register(MiscParticles.SPRINKLE, Settings.of(0.2F, 1.5F, 25, 0.42F, true, 1.0, 1, 0).randomSprite().lifetimeSpread(10).spin(2.0F, -0.07F));
		SpriteParticle.register(MiscParticles.WHITE_SPRINKLE, Settings.of(0.2F, 1.5F, 25, 0.42F, true, 1.0, 1, 0).randomSprite().lifetimeSpread(10).spin(2.0F, -0.07F));
		SpriteParticle.register(MiscParticles.REMAINS, Settings.of(0.2F, 1.2F, 15, 0.42F, true, 1.0, 1, 0).randomSprite().lifetimeSpread(10).spin(1.0F, -0.02F));
		SpriteParticle.register(MiscParticles.WARDEN_EXPLOSION, Settings.of(0.2F, 15.0F, 10, 0.0F, true, 1.0, 1, 0).translucentSheet().spin(3.0F, -0.3F));
		SpriteParticle.register(MiscParticles.ALERT, Settings.of(0.2F, 3.0F, 12, -0.2F, false, 1.0, 13, 1).translucentSheet());
		SpriteParticle.register(MiscParticles.ALERTED, Settings.of(0.2F, 3.0F, 12, -0.2F, false, 1.0, 13, 1).translucentSheet());
	}
}
