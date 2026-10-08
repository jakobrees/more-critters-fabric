package com.morecritters.fabric.module.warptrap;

import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Sounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * "Digging in": while it lasts the bearer is held almost still. A warptrap plays its dig
 * animation and sprays nylium until it is hidden. The warptrap gives it to itself, hidden
 * (no particles, no icon).
 */
public class WarptrapDiggerEffect extends MobEffect {
	/** Slowness XXXI: effectively rooted in place. */
	private static final int ROOTED_AMPLIFIER = 30;

	public WarptrapDiggerEffect() {
		super(MobEffectCategory.NEUTRAL, -1);
	}

	@Override
	public void onEffectStarted(LivingEntity entity, int amplifier) {
		if (entity.level().isClientSide()) return;
		if (entity instanceof WarptrapEntity warptrap) warptrap.triggerAnim(Animations.ACTIONS, "dig");
		entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, WarptrapEntity.DIG_TICKS, ROOTED_AMPLIFIER, false, false));
		Sounds.playAt(entity, WarptrapModule.DIG_SOUND, SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		if (entity instanceof WarptrapEntity warptrap && !warptrap.isBuried()) warptrap.kickUpNylium(level);
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
		return true;
	}
}
