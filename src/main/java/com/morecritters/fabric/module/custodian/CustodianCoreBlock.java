package com.morecritters.fabric.module.custodian;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import com.morecritters.fabric.core.Animations;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The heart of a home-built custodian. Placed in the middle of a 3x3x3 frame (deepslate bricks on
 * the faces and above and below, bone blocks on the eight vertical edges) the whole frame
 * crumbles and a custodian stands up in its place.
 */
public class CustodianCoreBlock extends Block {
	private static final double ADVANCEMENT_RADIUS = 12.5;

	public CustodianCoreBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		level.scheduleTick(pos, this, 1);
	}

	/** Checks the frame every tick, like the original. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (isFrameComplete(level, pos)) {
			buildCustodian(level, pos);
		} else {
			level.scheduleTick(pos, this, 1);
		}
	}

	private static boolean isFrameComplete(ServerLevel level, BlockPos core) {
		if (!level.getBlockState(core.above()).is(Blocks.DEEPSLATE_BRICKS) || !level.getBlockState(core.below()).is(Blocks.DEEPSLATE_BRICKS)) {
			return false;
		}
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dz == 0) {
						continue;
					}
					Block expected = dx != 0 && dz != 0 ? Blocks.BONE_BLOCK : Blocks.DEEPSLATE_BRICKS;
					if (!level.getBlockState(core.offset(dx, dy, dz)).is(expected)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private static void buildCustodian(ServerLevel level, BlockPos core) {
		for (BlockPos pos : BlockPos.betweenClosed(core.offset(-1, -1, -1), core.offset(1, 1, 1))) {
			level.destroyBlock(pos, false);
		}
		CustodianEntity custodian = CustodianModule.CUSTODIAN.spawn(level, core, EntitySpawnReason.MOB_SUMMONED);
		level.playSound(null, core, CustodianModule.OPEN_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
		for (Player player : level.getEntitiesOfClass(Player.class, new AABB(core).inflate(ADVANCEMENT_RADIUS))) {
			Advancements.award(player, MoreCritters.id("create_custodian"));
		}
		if (custodian != null) {
			custodian.triggerAnim(Animations.ACTIONS, "openup");
		}
	}
}
