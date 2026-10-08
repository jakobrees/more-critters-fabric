package com.morecritters.fabric.module.corpse_gear;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Crew's rum: the corpse crew's drink. It wears off at once, leaving the crew with Regeneration II and anyone
 * else with Poison II, for five seconds.
 */
public class CrewsRumEffect extends MobEffect {
	private static final int COLOUR = -1283302;
	private static final int AFTERMATH_TICKS = 100;
	private static final int AFTERMATH_AMPLIFIER = 1;

	CrewsRumEffect() {
		super(MobEffectCategory.HARMFUL, COLOUR);
	}

	/** Applied as the rum goes down rather than during its tick, so the effect list is not changed while it is walked. */
	@Override
	public void onEffectStarted(LivingEntity drinker, int amplifier) {
		if (drinker.level().isClientSide()) return;
		boolean crew = Crew.isOneOf(drinker, Crew.RUM_DRINKERS);
		drinker.addEffect(new MobEffectInstance(crew ? MobEffects.REGENERATION : MobEffects.POISON, AFTERMATH_TICKS, AFTERMATH_AMPLIFIER, false, true));
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	/** Ends the rum on its first tick. */
	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity drinker, int amplifier) {
		return false;
	}
}
