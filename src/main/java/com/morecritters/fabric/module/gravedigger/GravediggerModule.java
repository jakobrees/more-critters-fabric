package com.morecritters.fabric.module.gravedigger;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.GravediggerIds;
import java.util.Set;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;

/** The gravedigger, the Amalgam it digs up, its pickled jar, and the Reaper music disc. */
public final class GravediggerModule implements Module {
	public static EntityType<GravediggerEntity> GRAVEDIGGER;
	public static EntityType<AmalgamEntity> AMALGAM;
	public static GravediggerJarBlock JAR;
	public static BlockEntityType<GravediggerJarBlockEntity> JAR_BLOCK_ENTITY;
	public static Item JAR_ITEM;
	public static SimpleParticleType REAPER_PARTICLE, MAD_REAPER_PARTICLE;
	static Item APPENDAGE;

	static SoundEvent SNIFF_SOUND, GROWL_SOUND, HURT_SOUND, DEATH_SOUND, DIG_DOWN_SOUND, DIG_UP_SOUND;
	static SoundEvent AMALGAM_IDLE_SOUND, AMALGAM_HURT_SOUND, AMALGAM_DEATH_SOUND;

	@Override
	public void register() {
		registerSounds();

		GRAVEDIGGER = Registration.livingEntity(GravediggerIds.Entities.GRAVEDIGGER,
			EntityType.Builder.of(GravediggerEntity::new, MobCategory.MONSTER).sized(0.8F, 0.6F).clientTrackingRange(8).updateInterval(3),
			GravediggerEntity.createAttributes());
		AMALGAM = Registration.livingEntity(GravediggerIds.Entities.AMALGAM,
			EntityType.Builder.of(AmalgamEntity::new, MobCategory.MONSTER).sized(1.0F, 2.4F).clientTrackingRange(8).updateInterval(3),
			AmalgamEntity.createAttributes());

		Registration.spawnEgg(GravediggerIds.Items.GRAVEDIGGER_SPAWN_EGG, GRAVEDIGGER);
		APPENDAGE = Registration.item(GravediggerIds.Items.GRAVEDIGGER_APPENDAGE);
		Registration.item(GravediggerIds.Items.AMALGAM_SPAWN_DOLL, AmalgamSpawnDollItem::new,
			Tooltips.describe(new Item.Properties(), "amalgam_spawn_doll", 3));
		Registration.item(GravediggerIds.Items.MUSIC_DISC_REAPER, new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
			.jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG, GravediggerIds.Items.MUSIC_DISC_REAPER)));

		JAR = Registration.block(GravediggerIds.Blocks.GRAVEDIGGER_JAR, GravediggerJarBlock::new,
			BlockBehaviour.Properties.of().sound(SoundType.GLASS).strength(1.0F).noOcclusion().isRedstoneConductor((state, level, pos) -> false));
		// The item shares the block's name and is drawn by GeckoLib (see GeoItems).
		JAR_ITEM = Registration.item(GravediggerIds.Blocks.GRAVEDIGGER_JAR, p -> new GravediggerJarItem(JAR, p), new Item.Properties().useBlockDescriptionPrefix());
		JAR_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, GravediggerIds.BlockEntities.GRAVEDIGGER_JAR,
			new BlockEntityType<>(GravediggerJarBlockEntity::new, Set.of(JAR)));

		REAPER_PARTICLE = Particles.simple(GravediggerIds.Particles.REAPER, true);
		MAD_REAPER_PARTICLE = Particles.simple(GravediggerIds.Particles.MAD_REAPER, false);

		// Any biome; the placement rule keeps it deep underground in the Overworld.
		SpawnPlacements.register(GRAVEDIGGER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, GravediggerEntity::canSpawnAt);
		BiomeModifications.addSpawn(BiomeSelectors.all(), MobCategory.MONSTER, GRAVEDIGGER, 10, 1, 1);
	}

	private static void registerSounds() {
		Registration.sound(GravediggerIds.Sounds.MUSIC_DISC_REAPER);
		// The reaper sounds are owned here but played by nothing in this module.
		Registration.sound(GravediggerIds.Sounds.AMBIENT_REAPER_READY);
		Registration.sound(GravediggerIds.Sounds.AMBIENT_REAPER_HIT);
		Registration.sound(GravediggerIds.Sounds.AMBIENT_REAPER_RAGE);
		SNIFF_SOUND = Registration.sound(GravediggerIds.Sounds.ENTITY_GRAVEDIGGER_SNIFF);
		GROWL_SOUND = Registration.sound(GravediggerIds.Sounds.ENTITY_GRAVEDIGGER_GROWL);
		HURT_SOUND = Registration.sound(GravediggerIds.Sounds.ENTITY_GRAVEDIGGER_HURT);
		DEATH_SOUND = Registration.sound(GravediggerIds.Sounds.ENTITY_GRAVEDIGGER_DEATH);
		DIG_DOWN_SOUND = Registration.sound(GravediggerIds.Sounds.ENTITY_GRAVEDIGGER_DIG_DOWN);
		DIG_UP_SOUND = Registration.sound(GravediggerIds.Sounds.ENTITY_GRAVEDIGGER_DIG_UP);
		AMALGAM_IDLE_SOUND = Registration.sound(GravediggerIds.Sounds.ENTITY_AMALGAM_IDLE);
		AMALGAM_HURT_SOUND = Registration.sound(GravediggerIds.Sounds.ENTITY_AMALGAM_HURT);
		AMALGAM_DEATH_SOUND = Registration.sound(GravediggerIds.Sounds.ENTITY_AMALGAM_DEATH);
		// Registered as in the original, which never plays it.
		Registration.sound(GravediggerIds.Sounds.ENTITY_AMALGAM_ATTACK);
	}
}
