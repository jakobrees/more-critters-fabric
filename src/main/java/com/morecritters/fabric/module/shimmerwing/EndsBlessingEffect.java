package com.morecritters.fabric.module.shimmerwing;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import com.morecritters.fabric.ids.ShimmerwingIds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** End's Blessing: jumps launch you a block-plus into the air and you drift down slowly. */
public class EndsBlessingEffect extends MobEffect {
	private static final int COLOUR = -3502103;
	/** Vanilla's base jump strength is 0.42; the original set the jump's upward speed to 1.0. */
	private static final double EXTRA_JUMP_STRENGTH = 1.0 - 0.42;

	public EndsBlessingEffect() {
		super(MobEffectCategory.BENEFICIAL, COLOUR);
		addAttributeModifier(Attributes.JUMP_STRENGTH, ShimmerwingIds.Effects.ENDS_BLESSING, EXTRA_JUMP_STRENGTH, AttributeModifier.Operation.ADD_VALUE);
	}

	@Override
	public void onEffectStarted(LivingEntity entity, int amplifier) {
		Advancements.award(entity, MoreCritters.id("get_ends_blessing"));
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	/** While airborne, keeps slow falling topped up. */
	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		if (!entity.onGround()) {
			entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0, false, false));
		}
		return true;
	}
}
