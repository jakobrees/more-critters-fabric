package com.morecritters.fabric.core;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Arrays;

/**
 * Natural spawning. The original mod declared spawns as NeoForge biome modifiers
 * (tools/reference-data); here each module declares the same spawns in code.
 */
public final class Spawns {
	/** Adds a spawn entry to the named vanilla biomes, e.g. {@code "desert"}. */
	public static void inBiomes(EntityType<?> type, MobCategory category, int weight, int minCount, int maxCount, String... biomeNames) {
		ResourceKey<Biome>[] keys = Arrays.stream(biomeNames)
			.map(name -> ResourceKey.create(Registries.BIOME, Identifier.parse(name)))
			.<ResourceKey<Biome>>toArray(ResourceKey[]::new);
		BiomeModifications.addSpawn(BiomeSelectors.includeByKey(keys), category, type, weight, minCount, maxCount);
	}

	/** Adds a spawn entry to every biome in a tag, e.g. {@code "minecraft:is_ocean"}. */
	public static void inBiomeTag(EntityType<?> type, MobCategory category, int weight, int minCount, int maxCount, String tag) {
		TagKey<Biome> key = TagKey.create(Registries.BIOME, Identifier.parse(tag));
		BiomeModifications.addSpawn(BiomeSelectors.tag(key), category, type, weight, minCount, maxCount);
	}

	/** Land mob placed on the surface, the common case. */
	public static <T extends Mob> void onSurface(EntityType<T> type) {
		SpawnPlacements.register(type, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules);
	}

	/** Water mob that may spawn anywhere in open water: water at the spot and above it, no floor needed (deep oceans). */
	public static <T extends Mob> void inWaterColumn(EntityType<T> type) {
		SpawnPlacements.register(type, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(entityType, level, reason, pos, random) -> level.getBlockState(pos).is(Blocks.WATER) && level.getBlockState(pos.above()).is(Blocks.WATER));
	}

	/** Monster placed on the surface in the dark, the vanilla monster rules. */
	public static <T extends Monster> void monsterOnSurface(EntityType<T> type) {
		SpawnPlacements.register(type, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
	}

	public static <T extends Mob> void inWater(EntityType<T> type) {
		SpawnPlacements.register(type, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules);
	}

	private Spawns() {}
}
