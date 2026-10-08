package com.morecritters.fabric.module.nightshroom;

import com.morecritters.fabric.core.Sounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Whatever a frightshroom or nightshroom hurts, directly or with its rot pieces, gets the
 * shroom's attack sound; a frightshroom also sheds rot as it strikes.
 */
final class ShroomAttacks {
	static void onDamage(LivingEntity victim, DamageSource source) {
		if (!(victim.level() instanceof ServerLevel level)) return;
		if (source.getEntity() instanceof FrightshroomEntity frightshroom) {
			Sounds.playAt(victim, NightshroomModule.FRIGHTSHROOM_ATTACK_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
			frightshroom.shedRot(level);
		} else if (source.getEntity() instanceof NightshroomEntity) {
			Sounds.playAt(victim, NightshroomModule.NIGHTSHROOM_ATTACK_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
		}
	}

	private ShroomAttacks() {}
}
