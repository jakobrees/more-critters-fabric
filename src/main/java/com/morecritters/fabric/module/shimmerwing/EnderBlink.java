package com.morecritters.fabric.module.shimmerwing;

import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.core.Sounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

/** The chorus-fruit hop both shimmer critters do when wet, stuck in a wall, or (the shimmerwing) after eating chorus. */
final class EnderBlink {
	private static final double HORIZONTAL_REACH = 10.0;

	/** Leaves a puff of portal particles and reappears up to ten blocks away and {@code maxRise} blocks higher. */
	static void blinkAway(LivingEntity entity, double maxRise) {
		Particles.spawnAt(entity, ParticleTypes.PORTAL, 5, 0.2, 0.2, 0.2, 0.1);
		RandomSource random = entity.getRandom();
		entity.teleportTo(
			entity.getX() + Mth.nextDouble(random, -HORIZONTAL_REACH, HORIZONTAL_REACH),
			entity.getY() + Mth.nextDouble(random, 0.0, maxRise),
			entity.getZ() + Mth.nextDouble(random, -HORIZONTAL_REACH, HORIZONTAL_REACH));
		Sounds.playAt(entity, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.NEUTRAL, 1.0F, 1.0F);
	}

	private EnderBlink() {}
}
