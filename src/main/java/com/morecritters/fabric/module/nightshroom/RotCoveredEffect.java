package com.morecritters.fabric.module.nightshroom;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Splattered with rot by a rot piece. It does nothing by itself; a player who has it sees rot
 * splatters over the screen (client module).
 */
public class RotCoveredEffect extends MobEffect {
	public RotCoveredEffect() {
		super(MobEffectCategory.NEUTRAL, -15790578);
	}
}
