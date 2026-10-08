package com.morecritters.fabric.module.corpse_gear;

import com.morecritters.fabric.ids.MightshroomIds;
import com.morecritters.fabric.ids.MiscIds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Stunned: stars circle over the victim's head and it barely drifts the way it faces. The mightshroom
 * handles its own stun and is only given the stars.
 */
public class StunnedEffect extends MobEffect {
	private static final int COLOUR = -9216;
	/** A star appears every 5 ticks at the next of 8 points around the head, so one lap takes 40 ticks. */
	private static final int TICKS_PER_STAR = 5;
	private static final double[][] STAR_OFFSETS = {
		{0.5, 0.0}, {0.5, 0.5}, {0.0, 0.5}, {-0.5, 0.5}, {-0.5, 0.0}, {-0.5, -0.5}, {0.0, -0.5}, {0.5, -0.5}
	};
	private static final double STAR_HEIGHT = 0.5;
	private static final double STAR_SPEED = 0.02;
	/** The victim's motion is replaced each tick by this fraction of its look direction. */
	private static final double DRIFT = 0.02;

	StunnedEffect() {
		super(MobEffectCategory.NEUTRAL, COLOUR);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity victim, int amplifier) {
		circleStars(level, victim);
		if (!BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).equals(MightshroomIds.Entities.MIGHTSHROOM)) {
			victim.setDeltaMovement(victim.getLookAngle().scale(DRIFT));
		}
		return true;
	}

	private static void circleStars(ServerLevel level, LivingEntity victim) {
		if (victim.tickCount % TICKS_PER_STAR != 0) return;
		if (!(BuiltInRegistries.PARTICLE_TYPE.getValue(MiscIds.Particles.STUN_STAR) instanceof ParticleOptions star)) return;
		double[] offset = STAR_OFFSETS[(victim.tickCount / TICKS_PER_STAR) % STAR_OFFSETS.length];
		level.sendParticles(star, true, false, victim.getX() + offset[0], victim.getY() + victim.getBbHeight() + STAR_HEIGHT, victim.getZ() + offset[1],
			1, 0.0, 0.0, 0.0, STAR_SPEED);
	}
}
