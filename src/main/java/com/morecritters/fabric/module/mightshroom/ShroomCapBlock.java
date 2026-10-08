package com.morecritters.fabric.module.mightshroom;

import com.morecritters.fabric.core.Drops;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The cap block of a huge vita or mori shroom. Broken with silk touch it drops itself; otherwise zero to two
 * small shrooms. (Its loot table is empty; the drops come from here, as in the original.)
 */
public class ShroomCapBlock extends Block {
	private final Supplier<Block> shroom;

	public ShroomCapBlock(Properties properties, Supplier<Block> shroom) {
		super(properties);
		this.shroom = shroom;
	}

	@Override
	public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
		super.playerDestroy(level, player, pos, state, blockEntity, tool);
		var silkTouch = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
		Vec3 centre = Vec3.atCenterOf(pos);
		if (EnchantmentHelper.getItemEnchantmentLevel(silkTouch, tool) != 0) {
			Drops.dropSingles(level, centre, this, 1);
		} else {
			Drops.dropSingles(level, centre, shroom.get(), (int) (level.getRandom().nextDouble() * 3.0));
		}
	}
}
