package com.morecritters.fabric.module.balloon_rat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Muscle ache: exertion hurts. The pains themselves are in {@link MuscleAche}; this checks the per-tick ones. */
final class MuscleAcheEffect extends MobEffect {
	private static final int COLOUR = -5225165;

	MuscleAcheEffect() {
		super(MobEffectCategory.HARMFUL, COLOUR);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity sufferer, int amplification) {
		MuscleAche.tick(level, sufferer);
		return true;
	}
}
