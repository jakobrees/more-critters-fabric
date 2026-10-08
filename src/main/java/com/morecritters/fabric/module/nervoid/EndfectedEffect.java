package com.morecritters.fabric.module.nervoid;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Endfected: does nothing while it lasts; a creature that dies with it releases a nervoid (see {@link NervoidModule}). */
public final class EndfectedEffect extends MobEffect {
	private static final int COLOUR = -4685313;

	public EndfectedEffect() {
		super(MobEffectCategory.BENEFICIAL, COLOUR);
	}
}
