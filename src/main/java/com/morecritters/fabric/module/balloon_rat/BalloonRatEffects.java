package com.morecritters.fabric.module.balloon_rat;

import com.morecritters.fabric.ids.BalloonRatIds;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.alchemy.Potion;

/**
 * The three poisons a soldier balloon rat can carry: stagnation (no healing), muscle ache (moving
 * hurts) and hallucinazium (pink visions and jumpscares), with a potion of each. The brewing
 * recipes are data.
 */
public final class BalloonRatEffects {
	public static Holder<MobEffect> STAGNATION, MUSCLE_ACHE, HALLUCINAZIUM;

	private static final int STAGNATION_POTION_TICKS = 1800;
	private static final int MUSCLE_ACHE_POTION_TICKS = 1200;
	private static final int HALLUCINAZIUM_POTION_TICKS = 600;

	static void register() {
		STAGNATION = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, BalloonRatIds.Effects.STAGNATION, new StagnationEffect());
		MUSCLE_ACHE = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, BalloonRatIds.Effects.MUSCLE_ACHE, new MuscleAcheEffect());
		HALLUCINAZIUM = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, BalloonRatIds.Effects.HALLUCINAZIUM, new HallucinaziumEffect());

		potion(BalloonRatIds.Potions.STAGNATION_POTION, STAGNATION, STAGNATION_POTION_TICKS);
		potion(BalloonRatIds.Potions.MUSCLE_ACHE_POTION, MUSCLE_ACHE, MUSCLE_ACHE_POTION_TICKS);
		potion(BalloonRatIds.Potions.HALLUCINAZIUM_POTION, HALLUCINAZIUM, HALLUCINAZIUM_POTION_TICKS);

		MuscleAche.registerEvents();
	}

	private static void potion(Identifier id, Holder<MobEffect> effect, int ticks) {
		Registry.register(BuiltInRegistries.POTION, id, new Potion(id.getPath(), new MobEffectInstance(effect, ticks, 0, false, true)));
	}

	/** Balloon rats carry these poisons and shrug them all off. */
	static boolean isBalloonRat(LivingEntity entity) {
		return entity.getType() == BalloonRatEntities.BALLOON_RAT;
	}

	private BalloonRatEffects() {}
}
