package com.morecritters.fabric.module.nauticrawl;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * The nauticrawls' own one-tick effect that sets off a swim stroke. It does nothing to any
 * other creature.
 */
public class SwimmerEffect extends MobEffect {
	public SwimmerEffect() {
		super(MobEffectCategory.NEUTRAL, 0xFFFFFF);
	}

	@Override
	public void onEffectStarted(LivingEntity mob, int amplifier) {
		if (mob instanceof NauticrawlEntity nauticrawl && !mob.level().isClientSide()) nauticrawl.swimStroke();
	}
}
