package com.morecritters.fabric.module.nauticrawl;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Ramen served in a nauticrawl shell, eaten off the block in three goes: the noodles
 * (ten hunger), then the broth (ten more), then the shell itself is crunched up for
 * fifteen seconds of water breathing and the block is gone.
 */
public class NauticrawlRamenBlock extends NauticrawlShellBlock {
	/** 0 full bowl, 2 broth left, 3 empty shell. Stage 1 is in the blockstate file but never reached. */
	public static final IntegerProperty STAGE = IntegerProperty.create("blockstate", 0, 3);
	private static final int FULL = 0, BROTH = 2, SHELL = 3;

	private static final int HUNGER_PER_SERVING = 10;
	private static final int WATER_BREATHING_TICKS = 300;

	public NauticrawlRamenBlock(Properties properties) {
		super(properties, Block.box(2.5, 0.0, 2.5, 13.5, 13.0, 13.5));
		this.registerDefaultState(this.defaultBlockState().setValue(STAGE, FULL));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(STAGE);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		switch (state.getValue(STAGE)) {
			case FULL -> serve(level, pos, state.setValue(STAGE, BROTH), player, NauticrawlModule.RAMEN_EAT_SOUND);
			case BROTH -> serve(level, pos, state.setValue(STAGE, SHELL), player, NauticrawlModule.RAMEN_DRINK_SOUND);
			case SHELL -> crunchShell(level, pos, player);
			default -> { }
		}
		return InteractionResult.SUCCESS;
	}

	private static void serve(Level level, BlockPos pos, BlockState next, Player player, SoundEvent sound) {
		level.setBlock(pos, next, Block.UPDATE_ALL);
		player.getFoodData().eat(HUNGER_PER_SERVING, 0.0F);
		level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	private static void crunchShell(Level level, BlockPos pos, Player player) {
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, WATER_BREATHING_TICKS, 0, false, true));
		level.playSound(null, pos, NauticrawlModule.RAMEN_CRUNCH_SOUND, SoundSource.BLOCKS, 1.0F, 1.0F);
	}
}
