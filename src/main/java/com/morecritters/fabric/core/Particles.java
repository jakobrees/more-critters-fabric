package com.morecritters.fabric.core;

import net.minecraft.core.Registry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;

/** Particle types (both sides) and spawning them from server code. The client appearance is {@code client.core.SpriteParticle}. */
public final class Particles {
	/** @param alwaysShow true if the particle shows even on the "minimal" particle setting. */
	public static SimpleParticleType simple(Identifier id, boolean alwaysShow) {
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, id, FabricParticleTypes.simple(alwaysShow));
	}

	/** Spawns particles around an entity the way the original's {@code /particle ~ ~ ~ dx dy dz speed count} commands did. */
	public static void spawnAt(Entity entity, ParticleOptions particle, int count, double spreadX, double spreadY, double spreadZ, double speed) {
		if (entity.level() instanceof ServerLevel level) {
			level.sendParticles(particle, entity.getX(), entity.getY(), entity.getZ(), count, spreadX, spreadY, spreadZ, speed);
		}
	}

	private Particles() {}
}
