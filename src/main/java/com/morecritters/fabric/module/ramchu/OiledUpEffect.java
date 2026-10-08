package com.morecritters.fabric.module.ramchu;

import com.morecritters.fabric.core.Sounds;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Oiled up, from drinking ramchu oil: direct hits by a living attacker slip off. Each slipped
 * hit costs ten seconds of the effect.
 */
public class OiledUpEffect extends MobEffect {
	private static final int COLOUR = -2638721;
	private static final int TICKS_LOST_PER_SLIP = 200;

	OiledUpEffect() {
		super(MobEffectCategory.BENEFICIAL, COLOUR);
	}

	static void registerSlipping() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(OiledUpEffect::allowDamage);
	}

	private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		MobEffectInstance oil = entity.getEffect(RamchuModule.OILED_UP);
		if (oil == null || !(source.getEntity() instanceof LivingEntity) || !source.isDirect()) return true;
		Sounds.playAt(entity, RamchuModule.OIL_SLIP_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
		int remaining = oil.getDuration() - TICKS_LOST_PER_SLIP;
		entity.removeEffect(RamchuModule.OILED_UP);
		if (remaining > 0) {
			entity.addEffect(new MobEffectInstance(RamchuModule.OILED_UP, remaining, 0, false, true));
		}
		return false;
	}
}
