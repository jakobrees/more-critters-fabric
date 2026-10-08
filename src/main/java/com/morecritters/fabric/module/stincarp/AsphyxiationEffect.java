package com.morecritters.fabric.module.stincarp;

import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.ids.BalloonRatIds;
import com.morecritters.fabric.ids.MiscIds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Suffocation turned inside out: in water the victim runs out of air twice as fast, and on
 * land it drowns, taking a point of drowning damage every second with a puff of bubbles.
 * Balloon rats float above it all and are immune.
 */
final class AsphyxiationEffect extends MobEffect {
	private static final int COLOUR = -8922625;
	private static final int DAMAGE_INTERVAL = 20;
	private static final int AIR_LOSS_DELAY = 10;
	private static final int BUBBLES_MIN = 3, BUBBLES_MAX = 5;

	AsphyxiationEffect() {
		super(MobEffectCategory.HARMFUL, COLOUR);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity victim, int amplification) {
		if (isBalloonRat(victim)) {
			return true;
		}
		if (victim.isInWater()) {
			ServerScheduler.runLater(AIR_LOSS_DELAY, () -> victim.setAirSupply(victim.getAirSupply() - 1));
		} else if (victim.tickCount % DAMAGE_INTERVAL == 0) {
			victim.hurtServer(level, victim.damageSources().drown(), 1.0F);
			puffBubbles(level, victim);
		}
		return true;
	}

	private static boolean isBalloonRat(LivingEntity entity) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(BalloonRatIds.Entities.BALLOON_RAT);
	}

	private static void puffBubbles(ServerLevel level, LivingEntity victim) {
		if (BuiltInRegistries.PARTICLE_TYPE.getValue(MiscIds.Particles.DROWN_BUBBLE) instanceof ParticleOptions bubble) {
			int count = Mth.nextInt(level.getRandom(), BUBBLES_MIN, BUBBLES_MAX);
			level.sendParticles(bubble, true, true, victim.getX(), victim.getY() + victim.getBbHeight(), victim.getZ(), count, 0, 0, 0, 0.02);
		}
	}
}
