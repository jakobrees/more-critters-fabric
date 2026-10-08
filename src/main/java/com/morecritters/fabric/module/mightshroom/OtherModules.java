package com.morecritters.fabric.module.mightshroom;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;

/**
 * Particles, sounds and creatures of other modules (the reaper's particles, the misc stripes, the
 * nightshroom's fungal zombies...), looked up by id when used. Until their module is ported they are
 * simply skipped.
 */
final class OtherModules {
	/** Like the original's {@code /particle ... force} commands. */
	static void particles(ServerLevel level, Identifier particle, double x, double y, double z, int count,
	                      double spreadX, double spreadY, double spreadZ, double speed) {
		if (BuiltInRegistries.PARTICLE_TYPE.getValue(particle) instanceof ParticleOptions options) {
			level.sendParticles(options, true, false, x, y, z, count, spreadX, spreadY, spreadZ, speed);
		}
	}

	static void sound(ServerLevel level, BlockPos pos, Identifier sound, SoundSource source) {
		BuiltInRegistries.SOUND_EVENT.get(sound).ifPresent(event -> level.playSound(null, pos, event.value(), source, 1.0F, 1.0F));
	}

	static Optional<EntityType<?>> entityType(Identifier id) {
		return BuiltInRegistries.ENTITY_TYPE.getOptional(id);
	}

	static boolean isOfType(Entity entity, Identifier type) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(type);
	}

	/** Spawns another module's creature at a block, as {@code EntityType.spawn} does; empty if that module is absent. */
	static Optional<Entity> spawn(ServerLevel level, Identifier type, BlockPos pos) {
		return entityType(type).map(entityType -> entityType.spawn(level, pos, EntitySpawnReason.MOB_SUMMONED));
	}

	private OtherModules() {}
}
