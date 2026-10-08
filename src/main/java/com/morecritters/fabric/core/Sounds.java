package com.morecritters.fabric.core;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

/** Playing a sound at an entity from server code so every nearby player hears it. */
public final class Sounds {
	public static void playAt(Entity entity, SoundEvent sound, SoundSource source, float volume, float pitch) {
		if (!entity.level().isClientSide()) {
			entity.level().playSound(null, BlockPos.containing(entity.position()), sound, source, volume, pitch);
		}
	}

	public static void playAt(Entity entity, SoundEvent sound) {
		playAt(entity, sound, SoundSource.NEUTRAL, 1.0F, 1.0F);
	}

	private Sounds() {}
}
