package com.morecritters.fabric.module.balloon_rat;

import com.mojang.serialization.Codec;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.ids.BalloonRatIds;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Hallucinazium: the victim reels with nausea, pink eyes and spirals swirl around its head and an
 * eerie hum comes and goes. Every four seconds comes a jumpscare: a player sees a pink monster
 * appear at one corner around it, laughing, and two seconds later takes one to three points of
 * magic damage; any other creature simply takes ten. The pink tint on screen is client-side.
 */
final class HallucinaziumEffect extends MobEffect {
	private static final int COLOUR = -29702;

	private static final int JUMPSCARE_INTERVAL = 80;
	private static final int NAUSEA_TICKS = 100;
	/** One tick in this many shows pink visions, and (independently) one in this many hums. */
	private static final int VISION_CHANCE = 10, HUM_CHANCE = 10;
	private static final int VISION_MIN = 2, VISION_MAX = 4;
	private static final int MOB_JUMPSCARE_EYES = 15;
	private static final float MOB_JUMPSCARE_DAMAGE = 10.0F;
	private static final int PLAYER_DAMAGE_DELAY = 40;
	private static final float PLAYER_DAMAGE_MIN = 1.0F, PLAYER_DAMAGE_MAX = 3.0F;
	private static final double MONSTER_OFFSET = 2.0;

	/** Ticks until the next jumpscare, counting down to 1. Saved like the original's persistent data. */
	private static final AttachmentType<Integer> JUMPSCARE_COUNTDOWN =
		AttachmentRegistry.createPersistent(BalloonRatIds.Effects.HALLUCINAZIUM.withSuffix("_jumpscare"), Codec.INT);

	HallucinaziumEffect() {
		super(MobEffectCategory.HARMFUL, COLOUR);
	}

	@Override
	public void onEffectStarted(LivingEntity victim, int amplifier) {
		victim.setAttached(JUMPSCARE_COUNTDOWN, JUMPSCARE_INTERVAL);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity victim, int amplification) {
		if (BalloonRatEffects.isBalloonRat(victim)) {
			return true;
		}
		int countdown = victim.getAttachedOrElse(JUMPSCARE_COUNTDOWN, 0) - 1;
		victim.setAttached(JUMPSCARE_COUNTDOWN, countdown);
		victim.addEffect(new MobEffectInstance(MobEffects.NAUSEA, NAUSEA_TICKS, 0, false, false));
		if (level.getRandom().nextInt(VISION_CHANCE) == 0) {
			showVisions(level, victim);
		}
		if (countdown == 1) {
			victim.setAttached(JUMPSCARE_COUNTDOWN, JUMPSCARE_INTERVAL);
			if (victim instanceof Player) {
				jumpscarePlayer(level, victim);
			} else {
				jumpscareMob(level, victim);
			}
		}
		if (level.getRandom().nextInt(HUM_CHANCE) == 0) {
			Sounds.playAt(victim, BalloonRatModule.HALLUCINAZIUM_AMBIENT_SOUND, SoundSource.AMBIENT, 1.0F, 1.0F);
		}
		return true;
	}

	private static void showVisions(ServerLevel level, LivingEntity victim) {
		int count = Mth.nextInt(level.getRandom(), VISION_MIN, VISION_MAX);
		aboveHead(level, victim, BalloonRatModule.PINK_EYE, count, 0.5, 0.12);
		aboveHead(level, victim, BalloonRatModule.PINK_SPIRAL, count, 0.2, 0.05);
	}

	/** A pink monster pops up at a random corner around the player, and the hit lands two seconds later. */
	private static void jumpscarePlayer(ServerLevel level, LivingEntity victim) {
		ServerScheduler.runLater(PLAYER_DAMAGE_DELAY, () -> {
			victim.hurtServer(level, victim.damageSources().magic(), Mth.nextFloat(level.getRandom(), PLAYER_DAMAGE_MIN, PLAYER_DAMAGE_MAX));
			laugh(victim);
		});
		double dx = level.getRandom().nextBoolean() ? -MONSTER_OFFSET : MONSTER_OFFSET;
		double dz = level.getRandom().nextBoolean() ? -MONSTER_OFFSET : MONSTER_OFFSET;
		BlockPos spot = BlockPos.containing(victim.getX() + dx, victim.getY() + victim.getBbHeight() + 1.0, victim.getZ() + dz);
		var monster = BalloonRatEntities.PINK_MONSTER.spawn(level, spot, EntitySpawnReason.MOB_SUMMONED);
		if (monster != null) {
			monster.setDeltaMovement(Vec3.ZERO);
		}
		laugh(victim);
	}

	private static void jumpscareMob(ServerLevel level, LivingEntity victim) {
		victim.hurtServer(level, victim.damageSources().magic(), MOB_JUMPSCARE_DAMAGE);
		aboveHead(level, victim, BalloonRatModule.PINK_EYE, MOB_JUMPSCARE_EYES, 0.5, 0.12);
		laugh(victim);
	}

	private static void laugh(LivingEntity victim) {
		Sounds.playAt(victim, BalloonRatModule.MONSTER_LAUGH_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
	}

	private static void aboveHead(ServerLevel level, LivingEntity victim, ParticleOptions particle, int count, double spread, double speed) {
		level.sendParticles(particle, true, true, victim.getX(), victim.getY() + victim.getBbHeight(), victim.getZ(), count, spread, spread, spread, speed);
	}
}
