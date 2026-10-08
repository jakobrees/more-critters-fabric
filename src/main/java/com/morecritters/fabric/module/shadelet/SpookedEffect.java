package com.morecritters.fabric.module.shadelet;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Spooked: a shadelet screamed at you. You freeze for a moment, feel weak, and stare at it. */
public class SpookedEffect extends MobEffect {
	private static final int COLOUR = -7484710;
	private static final int FREEZE_TICKS = 10;
	private static final int FREEZE_AMPLIFIER = 49;
	private static final int WEAKNESS_TICKS = 100;
	private static final double STARE_RANGE = 3.0;

	public SpookedEffect() {
		super(MobEffectCategory.HARMFUL, COLOUR);
	}

	@Override
	public void onEffectStarted(LivingEntity entity, int amplifier) {
		if (entity.level().isClientSide()) {
			return;
		}
		entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, FREEZE_TICKS, FREEZE_AMPLIFIER, false, false));
		entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_TICKS, 0, false, true));
		stareAtNearbyShadelets(entity);
	}

	/** Turns the victim's head to the shadelets close by; the last (farthest) one wins, as in the original. */
	private static void stareAtNearbyShadelets(LivingEntity entity) {
		Vec3 centre = entity.position();
		entity.level().getEntitiesOfClass(ShadeletEntity.class, new AABB(centre, centre).inflate(STARE_RANGE)).stream()
			.sorted((a, b) -> Double.compare(a.distanceToSqr(centre), b.distanceToSqr(centre)))
			.forEach(shadelet -> entity.lookAt(EntityAnchorArgument.Anchor.EYES,
				new Vec3(shadelet.getX(), shadelet.getY() + shadelet.getBbHeight(), shadelet.getZ())));
	}
}
