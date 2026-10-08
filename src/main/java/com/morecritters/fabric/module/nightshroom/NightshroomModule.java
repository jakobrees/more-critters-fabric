package com.morecritters.fabric.module.nightshroom;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.ids.NightshroomIds;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * The dark side of the mushroom family: the tame nightshroom and the hostile frightshroom that an
 * ancient skeleton becomes after a stew and a purgatorial mixture, the frightshroom's rot (rot
 * pieces, rot splashes and the rot zombies they raise), fungal zombies, the mori roots, and their
 * spawn dolls and drops.
 */
public final class NightshroomModule implements Module {
	public static EntityType<NightshroomEntity> NIGHTSHROOM;
	public static EntityType<FrightshroomEntity> FRIGHTSHROOM;
	public static EntityType<FungalZombieEntity> FUNGAL_ZOMBIE;
	public static EntityType<RotZombieEntity> ROT_ZOMBIE;
	public static EntityType<RotSplashEntity> ROT_SPLASH;
	public static EntityType<RotPieceEntity> ROT_PIECE;
	public static EntityType<MoriRootsEntity> MORI_ROOTS;
	public static EntityType<AncientSkeletonEntity> ANCIENT_SKELETON;

	public static SimpleParticleType ROT_PARTICLE;
	public static Holder<MobEffect> ROT_COVERED;
	public static Holder<MobEffect> SPAWN_NIGHTSHROOM;

	static SoundEvent FRIGHTSHROOM_ATTACK_SOUND, FRIGHTSHROOM_BURST_SOUND, FRIGHTSHROOM_DEATH_SOUND, FRIGHTSHROOM_HURT_SOUND, FRIGHTSHROOM_IDLE_SOUND;
	static SoundEvent FUNGAL_ZOMBIE_DEATH_SOUND, FUNGAL_ZOMBIE_HURT_SOUND, FUNGAL_ZOMBIE_IDLE_SOUND, FUNGAL_ZOMBIE_TRANSFORM_SOUND;
	static SoundEvent MORI_ROOTS_RELEASE_SOUND;
	static SoundEvent NIGHTSHROOM_ATTACK_SOUND, NIGHTSHROOM_DEATH_SOUND, NIGHTSHROOM_HURT_SOUND, NIGHTSHROOM_IDLE_SOUND, NIGHTSHROOM_RUFFLE_SOUND;
	static SoundEvent ROT_SPLASH_END_SOUND, ROT_SPLASH_IDLE_SOUND, ROT_SPLASH_START_SOUND;
	static SoundEvent ROT_ZOMBIE_DEATH_SOUND, ROT_ZOMBIE_HURT_SOUND, ROT_ZOMBIE_IDLE_SOUND, ROT_ZOMBIE_SPAWN_SOUND;

	@Override
	public void register() {
		registerSounds();
		registerEntities();
		NightshroomItems.register();

		ROT_PARTICLE = Particles.simple(NightshroomIds.Particles.ROT, false);
		ROT_COVERED = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, NightshroomIds.Effects.ROT_COVERED, new RotCoveredEffect());
		SPAWN_NIGHTSHROOM = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, NightshroomIds.Effects.SPAWN_NIGHTSHROOM, new SpawnNightshroomEffect());

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) -> {
			ShroomAttacks.onDamage(victim, source);
			return true;
		});
	}

	private static void registerEntities() {
		// The original sizes the two shrooms 1.6 x 5 and scales them by 0.8 and 0.9.
		NIGHTSHROOM = Registration.livingEntity(NightshroomIds.Entities.NIGHTSHROOM,
			EntityType.Builder.of(NightshroomEntity::new, MobCategory.MONSTER).sized(1.6F * 0.8F, 5.0F * 0.8F).clientTrackingRange(8).updateInterval(3),
			NightshroomEntity.createAttributes());
		FRIGHTSHROOM = Registration.livingEntity(NightshroomIds.Entities.FRIGHTSHROOM,
			EntityType.Builder.of(FrightshroomEntity::new, MobCategory.MONSTER).sized(1.6F * 0.9F, 5.0F * 0.9F).clientTrackingRange(8).updateInterval(3),
			FrightshroomEntity.createAttributes());
		FUNGAL_ZOMBIE = Registration.livingEntity(NightshroomIds.Entities.FUNGAL_ZOMBIE,
			EntityType.Builder.of(FungalZombieEntity::new, MobCategory.MONSTER).sized(0.6F, 1.8F).clientTrackingRange(8).updateInterval(3),
			FungalZombieEntity.createAttributes());
		ROT_ZOMBIE = Registration.livingEntity(NightshroomIds.Entities.ROT_ZOMBIE,
			EntityType.Builder.of(RotZombieEntity::new, MobCategory.MONSTER).sized(0.6F, 1.6F).clientTrackingRange(8).updateInterval(3),
			RotZombieEntity.createAttributes());
		ROT_SPLASH = Registration.livingEntity(NightshroomIds.Entities.ROT_SPLASH,
			EntityType.Builder.of(RotSplashEntity::new, MobCategory.MONSTER).sized(0.3F, 0.5F).fireImmune().clientTrackingRange(8).updateInterval(3),
			RotSplashEntity.createAttributes());
		ROT_PIECE = Registration.entity(NightshroomIds.Entities.ROT_PIECE,
			EntityType.Builder.<RotPieceEntity>of(RotPieceEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(1));
		MORI_ROOTS = Registration.livingEntity(NightshroomIds.Entities.MORI_ROOTS,
			EntityType.Builder.of(MoriRootsEntity::new, MobCategory.MONSTER).sized(0.5F, 0.7F).fireImmune().clientTrackingRange(8).updateInterval(3),
			MoriRootsEntity.createAttributes());
		ANCIENT_SKELETON = Registration.livingEntity(NightshroomIds.Entities.ANCIENT_SKELETON,
			EntityType.Builder.of(AncientSkeletonEntity::new, MobCategory.MONSTER).sized(2.0F, 1.0F).fireImmune().clientTrackingRange(8).updateInterval(3),
			AncientSkeletonEntity.createAttributes());
	}

	private static void registerSounds() {
		// Played by the mightshroom module's raising ritual.
		Registration.sound(NightshroomIds.Sounds.ENTITY_ANCIENT_SKELETON_POUR_SOUP);
		Registration.sound(NightshroomIds.Sounds.ENTITY_ANCIENT_SKELETON_RISE);
		FRIGHTSHROOM_ATTACK_SOUND = Registration.sound(NightshroomIds.Sounds.ENTITY_FRIGHTSHROOM_ATTACK);
		FRIGHTSHROOM_BURST_SOUND = Registration.sound(NightshroomIds.Sounds.ENTITY_FRIGHTSHROOM_BURST);
		FRIGHTSHROOM_DEATH_SOUND = Registration.sound(NightshroomIds.Sounds.ENTITY_FRIGHTSHROOM_DEATH);
		FRIGHTSHROOM_HURT_SOUND = Registration.sound(NightshroomIds.Sounds.ENTITY_FRIGHTSHROOM_HURT);
		FRIGHTSHROOM_IDLE_SOUND = Registration.sound(NightshroomIds.Sounds.ENTITY_FRIGHTSHROOM_IDLE);
		// Registered as in the original, which never plays it.
		Registration.sound(NightshroomIds.Sounds.ENTITY_FRIGHTSHROOM_SPIT);
		FUNGAL_ZOMBIE_DEATH_SOUND = Registration.sound(NightshroomIds.Sounds.ENTITY_FUNGAL_ZOMBIE_DEATH);
		FUNGAL_ZOMBIE_HURT_SOUND = Registration.sound(NightshroomIds.Sounds.ENTITY_FUNGAL_ZOMBIE_HURT);
		FUNGAL_ZOMBIE_IDLE_SOUND = Registration.sound(NightshroomIds.Sounds.ENTITY_FUNGAL_ZOMBIE_IDLE);
		FUNGAL_ZOMBIE_TRANSFORM_SOUND = Registration.sound(NightshroomIds.Sounds.ENTITY_FUNGAL_ZOMBIE_TRANSFORM);
		// The bite is played by the mightshroom module's fungal staff when it summons the roots.
		Registration.sound(NightshroomIds.Sounds.ENTITY_MORI_ROOTS_BITE);
		MORI_ROOTS_RELEASE_SOUND = Registration.sound(NightshroomIds.Sounds.ENTITY_MORI_ROOTS_RELEASE);
		NIGHTSHROOM_ATTACK_SOUND = Registration.sound(NightshroomIds.Sounds.ENTITY_NIGHTSHROOM_ATTACK);
		NIGHTSHROOM_DEATH_SOUND = Registration.sound(NightshroomIds.Sounds.ENTITY_NIGHTSHROOM_DEATH);
		NIGHTSHROOM_HURT_SOUND = Registration.sound(NightshroomIds.Sounds.ENTITY_NIGHTSHROOM_HURT);
		NIGHTSHROOM_IDLE_SOUND = Registration.sound(NightshroomIds.Sounds.ENTITY_NIGHTSHROOM_IDLE);
		NIGHTSHROOM_RUFFLE_SOUND = Registration.sound(NightshroomIds.Sounds.ENTITY_NIGHTSHROOM_RUFFLE);
		ROT_SPLASH_END_SOUND = Registration.sound(NightshroomIds.Sounds.ENTITY_ROT_SPLASH_END);
		ROT_SPLASH_IDLE_SOUND = Registration.sound(NightshroomIds.Sounds.ENTITY_ROT_SPLASH_IDLE);
		ROT_SPLASH_START_SOUND = Registration.sound(NightshroomIds.Sounds.ENTITY_ROT_SPLASH_START);
		ROT_ZOMBIE_DEATH_SOUND = Registration.sound(NightshroomIds.Sounds.ENTITY_ROT_ZOMBIE_DEATH);
		ROT_ZOMBIE_HURT_SOUND = Registration.sound(NightshroomIds.Sounds.ENTITY_ROT_ZOMBIE_HURT);
		ROT_ZOMBIE_IDLE_SOUND = Registration.sound(NightshroomIds.Sounds.ENTITY_ROT_ZOMBIE_IDLE);
		ROT_ZOMBIE_SPAWN_SOUND = Registration.sound(NightshroomIds.Sounds.ENTITY_ROT_ZOMBIE_SPAWN);
	}
}
