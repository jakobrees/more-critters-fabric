package com.morecritters.fabric.module.critterling_system;

import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.ids.CritterlingsEIds;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * A lightfly (critterlings_e) that hits something burns out: it is gone with a fizz and its victim
 * catches fire for five seconds. The original's {@code AttackedByLightProcedure}, a global damage
 * handler listed with this module; it looks the lightfly up by id, so it does nothing until
 * critterlings_e registers it.
 */
final class LightflyStrike {
	private static final int BURN_SECONDS = 5;

	static void register() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) -> {
			Entity attacker = source.getEntity();
			if (attacker != null && BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType()).equals(CritterlingsEIds.Entities.LIGHTFLY)) {
				burnOut(victim, attacker);
			}
			return true;
		});
	}

	private static void burnOut(LivingEntity victim, Entity lightfly) {
		SoundEvent hit = BuiltInRegistries.SOUND_EVENT.getValue(CritterlingsEIds.Sounds.ENTITY_LIGHTFLY_HIT);
		if (hit != null) Sounds.playAt(victim, hit, SoundSource.NEUTRAL, 1.0F, 1.0F);
		lightfly.discard();
		victim.igniteForSeconds(BURN_SECONDS);
	}

	private LightflyStrike() {}
}
