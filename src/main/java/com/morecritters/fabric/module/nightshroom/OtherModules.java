package com.morecritters.fabric.module.nightshroom;

import java.util.Optional;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Content of other modules (the lightfly, the stripes, the mightshroom's sounds and blocks),
 * looked up by name where it is used. Each lookup is empty while the owning module is a stub, so
 * this module runs on its own; nothing here falls back to a registry's default entry.
 */
final class OtherModules {
	static Optional<EntityType<?>> entityType(Identifier id) {
		return BuiltInRegistries.ENTITY_TYPE.getOptional(id);
	}

	static Optional<SoundEvent> sound(Identifier id) {
		return BuiltInRegistries.SOUND_EVENT.getOptional(id);
	}

	static Optional<Item> item(Identifier id) {
		return BuiltInRegistries.ITEM.getOptional(id);
	}

	static Optional<Block> block(Identifier id) {
		return BuiltInRegistries.BLOCK.getOptional(id);
	}

	/** A plain particle of another module; particles with options are not looked up this way. */
	static Optional<ParticleOptions> particle(Identifier id) {
		return BuiltInRegistries.PARTICLE_TYPE.getOptional(id)
			.filter(SimpleParticleType.class::isInstance)
			.map(SimpleParticleType.class::cast);
	}

	static boolean isType(Entity entity, Identifier id) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(id);
	}

	private OtherModules() {}
}
