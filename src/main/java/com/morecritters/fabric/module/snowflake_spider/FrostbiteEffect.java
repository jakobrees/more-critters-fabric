package com.morecritters.fabric.module.snowflake_spider;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Keeps the victim frozen solid, as if standing in powder snow; snowflake spiders shrug it off. */
final class FrostbiteEffect extends MobEffect {
	private static final int COLOUR = -13580549;
	private static final int FROZEN_TICKS = 200;

	FrostbiteEffect() {
		super(MobEffectCategory.HARMFUL, COLOUR);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity victim, int amplification) {
		if (!(victim instanceof SnowflakeSpiderEntity)) {
			victim.setTicksFrozen(FROZEN_TICKS);
		}
		return true;
	}
}
