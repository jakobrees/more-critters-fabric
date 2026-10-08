package com.morecritters.fabric.module.fossils;

import com.morecritters.fabric.core.Drops;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Fossil ore (stone and deepslate). Its loot table is empty: a player breaking it with silk
 * touch gets the block back, otherwise one time in three a random one of the seventeen fossils.
 */
public class FossilBlock extends Block {
	private static final int FOSSIL_CHANCE = 3;

	public FossilBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (level instanceof ServerLevel serverLevel) {
			dropFor(serverLevel, pos, state, player);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	private void dropFor(ServerLevel level, BlockPos pos, BlockState state, Player player) {
		Vec3 centre = Vec3.atCenterOf(pos);
		var silkTouch = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
		if (EnchantmentHelper.getItemEnchantmentLevel(silkTouch, player.getMainHandItem()) != 0) {
			Drops.dropSingles(level, centre, this, 1);
			return;
		}
		if (player.hasInfiniteMaterials()) return;
		// The original plays the break effect (particles and sound) twice more.
		for (int i = 0; i < 2; i++) {
			level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, pos, Block.getId(state));
		}
		if (level.getRandom().nextInt(FOSSIL_CHANCE) == 0) {
			var fossil = Fossils.ALL_FOSSILS.get(level.getRandom().nextInt(Fossils.ALL_FOSSILS.size()));
			Drops.dropSingles(level, centre, Fossils.item(fossil), 1);
		}
	}
}
