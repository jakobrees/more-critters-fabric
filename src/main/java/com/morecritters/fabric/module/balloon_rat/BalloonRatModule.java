package com.morecritters.fabric.module.balloon_rat;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.ids.BalloonRatIds;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;

/**
 * The balloon rat, a pet that eats cupcakes and inflates; the pink monster of the hallucinazium
 * poison; the three poisons (stagnation, muscle ache, hallucinazium) with their toxin bladders,
 * poisoned cupcakes and potions.
 */
public final class BalloonRatModule implements Module {
	public static SoundEvent IDLE_SOUND, HURT_SOUND, DEATH_SOUND, INFLATE_SOUND, DEFLATE_SOUND, TRANSFORM_SOUND;
	public static SoundEvent HALLUCINAZIUM_AMBIENT_SOUND, MONSTER_LAUGH_SOUND;

	public static SimpleParticleType PINK_EYE, PINK_SPIRAL, MEDIC_STRIPE, SOLDIER_STRIPE;

	@Override
	public void register() {
		IDLE_SOUND = Registration.sound(BalloonRatIds.Sounds.ENTITY_BALLOON_RAT_IDLE);
		HURT_SOUND = Registration.sound(BalloonRatIds.Sounds.ENTITY_BALLOON_RAT_HURT);
		DEATH_SOUND = Registration.sound(BalloonRatIds.Sounds.ENTITY_BALLOON_RAT_DEATH);
		INFLATE_SOUND = Registration.sound(BalloonRatIds.Sounds.ENTITY_BALLOON_RAT_INFLATE);
		DEFLATE_SOUND = Registration.sound(BalloonRatIds.Sounds.ENTITY_BALLOON_RAT_DEFLATE);
		TRANSFORM_SOUND = Registration.sound(BalloonRatIds.Sounds.ENTITY_BALLOON_RAT_TRANSFORM);
		HALLUCINAZIUM_AMBIENT_SOUND = Registration.sound(BalloonRatIds.Sounds.AMBIENT_HALLUCINAZIUM_IDLE);
		MONSTER_LAUGH_SOUND = Registration.sound(BalloonRatIds.Sounds.ENTITY_HALLUCINAZIUM_MONSTER_LAUGH);

		PINK_EYE = Particles.simple(BalloonRatIds.Particles.PINK_EYE, true);
		PINK_SPIRAL = Particles.simple(BalloonRatIds.Particles.PINK_SPIRAL, true);
		MEDIC_STRIPE = Particles.simple(BalloonRatIds.Particles.MEDIC_STRIPE, false);
		SOLDIER_STRIPE = Particles.simple(BalloonRatIds.Particles.SOLDIER_STRIPE, false);

		BalloonRatEffects.register();
		BalloonRatEntities.register();
		BalloonRatItems.register();
	}
}
