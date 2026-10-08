package com.morecritters.fabric.module.critterling_system;

import com.morecritters.fabric.core.ServerScheduler;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * The twinkle the original drew around epic critterlings and evolightened mobs: four puffs of three
 * particles, three ticks apart, at the spot where it started. Sent with {@code force} like the
 * original's {@code /particle ... force} commands.
 */
public final class Sparkle {
	private static final int PUFFS = 4, PUFF_DELAY = 3, PER_PUFF = 3;
	private static final double SPREAD = 0.3, SPEED = 0.01;

	public static void at(ServerLevel level, ParticleOptions particle, Vec3 pos) {
		puff(level, particle, pos, PUFFS);
	}

	private static void puff(ServerLevel level, ParticleOptions particle, Vec3 pos, int remaining) {
		level.sendParticles(particle, true, false, pos.x, pos.y, pos.z, PER_PUFF, SPREAD, SPREAD, SPREAD, SPEED);
		if (remaining > 1) {
			ServerScheduler.runLater(PUFF_DELAY, () -> puff(level, particle, pos, remaining - 1));
		}
	}

	private Sparkle() {}
}
