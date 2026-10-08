package com.morecritters.fabric.module.bomb_jelly;

import com.morecritters.fabric.core.Sounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Combustion, from eating explosive jelly: a ticking bomb. In its last five seconds a fuse smokes
 * over the head and beeps ever higher, then flames for the final second, and when it runs out the
 * eater explodes. Creative and spectator players only hear a fizzle.
 */
public class CombustionEffect extends MobEffect {
	private static final int COLOUR = -2414809;
	private static final float EXPLOSION_POWER = 4.0F;
	private static final DustParticleOptions FUSE_SMOKE = new DustParticleOptions(0x1A1A1A, 1.5F);

	CombustionEffect() {
		super(MobEffectCategory.HARMFUL, COLOUR);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		MobEffectInstance combustion = entity.getEffect(BombJellyModule.COMBUSTION);
		if (combustion == null) {
			return true;
		}
		int remaining = combustion.getDuration();
		if (remaining == 1) {
			goOff(level, entity);
		}
		if (remaining <= 100) {
			ParticleOptions fuse = remaining > 20 ? FUSE_SMOKE : ParticleTypes.FLAME;
			level.sendParticles(fuse, true, true, entity.getX(), entity.getY() + entity.getBbHeight() + 0.2, entity.getZ(), 1, 0.0, 0.0, 0.0, remaining > 20 ? 1.0 : 0.0);
		}
		switch (remaining) {
			case 100 -> Sounds.playAt(entity, BombJellyModule.BEEP_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
			case 80 -> Sounds.playAt(entity, BombJellyModule.BEEP_SOUND, SoundSource.NEUTRAL, 1.0F, 1.2F);
			case 60 -> Sounds.playAt(entity, BombJellyModule.BEEP_SOUND, SoundSource.NEUTRAL, 1.0F, 1.8F);
			case 40 -> Sounds.playAt(entity, BombJellyModule.BEEP_SOUND, SoundSource.NEUTRAL, 1.0F, 2.0F);
			case 20 -> Sounds.playAt(entity, BombJellyModule.WARNING_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
			default -> {
			}
		}
		return true;
	}

	private static void goOff(ServerLevel level, LivingEntity entity) {
		if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
			Sounds.playAt(entity, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 1.0F, 1.0F);
		} else {
			level.explode(null, entity.getX(), entity.getY(), entity.getZ(), EXPLOSION_POWER, net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
		}
	}
}
