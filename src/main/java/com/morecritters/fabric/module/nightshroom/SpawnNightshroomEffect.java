package com.morecritters.fabric.module.nightshroom;

import com.geckolib.animatable.GeoEntity;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.ids.MightshroomIds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Turns a mightshroom into a nightshroom: the mightshroom goes through its transformation and,
 * when the effect wears off, bursts apart into a nightshroom facing the same way. On an ancient
 * skeleton it only makes the bones shake and sparks fly.
 */
public class SpawnNightshroomEffect extends MobEffect {
	private static final int FEATHER_WAVES = 3, FEATHER_WAVE_TICKS = 2;

	public SpawnNightshroomEffect() {
		super(MobEffectCategory.NEUTRAL, -1);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int remainingDuration, int amplifier) {
		return true;
	}

	@Override
	public void onEffectStarted(LivingEntity mob, int amplifier) {
		if (mob instanceof AncientSkeletonEntity skeleton) {
			skeleton.startRising();
		} else if (OtherModules.isType(mob, MightshroomIds.Entities.MIGHTSHROOM)) {
			if (mob instanceof GeoEntity animated) animated.triggerAnim(null, "transform");
			OtherModules.sound(MightshroomIds.Sounds.ENTITY_MIGHTSHROOM_TRANSFORM)
				.ifPresent(sound -> Sounds.playAt(mob, sound, SoundSource.HOSTILE, 1.0F, 1.0F));
		}
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplifier) {
		if (mob instanceof AncientSkeletonEntity skeleton) skeleton.sparkStripes(level);
		return true;
	}

	/** The mightshroom is gone; a nightshroom stands in its place amid flesh, mushroom and feathers. */
	@Override
	public void onEffectRemoved(MobEffectInstance instance, LivingEntity mob) {
		if (!(mob.level() instanceof ServerLevel level) || !OtherModules.isType(mob, MightshroomIds.Entities.MIGHTSHROOM)) return;
		Vec3 at = mob.position();
		Transformations.replace(level, mob, NightshroomModule.NIGHTSHROOM);

		Bursts.forced(level, Bursts.item(Items.ROTTEN_FLESH), at, 30, 0.5, 2.0, 0.5, 0.0);
		OtherModules.block(MightshroomIds.Blocks.MORI_SHROOM_BLOCK)
			.ifPresent(block -> Bursts.forced(level, Bursts.block(block), at, 30, 0.6, 3.0, 0.6, 0.0));
		OtherModules.particle(MightshroomIds.Particles.MIGHTSHROOM_FEATHER)
			.ifPresent(feather -> featherWaves(level, feather, at, FEATHER_WAVES));
	}

	private static void featherWaves(ServerLevel level, ParticleOptions feather, Vec3 at, int waves) {
		Bursts.forced(level, feather, at, 30, 1.0, 3.0, 1.0, 7.0);
		if (waves > 1) ServerScheduler.runLater(FEATHER_WAVE_TICKS, () -> featherWaves(level, feather, at, waves - 1));
	}
}
