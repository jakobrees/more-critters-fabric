package com.morecritters.fabric.module.ship_fittings;

import com.morecritters.fabric.ids.BombJellyIds;
import com.morecritters.fabric.ids.CorpseGearIds;
import com.morecritters.fabric.ids.ShockCubeIds;
import com.morecritters.fabric.ids.SnowflakeSpiderIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * What a cannon ball is infused with. Every cannon ball explodes where it lands; an infused one also bursts into
 * particles and puts an effect on every creature within 7.5 blocks. Effects, sounds and particles from other modules
 * are looked up by name and skipped if that module is absent.
 */
public enum Infusion {
	NONE, COLD, FIRE, SLIME, ELECTRIC, COMBUSTING;

	private static final double BURST_RADIUS = 7.5;

	public static @Nullable Infusion ofAmmo(Item item) {
		for (Infusion infusion : values()) {
			if (ShipFittingsModule.CANNON_BALLS.get(infusion) == item) return infusion;
		}
		return null;
	}

	/** The particles a flying ball of this kind trails. */
	ParticleOptions trail() {
		return this == SLIME ? new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SLIME_BLOCK.defaultBlockState()) : ParticleTypes.SMOKE;
	}

	/** The infusion's burst where the ball hit: particles, then the effect on everything nearby, then the sound. */
	void burst(ServerLevel level, Vec3 at) {
		switch (this) {
			case NONE -> {}
			case COLD -> {
				level.sendParticles(ParticleTypes.SNOWFLAKE, true, false, at.x, at.y, at.z, 10, 2.0, 2.0, 2.0, 0.01);
				affectNearby(level, at, SnowflakeSpiderIds.Effects.FROSTBITE, 400, 0, true);
				playSound(level, at, CorpseGearIds.Sounds.ENTITY_CANNON_BALL_COLD_HIT);
			}
			case FIRE -> {
				level.sendParticles(ParticleTypes.FLAME, true, false, at.x, at.y, at.z, 10, 2.0, 2.0, 2.0, 0.01);
				for (LivingEntity creature : nearby(level, at)) creature.igniteForSeconds(10.0F);
				playSound(level, at, CorpseGearIds.Sounds.ENTITY_CANNON_BALL_FIRE_HIT);
			}
			case SLIME -> {
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SLIME_BLOCK.defaultBlockState()), true, false,
					at.x, at.y, at.z, 10, 2.0, 2.0, 2.0, 0.01);
				for (LivingEntity creature : nearby(level, at)) {
					creature.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 3, false, true));
				}
				playSound(level, at, CorpseGearIds.Sounds.ENTITY_CANNON_BALL_SLIME_HIT);
			}
			case ELECTRIC -> {
				if (BuiltInRegistries.PARTICLE_TYPE.getValue(ShockCubeIds.Particles.ZAP) instanceof ParticleOptions zap) {
					level.sendParticles(zap, true, false, at.x, at.y, at.z, 10, 2.0, 2.0, 2.0, 0.01);
				}
				affectNearby(level, at, ShockCubeIds.Effects.ELECTROCUTED, 2000, 0, false);
				playSound(level, at, CorpseGearIds.Sounds.ENTITY_CANNON_BALL_FIRE_HIT);
			}
			case COMBUSTING -> {
				Item jelly = BuiltInRegistries.ITEM.getValue(BombJellyIds.Items.EXPLOSIVE_JELLY);
				if (jelly != Items.AIR) {
					level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, jelly), at.x, at.y, at.z, 35, 2.0, 2.0, 2.0, 0.0);
				}
				affectNearby(level, at, BombJellyIds.Effects.COMBUSTION, 60, 0, false);
				playSound(level, at, CorpseGearIds.Sounds.ENTITY_CANNON_BALL_FIRE_HIT);
			}
		}
	}

	private static Iterable<LivingEntity> nearby(ServerLevel level, Vec3 at) {
		return level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(BURST_RADIUS));
	}

	private static void affectNearby(ServerLevel level, Vec3 at, Identifier effectId, int ticks, int amplifier, boolean visible) {
		Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.get(effectId).<Holder<MobEffect>>map(holder -> holder).orElse(null);
		if (effect == null) return;
		for (LivingEntity creature : nearby(level, at)) {
			creature.addEffect(new MobEffectInstance(effect, ticks, amplifier, false, visible));
		}
	}

	private static void playSound(ServerLevel level, Vec3 at, Identifier soundId) {
		var sound = BuiltInRegistries.SOUND_EVENT.getValue(soundId);
		if (sound != null) level.playSound(null, BlockPos.containing(at), sound, SoundSource.BLOCKS, 2.0F, 1.0F);
	}
}
