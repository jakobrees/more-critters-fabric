package com.morecritters.fabric.module.ghostly_wood;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Sea-soaked ghostly planks: now and then a drop of water falls from their underside. They never dry. */
public class WetGhostlyPlanksBlock extends GhostlyPlanksBlock {
	private static final int DRIP_CHANCE = 10;
	private static final double DRIP_SPREAD = 0.4;

	public WetGhostlyPlanksBlock(Properties properties) {
		super(properties);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		super.animateTick(state, level, pos, random);
		if (random.nextInt(DRIP_CHANCE) == 0) {
			level.addParticle(ParticleTypes.DRIPPING_WATER,
				pos.getX() + 0.5 + Mth.nextDouble(random, -DRIP_SPREAD, DRIP_SPREAD),
				pos.getY(),
				pos.getZ() + 0.5 + Mth.nextDouble(random, -DRIP_SPREAD, DRIP_SPREAD),
				0.0, 0.0, 0.0);
		}
	}
}
