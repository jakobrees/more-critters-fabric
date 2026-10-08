package com.morecritters.fabric.module.misc;

import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.ids.MiscIds;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * The shared particles many critters spawn by id. The second argument is the original's
 * "always show" flag (drawn even with particles set to minimal).
 */
public final class MiscParticles {
	public static SimpleParticleType LOCKED_HEART, DROWN_BUBBLE, BONE_DEBRIS, BITE, ANGEL, STUN_STAR;
	public static SimpleParticleType YELLOW_STRIPE, BLACK_STRIPE, XP, XPRARE, XPEPIC, EPIC_PARTICLE, DASH;
	public static SimpleParticleType BLOOD_BUBBLE, BLOOD_BUBBLE_POP, HEAL_PLUS, SPRINKLE, WHITE_SPRINKLE, REMAINS;
	public static SimpleParticleType WARDEN_EXPLOSION, ALERT, ALERTED;

	static void register() {
		LOCKED_HEART = Particles.simple(MiscIds.Particles.LOCKED_HEART, false);
		DROWN_BUBBLE = Particles.simple(MiscIds.Particles.DROWN_BUBBLE, false);
		BONE_DEBRIS = Particles.simple(MiscIds.Particles.BONE_DEBRIS, false);
		BITE = Particles.simple(MiscIds.Particles.BITE, true);
		ANGEL = Particles.simple(MiscIds.Particles.ANGEL, false);
		STUN_STAR = Particles.simple(MiscIds.Particles.STUN_STAR, false);
		YELLOW_STRIPE = Particles.simple(MiscIds.Particles.YELLOW_STRIPE, false);
		BLACK_STRIPE = Particles.simple(MiscIds.Particles.BLACK_STRIPE, false);
		XP = Particles.simple(MiscIds.Particles.XP, true);
		DASH = Particles.simple(MiscIds.Particles.DASH, false);
		BLOOD_BUBBLE = Particles.simple(MiscIds.Particles.BLOOD_BUBBLE_PARTICLE, true);
		BLOOD_BUBBLE_POP = Particles.simple(MiscIds.Particles.BLOOD_BUBBLE_POP, true);
		HEAL_PLUS = Particles.simple(MiscIds.Particles.HEAL_PLUS, false);
		SPRINKLE = Particles.simple(MiscIds.Particles.SPRINKLE, false);
		WHITE_SPRINKLE = Particles.simple(MiscIds.Particles.WHITE_SPRINKLE, false);
		REMAINS = Particles.simple(MiscIds.Particles.REMAINS, false);
		WARDEN_EXPLOSION = Particles.simple(MiscIds.Particles.WARDEN_EXPLOSION, true);
		ALERT = Particles.simple(MiscIds.Particles.ALERT, true);
		ALERTED = Particles.simple(MiscIds.Particles.ALERTED, true);
		XPRARE = Particles.simple(MiscIds.Particles.XPRARE, true);
		XPEPIC = Particles.simple(MiscIds.Particles.XPEPIC, true);
		EPIC_PARTICLE = Particles.simple(MiscIds.Particles.EPIC_PARTICLE, false);
	}

	private MiscParticles() {}
}
