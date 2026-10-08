package com.morecritters.fabric.module.creeblossom;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.morecritters.fabric.core.Drops;

/** A blossombush in a flower pot. An empty hand takes the bush back out; breaking it also gives back the pot. */
public class PottedBlossombushBlock extends Block {
	private static final VoxelShape SHAPE = box(5.0, 0.0, 5.0, 11.0, 6.0, 11.0);
	private final Supplier<Block> plant;

	public PottedBlossombushBlock(Properties properties, Supplier<Block> plant) {
		super(properties);
		this.plant = plant;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return true;
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(plant.get());
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player.getMainHandItem().isEmpty()) {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(plant.get()));
			player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
			level.setBlock(pos, Blocks.FLOWER_POT.defaultBlockState(), Block.UPDATE_ALL);
		}
		return InteractionResult.SUCCESS;
	}

	/** The loot table drops the bush; the pot comes back here, as in the original. */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (level instanceof ServerLevel server && !player.hasInfiniteMaterials()) {
			Drops.dropSingles(server, Vec3.atCenterOf(pos), Blocks.FLOWER_POT, 1);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** Blown up, it throws out an extra bush on top of the loot table's. */
	@Override
	public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
		super.wasExploded(level, pos, explosion);
		Drops.dropSingles(level, Vec3.atCenterOf(pos), plant.get(), 1);
	}
}
