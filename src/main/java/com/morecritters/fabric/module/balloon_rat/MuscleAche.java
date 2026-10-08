package com.morecritters.fabric.module.balloon_rat;

import com.morecritters.fabric.ids.BalloonRatIds;
import com.morecritters.fabric.ids.MiscIds;
import com.morecritters.fabric.ids.SnowflakeSpiderIds;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * The pains of muscle ache. Jumping, sprinting (now and then), hitting something, punching a block
 * or starting to use an item makes the sufferer's bones crack: a hurt sound, two to three points of
 * damage and a spray of bone debris. Balloon rats and creative players feel nothing.
 */
final class MuscleAche {
	private static final float DAMAGE_MIN = 2.0F, DAMAGE_MAX = 3.0F;
	private static final int DEBRIS_MIN = 3, DEBRIS_MAX = 6;
	private static final double DEBRIS_SPEED = 0.05;
	/** One sprinting tick in this many hurts. */
	private static final int SPRINT_PAIN_CHANCE = 5;

	/** Whether the sufferer stood on the ground last tick, to notice it jumping. Not saved. */
	private static final AttachmentType<Boolean> WAS_ON_GROUND =
		AttachmentRegistry.create(BalloonRatIds.Effects.MUSCLE_ACHE.withSuffix("_was_on_ground"));

	static void registerEvents() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) -> {
			if (source.getEntity() instanceof LivingEntity attacker && !BalloonRatEffects.isBalloonRat(victim) && suffers(attacker)) {
				strain(attacker);
			}
			return true;
		});
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
			if (!level.isClientSide() && suffers(player)) {
				strain(player);
			}
			return InteractionResult.PASS;
		});
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (!level.isClientSide() && player.getItemInHand(hand).getUseDuration(player) > 0 && suffers(player)) {
				strain(player);
			}
			return InteractionResult.PASS;
		});
	}

	/** Every tick of the effect: jumping always hurts, sprinting sometimes. */
	static void tick(ServerLevel level, LivingEntity sufferer) {
		boolean jumped = sufferer.getAttachedOrElse(WAS_ON_GROUND, false) && !sufferer.onGround() && sufferer.getDeltaMovement().y > 0;
		sufferer.setAttached(WAS_ON_GROUND, sufferer.onGround());
		if (!suffers(sufferer)) {
			return;
		}
		if (jumped || sufferer.isSprinting() && level.getRandom().nextInt(SPRINT_PAIN_CHANCE) == 0) {
			strain(sufferer);
		}
	}

	private static boolean suffers(LivingEntity entity) {
		return entity.hasEffect(BalloonRatEffects.MUSCLE_ACHE)
			&& !BalloonRatEffects.isBalloonRat(entity)
			&& !(entity instanceof Player player && player.hasInfiniteMaterials());
	}

	private static void strain(LivingEntity sufferer) {
		if (!(sufferer.level() instanceof ServerLevel level)) {
			return;
		}
		if (BuiltInRegistries.SOUND_EVENT.getValue(SnowflakeSpiderIds.Sounds.ENTITY_EXTENSIVE_HURT) instanceof SoundEvent crack) {
			level.playSound(null, sufferer.blockPosition(), crack, SoundSource.PLAYERS, 1.0F, 1.0F);
		}
		sufferer.hurtServer(level, sufferer.damageSources().generic(), Mth.nextFloat(level.getRandom(), DAMAGE_MIN, DAMAGE_MAX));
		if (BuiltInRegistries.PARTICLE_TYPE.getValue(MiscIds.Particles.BONE_DEBRIS) instanceof ParticleOptions debris) {
			int count = Mth.nextInt(level.getRandom(), DEBRIS_MIN, DEBRIS_MAX);
			level.sendParticles(debris, true, true, sufferer.getX(), sufferer.getY() + sufferer.getBbHeight(), sufferer.getZ(), count, 0, 0, 0, DEBRIS_SPEED);
		}
	}

	private MuscleAche() {}
}
