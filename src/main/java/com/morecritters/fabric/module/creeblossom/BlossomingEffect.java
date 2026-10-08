package com.morecritters.fabric.module.creeblossom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Blossoming: an infected monster sheds petals now and then, and bursts into four creeblossoms when killed. */
public class BlossomingEffect extends MobEffect {
	private static final int COLOUR = -8808142;
	private static final int PETAL_CHANCE = 10;

	public BlossomingEffect() {
		super(MobEffectCategory.HARMFUL, COLOUR);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		if (entity.getRandom().nextInt(PETAL_CHANCE) == 0) {
			int count = 2 + entity.getRandom().nextInt(3);
			level.sendParticles(CreeblossomModule.BLOSSOM_PARTICLE, entity.getX(), entity.getY() + 1.0, entity.getZ(), count, 0.4, 0.4, 0.4, 0.01);
		}
		return true;
	}
}
