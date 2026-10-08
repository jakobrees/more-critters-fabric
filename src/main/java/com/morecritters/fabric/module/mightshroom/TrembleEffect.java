package com.morecritters.fabric.module.mightshroom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Tremble: the victim's head and body jitter by up to three degrees every tick (the mightshroom's scream and stomps). */
public class TrembleEffect extends MobEffect {
	private static final int COLOUR = -1;
	private static final double JITTER_DEGREES = 3.0;

	public TrembleEffect() {
		super(MobEffectCategory.NEUTRAL, COLOUR);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		float yaw = (float) (entity.getYRot() + Mth.nextDouble(entity.getRandom(), -JITTER_DEGREES, JITTER_DEGREES));
		float pitch = (float) (entity.getXRot() + Mth.nextDouble(entity.getRandom(), -JITTER_DEGREES, JITTER_DEGREES));
		entity.setYRot(yaw);
		entity.setXRot(pitch);
		entity.setYBodyRot(yaw);
		entity.setYHeadRot(yaw);
		entity.yRotO = yaw;
		entity.xRotO = pitch;
		entity.yBodyRotO = yaw;
		entity.yHeadRotO = yaw;
		return true;
	}
}
