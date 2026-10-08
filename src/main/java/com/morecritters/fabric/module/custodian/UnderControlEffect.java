package com.morecritters.fabric.module.custodian;

import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.ids.NervoidIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Possession by a nervoid: the victim staggers about, turning and hopping at random. When the
 * effect ends the nervoid drops out of its head.
 */
public class UnderControlEffect extends MobEffect {
	public UnderControlEffect() {
		super(MobEffectCategory.HARMFUL, -2065926);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int remainingDuration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity victim, int amplifier) {
		if (victim instanceof Player player && player.hasInfiniteMaterials()) {
			return true;
		}
		var random = victim.getRandom();
		victim.push(Mth.nextDouble(random, -0.3, 0.3), -0.1, Mth.nextDouble(random, -0.3, 0.3));
		float yaw = victim.getYRot() + (float) Mth.nextDouble(random, -5.0, 5.0);
		victim.setYRot(yaw);
		victim.setXRot((float) victim.getLookAngle().y);
		victim.setYBodyRot(yaw);
		victim.setYHeadRot(yaw);
		victim.yRotO = yaw;
		victim.xRotO = victim.getXRot();
		victim.yBodyRotO = yaw;
		victim.yHeadRotO = yaw;
		if (random.nextInt(10) == 0 && victim.onGround()) {
			victim.setDeltaMovement(victim.getDeltaMovement().x, 0.4, victim.getDeltaMovement().z);
		}
		return true;
	}

	/** The nervoid leaves its host when the effect runs out or is cleared. */
	@Override
	public void onEffectRemoved(MobEffectInstance instance, LivingEntity host) {
		if (!(host.level() instanceof ServerLevel level)) {
			return;
		}
		BuiltInRegistries.ENTITY_TYPE.getOptional(NervoidIds.Entities.NERVOID)
			.map(type -> type.spawn(level, host.blockPosition().above(), EntitySpawnReason.MOB_SUMMONED))
			.ifPresent(nervoid -> nervoid.setDeltaMovement(0.2, 0.5, 0.0));
		BuiltInRegistries.SOUND_EVENT.getOptional(NervoidIds.Sounds.ENTITY_NERVOID_UNPOSSESS)
			.ifPresent(sound -> Sounds.playAt(host, sound));
	}
}
