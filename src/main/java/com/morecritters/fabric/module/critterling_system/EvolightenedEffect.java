package com.morecritters.fabric.module.critterling_system;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Evolightened, given by the evolite chandelier: the mob takes no damage at all, and now and
 * then twinkles with evolite light.
 */
public class EvolightenedEffect extends MobEffect {
	private static final int COLOUR = -5721382;
	private static final int SPARKLE_CHANCE = 30;

	public EvolightenedEffect() {
		super(MobEffectCategory.NEUTRAL, COLOUR);
	}

	/** Cancels all incoming damage, as the original's damage-event handler did. */
	static void registerInvulnerability() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !entity.hasEffect(CritterlingSystemModule.EVOLIGHTENED));
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		if (entity.getRandom().nextInt(SPARKLE_CHANCE) == 0) {
			Sparkle.at(level, CritterlingSystemModule.EVOLIGHTENED_PARTICLE, entity.position());
		}
		return true;
	}
}
