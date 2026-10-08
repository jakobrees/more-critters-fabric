package com.morecritters.fabric.module.nervoid;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Brain Scented: every undead mob within ten blocks hunts the bearer, unless it is a creative or spectator player. */
public final class BrainScentedEffect extends MobEffect {
	private static final int COLOUR = -5465;
	private static final double SCENT_RANGE = 10.0;

	public BrainScentedEffect() {
		super(MobEffectCategory.HARMFUL, COLOUR);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		if (entity instanceof ServerPlayer player && (player.hasInfiniteMaterials() || player.isSpectator())) {
			return true;
		}
		for (Mob undead : level.getEntitiesOfClass(Mob.class, entity.getBoundingBox().inflate(SCENT_RANGE),
				mob -> mob != entity && mob.is(EntityTypeTags.UNDEAD))) {
			undead.setTarget(entity);
		}
		return true;
	}
}
