package com.morecritters.fabric.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morecritters.fabric.MoreCritters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * A worldgen feature that stamps a saved structure (an {@code .nbt} template) at the
 * placement position; the original mod's {@code more_critters:structure_feature}, used by
 * its features for bushes, trees, fossils and the like.
 */
public record StructureFeature(Identifier structure, boolean randomRotation, boolean randomMirror, HolderSet<Block> ignoredBlocks, Vec3i offset) implements Feature {
	public static final MapCodec<StructureFeature> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
		Identifier.CODEC.fieldOf("structure").forGetter(StructureFeature::structure),
		Codec.BOOL.fieldOf("random_rotation").orElse(false).forGetter(StructureFeature::randomRotation),
		Codec.BOOL.fieldOf("random_mirror").orElse(false).forGetter(StructureFeature::randomMirror),
		RegistryCodecs.holderSet(Registries.BLOCK).fieldOf("ignored_blocks").forGetter(StructureFeature::ignoredBlocks),
		Vec3i.offsetCodec(48).optionalFieldOf("offset", Vec3i.ZERO).forGetter(StructureFeature::offset)
	).apply(builder, StructureFeature::new));

	public static void register() {
		Registry.register(BuiltInRegistries.FEATURE_TYPE, MoreCritters.id("structure_feature"), CODEC);
	}

	@Override
	public MapCodec<StructureFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		Rotation rotation = randomRotation ? Rotation.getRandom(random) : Rotation.NONE;
		Mirror mirror = randomMirror ? Mirror.values()[random.nextInt(2)] : Mirror.NONE;
		BlockPos at = origin.offset(offset);
		StructureTemplate template = level.getLevel().getStructureTemplateManager().getOrCreate(structure);
		StructurePlaceSettings settings = new StructurePlaceSettings()
			.setRotation(rotation)
			.setMirror(mirror)
			.setRandom(random)
			.setIgnoreEntities(false)
			.addProcessor(new BlockIgnoreProcessor(ignoredBlocks.stream().map(Holder::value).toList()));
		template.placeInWorld(level, at, at, settings, random, 4);
		return true;
	}
}
