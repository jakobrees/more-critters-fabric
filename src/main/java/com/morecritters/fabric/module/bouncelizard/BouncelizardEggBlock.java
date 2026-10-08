package com.morecritters.fabric.module.bouncelizard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A clutch of bouncelizard eggs. The state counts the eggs: 0 is one egg, 2 to 4 are that many (1 is unused, as in the
 * original). Adding an egg item grows the clutch, breaking it takes one egg off (dropped only with Silk Touch).
 * After about a day the clutch hatches into that many babies.
 */
public class BouncelizardEggBlock extends Block implements EntityBlock {
	public static final IntegerProperty EGGS = IntegerProperty.create("blockstate", 0, 4);
	private static final VoxelShape FIRST = box(9.25, 0.0, 9.5, 15.25, 4.0, 15.5);
	private static final VoxelShape SECOND = box(1.0, 0.0, 9.0, 7.0, 3.0, 15.0);
	private static final VoxelShape THIRD = box(1.0, 0.0, 0.5, 7.0, 4.0, 6.5);
	private static final VoxelShape FOURTH = box(8.0, 0.0, 2.0, 14.0, 3.0, 8.0);

	public BouncelizardEggBlock(Properties properties) {
		super(properties);
		registerDefaultState(this.stateDefinition.any().setValue(EGGS, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(EGGS);
	}

	private static int eggCount(BlockState state) {
		return Math.max(1, state.getValue(EGGS));
	}

	/** The state for one egg more (or less); one egg is state 0. */
	private static int stateFor(int eggs) {
		return eggs <= 1 ? 0 : eggs;
	}

	@Override
	protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (eggCount(state)) {
			case 2 -> Shapes.or(SECOND, FIRST);
			case 3 -> Shapes.or(SECOND, THIRD, FIRST);
			case 4 -> Shapes.or(FOURTH, SECOND, THIRD, FIRST);
			default -> FIRST;
		};
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BouncelizardEggBlockEntity(pos, state);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		level.scheduleTick(pos, this, 1);
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BouncelizardEggBlockEntity eggs) eggs.startCountdown();
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof BouncelizardEggBlockEntity eggs && eggs.tickReady()) {
			hatch(state, level, pos);
			return;
		}
		level.scheduleTick(pos, this, 1);
	}

	private void hatch(BlockState state, ServerLevel level, BlockPos pos) {
		level.playSound(null, pos, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS, 1.0F, 1.0F);
		level.levelEvent(2001, pos, Block.getId(state));
		level.removeBlock(pos, false);
		for (int i = 0; i < eggCount(state); i++) {
			BouncelizardEntity baby = BouncelizardModule.BOUNCELIZARD.create(level, EntitySpawnReason.BREEDING);
			if (baby == null) continue;
			baby.setAge(-24_000);
			baby.markHatched();
			baby.snapTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.0F, 0.0F);
			level.addFreshEntity(baby);
		}
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!stack.is(asItem()) || eggCount(state) >= 4) return super.useItemOn(stack, state, level, pos, player, hand, hit);
		stack.consume(1, player);
		player.swing(hand, SwingAnimation.DEFAULT, true);
		level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
		level.setBlock(pos, state.setValue(EGGS, eggCount(state) + 1), 3);
		return InteractionResult.SUCCESS;
	}

	/** Breaking a clutch takes one egg; the rest stay (and start their countdown over, as in the original). */
	@Override
	public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
		super.playerDestroy(level, player, pos, state, blockEntity, tool);
		if (player.hasInfiniteMaterials()) return;
		if (eggCount(state) > 1) level.setBlock(pos, state.setValue(EGGS, stateFor(eggCount(state) - 1)), 3);
		var silkTouch = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
		if (EnchantmentHelper.getItemEnchantmentLevel(silkTouch, tool) > 0) {
			ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(this));
			drop.setPickUpDelay(10);
			level.addFreshEntity(drop);
		}
	}
}
