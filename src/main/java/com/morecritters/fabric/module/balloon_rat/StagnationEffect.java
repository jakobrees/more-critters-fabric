package com.morecritters.fabric.module.balloon_rat;

import com.mojang.serialization.Codec;
import com.morecritters.fabric.ids.BalloonRatIds;
import com.morecritters.fabric.ids.MiscIds;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Stagnation: the victim's health can only go down. Whatever it had when the effect began is a
 * ceiling, and every hit lowers the ceiling, so no healing sticks until the effect wears off.
 */
final class StagnationEffect extends MobEffect {
	private static final int COLOUR = -10352118;
	/** The original's loop ran (int) nextDouble(3, 7) times: three to six hearts. */
	private static final int LOCKED_HEARTS_MIN = 3, LOCKED_HEARTS_MAX = 6;
	private static final double LOCKED_HEART_SPEED = 0.02;

	/** The victim's health ceiling; 0 means none was recorded. Saved like the original's persistent data. */
	private static final AttachmentType<Float> HEALTH_CEILING =
		AttachmentRegistry.createPersistent(BalloonRatIds.Effects.STAGNATION.withSuffix("_health"), Codec.FLOAT);

	StagnationEffect() {
		super(MobEffectCategory.HARMFUL, COLOUR);
	}

	@Override
	public void onEffectStarted(LivingEntity victim, int amplifier) {
		if (!BalloonRatEffects.isBalloonRat(victim)) {
			victim.setAttached(HEALTH_CEILING, victim.getHealth());
		}
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity victim, int amplification) {
		float ceiling = victim.getAttachedOrElse(HEALTH_CEILING, 0.0F);
		if (BalloonRatEffects.isBalloonRat(victim) || ceiling == 0.0F) {
			return true;
		}
		if (victim.getHealth() > ceiling) {
			victim.setHealth(ceiling);
			showLockedHeart(level, victim);
		} else if (victim.getHealth() < ceiling) {
			victim.setAttached(HEALTH_CEILING, victim.getHealth());
		}
		return true;
	}

	/** A healing attempt bounced off: a few locked hearts rise above the victim's head. */
	private static void showLockedHeart(ServerLevel level, LivingEntity victim) {
		if (BuiltInRegistries.PARTICLE_TYPE.getValue(MiscIds.Particles.LOCKED_HEART) instanceof SimpleParticleType lockedHeart) {
			int count = Mth.nextInt(victim.getRandom(), LOCKED_HEARTS_MIN, LOCKED_HEARTS_MAX);
			level.sendParticles(lockedHeart, true, true, victim.getX(), victim.getY() + victim.getBbHeight(), victim.getZ(),
				count, 0.0, 0.0, 0.0, LOCKED_HEART_SPEED);
		}
	}
}
