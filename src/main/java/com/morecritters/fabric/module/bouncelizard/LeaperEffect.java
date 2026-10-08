package com.morecritters.fabric.module.bouncelizard;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Leaper: a hidden marker effect for leaping critters. It wears off as soon as its bearer is in water
 * with something solid right above its head, so a leap cannot carry on under a ceiling of blocks.
 */
public class LeaperEffect extends MobEffect {
	public LeaperEffect() {
		super(MobEffectCategory.NEUTRAL, 0xFFFFFF);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		if (entity.isInWater() && isCovered(level.getBlockState(BlockPos.containing(entity.getX(), entity.getY() + 1.0, entity.getZ())))) {
			// Returning false removes the effect.
			return false;
		}
		return true;
	}

	private static boolean isCovered(BlockState above) {
		return !above.is(Blocks.AIR) && !above.is(Blocks.WATER) && !above.is(Blocks.BUBBLE_COLUMN);
	}
}
