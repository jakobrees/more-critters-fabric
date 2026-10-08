package com.morecritters.fabric.module.mightshroom;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

/**
 * A vita or mori shroom: a small flower-like mushroom that grows on dirt, but never beside a potted shroom of
 * its own kind. Bone meal on one with nothing around it (the eight blocks beside it and the one above) has a
 * one in five chance to grow a huge shroom from a structure.
 */
public class ShroomBlock extends FlowerBlock implements BonemealableBlock {
	private static final float STEW_SECONDS = 5.0F;
	private static final int GROW_CHANCE = 5;

	private final Supplier<Block> pot;
	private final Identifier hugeShroom;

	public ShroomBlock(Properties properties, Supplier<Block> pot, Identifier hugeShroom) {
		super(MobEffects.SPEED, STEW_SECONDS, properties);
		this.pot = pot;
		this.hugeShroom = hugeShroom;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		// The original's minecraft:dirt tag (1.20.1) also held grass, podzol, mycelium, moss and mud; in 26.3 those
		// moved to their own tags, which substrate_overworld gathers again.
		if (!level.getBlockState(pos.below()).is(BlockTags.SUBSTRATE_OVERWORLD)) {
			return false;
		}
		for (Direction side : Direction.Plane.HORIZONTAL) {
			if (level.getBlockState(pos.relative(side)).is(pot.get())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
		return true;
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if ((dx != 0 || dz != 0) && !level.isEmptyBlock(pos.offset(dx, 0, dz))) {
					return false;
				}
			}
		}
		return level.isEmptyBlock(pos.above());
	}

	/** The huge shroom is a five-by-five structure centred on this block. */
	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		if (random.nextInt(GROW_CHANCE) != 0) {
			return;
		}
		level.getStructureTemplateManager().get(hugeShroom).ifPresent(template -> {
			BlockPos corner = pos.offset(-2, 0, -2);
			StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false);
			template.placeInWorld(level, corner, corner, settings, level.getRandom(), Block.UPDATE_ALL);
		});
	}
}
