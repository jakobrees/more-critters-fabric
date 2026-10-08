package com.morecritters.fabric.module.mightshroom;

import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.ids.CorpseGearIds;
import com.morecritters.fabric.ids.GravediggerIds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Imminent Death (death stew, fungal staff): one tick in eight a reaper appears over the head, readies
 * its scythe and a moment later strikes for three to six damage. A creative player only gets a mad
 * reaper that grumbles now and then.
 */
public class ImminentDeathEffect extends MobEffect {
	private static final int COLOUR = -15199480;
	private static final int REAPER_CHANCE = 8;
	private static final int GRUMBLE_CHANCE = 7;
	private static final int READY_DELAY = 5, STRIKE_DELAY = 15;
	private static final double MIN_DAMAGE = 3.0, MAX_DAMAGE = 6.0;

	public ImminentDeathEffect() {
		super(MobEffectCategory.HARMFUL, COLOUR);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		if (entity.getRandom().nextInt(REAPER_CHANCE) != 0) {
			return true;
		}
		double headY = entity.getY() + entity.getBbHeight();
		BlockPos at = entity.blockPosition();
		if (entity instanceof Player player && player.hasInfiniteMaterials()) {
			OtherModules.particles(level, GravediggerIds.Particles.MAD_REAPER, entity.getX(), headY, entity.getZ(), 1, 0.5, 0.5, 0.5, 0.01);
			if (entity.getRandom().nextInt(GRUMBLE_CHANCE) == 0) {
				OtherModules.sound(level, at, CorpseGearIds.Sounds.AMBIENT_REAPER_GRUMBLE, SoundSource.AMBIENT);
			}
			return true;
		}
		OtherModules.particles(level, GravediggerIds.Particles.REAPER, entity.getX(), headY, entity.getZ(), 1, 0.5, 0.5, 0.5, 0.01);
		ServerScheduler.runLater(READY_DELAY, () -> OtherModules.sound(level, at, GravediggerIds.Sounds.AMBIENT_REAPER_READY, SoundSource.AMBIENT));
		ServerScheduler.runLater(STRIKE_DELAY, () -> {
			OtherModules.sound(level, at, GravediggerIds.Sounds.AMBIENT_REAPER_HIT, SoundSource.AMBIENT);
			entity.hurtServer(level, level.damageSources().generic(), (float) Mth.nextDouble(entity.getRandom(), MIN_DAMAGE, MAX_DAMAGE));
		});
		return true;
	}
}
