package com.morecritters.fabric.module.mightshroom;

import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.ids.MiscIds;
import com.morecritters.fabric.ids.ShimmerwingIds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Gift of Life (life stew, fungal staff): one tick in eight an angel appears over the head, and half a
 * second later it heals one to three health with a chime and a shimmer.
 */
public class GiftOfLifeEffect extends MobEffect {
	private static final int COLOUR = -2816;
	private static final int ANGEL_CHANCE = 8;
	private static final int HEAL_DELAY = 10;

	public GiftOfLifeEffect() {
		super(MobEffectCategory.BENEFICIAL, COLOUR);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		if (entity.getRandom().nextInt(ANGEL_CHANCE) != 0) {
			return true;
		}
		double x = entity.getX(), y = entity.getY(), z = entity.getZ();
		double headY = y + entity.getBbHeight();
		OtherModules.particles(level, MiscIds.Particles.ANGEL, x, headY, z, 1, 0.5, 0.5, 0.5, 0.01);
		ServerScheduler.runLater(HEAL_DELAY, () -> {
			level.playSound(null, BlockPos.containing(x, y, z), MightshroomModule.ANGEL_HEAL_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
			if (entity.isAlive()) {
				entity.setHealth(entity.getHealth() + Mth.nextInt(entity.getRandom(), 1, 3));
			}
			int shimmers = (int) Mth.nextDouble(entity.getRandom(), 3.0, 6.0);
			for (int i = 0; i < shimmers; i++) {
				OtherModules.particles(level, ShimmerwingIds.Particles.HEAL_SHIMMER, x, headY, z, 1, 0.3, 0.3, 0.3, 0.02);
			}
		});
		return true;
	}
}
