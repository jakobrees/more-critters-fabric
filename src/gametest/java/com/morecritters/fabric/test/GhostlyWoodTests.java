package com.morecritters.fabric.test;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.ids.GhostlyWoodIds;
import com.morecritters.fabric.module.ghostly_wood.BarnacleClusterBlock;
import com.morecritters.fabric.module.ghostly_wood.RumBottleBlock;
import com.morecritters.fabric.module.ghostly_wood.TatteredFlagBlock;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Ghost ship building blocks: stripping, the planks advancement, barnacles, flags, rum and kelp carpet. */
public class GhostlyWoodTests {
	@GameTest
	public void anAxeStripsAGhostlyLogKeepingItsAxisAndWearsOneDurability(GameTestHelper helper) {
		BlockPos pos = new BlockPos(3, 1, 3);
		helper.setBlock(pos, block(GhostlyWoodIds.Blocks.GHOSTLY_LOG).defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));

		rightClick(helper, player, pos);

		BlockState state = helper.getBlockState(pos);
		helper.assertValueEqual(state.getBlock(), block(GhostlyWoodIds.Blocks.STRIPPED_GHOSTLY_LOG), "block after stripping");
		helper.assertValueEqual(state.getValue(RotatedPillarBlock.AXIS), Direction.Axis.X, "axis after stripping");
		helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 1, "axe damage");
		helper.succeed();
	}

	@GameTest
	public void aCreativeAxeStripsGhostlyWoodWithoutWear(GameTestHelper helper) {
		BlockPos pos = new BlockPos(3, 1, 3);
		helper.setBlock(pos, block(GhostlyWoodIds.Blocks.GHOSTLY_WOOD));
		Player player = helper.makeMockServerPlayer(GameType.CREATIVE);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_AXE));

		rightClick(helper, player, pos);

		helper.assertValueEqual(helper.getBlockState(pos).getBlock(), block(GhostlyWoodIds.Blocks.STRIPPED_GHOSTLY_WOOD), "block after stripping");
		helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 0, "creative axe damage");
		helper.succeed();
	}

	@GameTest
	public void onlyAnAxeStripsAGhostlyLog(GameTestHelper helper) {
		BlockPos pos = new BlockPos(3, 1, 3);
		helper.setBlock(pos, block(GhostlyWoodIds.Blocks.GHOSTLY_LOG));
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));

		rightClick(helper, player, pos);

		helper.assertValueEqual(helper.getBlockState(pos).getBlock(), block(GhostlyWoodIds.Blocks.GHOSTLY_LOG), "log after a pickaxe click");
		helper.succeed();
	}

	@GameTest
	public void walkingOnGhostlyPlanksOrThePetrifiedSlabAwardsTheAdvancementButThePlainSlabDoesNot(GameTestHelper helper) {
		helper.assertTrue(stepAwards(helper, GhostlyWoodIds.Blocks.GHOSTLY_PLANKS), "ghostly planks award the advancement");
		helper.assertTrue(stepAwards(helper, GhostlyWoodIds.Blocks.WET_KELPY_GHOSTLY_PLANKS), "wet kelpy planks award the advancement");
		helper.assertTrue(stepAwards(helper, GhostlyWoodIds.Blocks.PETRIFIED_GHOSTLY_SLAB), "the petrified slab awards the advancement");
		helper.assertTrue(stepAwards(helper, GhostlyWoodIds.Blocks.PETRIFIED_GHOSTLY_STAIRS), "the petrified stairs award the advancement");
		helper.assertFalse(stepAwards(helper, GhostlyWoodIds.Blocks.GHOSTLY_SLAB), "the plain ghostly slab does not award it");
		helper.assertFalse(stepAwards(helper, GhostlyWoodIds.Blocks.GHOSTLY_STAIRS), "the plain ghostly stairs do not award it");
		helper.succeed();
	}

	@GameTest
	public void aPlacedBarnacleClusterPicksLookZeroTwoOrThree(GameTestHelper helper) {
		TestScenes.floor(helper);
		Set<Integer> seen = new HashSet<>();
		BlockPos pos = new BlockPos(3, 1, 3);
		for (int i = 0; i < 60; i++) {
			place(helper, GhostlyWoodIds.Blocks.BARNACLE_CLUSTER, new BlockPos(3, 0, 3), Direction.UP);
			seen.add(helper.getBlockState(pos).getValue(BarnacleClusterBlock.LOOK));
			helper.setBlock(pos, Blocks.AIR);
		}
		helper.assertValueEqual(seen, Set.of(0, 2, 3), "looks picked over 60 placements");
		helper.succeed();
	}

	@GameTest
	public void aBarnacleClusterFallsOffWhenItsFloorOrWallIsBroken(GameTestHelper helper) {
		TestScenes.floor(helper);
		Block barnacle = block(GhostlyWoodIds.Blocks.BARNACLE_CLUSTER);
		place(helper, GhostlyWoodIds.Blocks.BARNACLE_CLUSTER, new BlockPos(2, 0, 2), Direction.UP);
		helper.assertBlockPresent(barnacle, new BlockPos(2, 1, 2));

		BlockPos wall = new BlockPos(5, 3, 3);
		helper.setBlock(wall, Blocks.STONE);
		place(helper, GhostlyWoodIds.Blocks.BARNACLE_CLUSTER, wall, Direction.EAST);
		helper.assertBlockPresent(barnacle, new BlockPos(6, 3, 3));

		helper.setBlock(new BlockPos(2, 0, 2), Blocks.AIR);
		helper.setBlock(wall, Blocks.AIR);
		helper.assertBlockNotPresent(barnacle, new BlockPos(2, 1, 2));
		helper.assertBlockNotPresent(barnacle, new BlockPos(6, 3, 3));
		helper.succeed();
	}

	@GameTest
	public void stackedTatteredFlagsShowUpperRagsOverABottomRagAndRepickWhenTheBottomGoes(GameTestHelper helper) {
		Block flag = block(GhostlyWoodIds.Blocks.TATTERED_FLAG);
		for (int y = 1; y <= 3; y++) {
			helper.setBlock(new BlockPos(3, y, 3), flag);
		}
		assertLookBetween(helper, new BlockPos(3, 1, 3), 4, 6, "lowest panel");
		assertLookBetween(helper, new BlockPos(3, 2, 3), 0, 2, "middle panel");
		assertLookBetween(helper, new BlockPos(3, 3, 3), 0, 2, "top panel");

		helper.setBlock(new BlockPos(3, 1, 3), Blocks.AIR);
		assertLookBetween(helper, new BlockPos(3, 2, 3), 4, 6, "middle panel after the lowest is gone");
		assertLookBetween(helper, new BlockPos(3, 3, 3), 0, 2, "top panel after the lowest is gone");
		helper.succeed();
	}

	@GameTest
	public void spilledRumFlingsAMovingEntityTheWayThePourerFaced(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos puddle = new BlockPos(3, 1, 1);
		// The pourer looked south, so the puddle faces north and flings things south (+z).
		helper.setBlock(puddle, block(GhostlyWoodIds.Blocks.RUM_BOTTLE).defaultBlockState().setValue(RumBottleBlock.FACING, Direction.NORTH));
		ArmorStand stand = helper.spawn(EntityTypes.ARMOR_STAND, puddle);
		double startZ = stand.getZ();
		helper.runAfterDelay(5, () -> stand.setDeltaMovement(0.05, 0.0, 0.0));
		helper.succeedWhen(() -> helper.assertTrue(stand.getZ() > startZ + 1.5,
			"the stand should be flung south; z moved " + (stand.getZ() - startZ)));
	}

	@GameTest(maxTicks = 40)
	public void spilledRumLeavesAnEntityStandingStillAlone(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos puddle = new BlockPos(3, 1, 3);
		helper.setBlock(puddle, block(GhostlyWoodIds.Blocks.RUM_BOTTLE).defaultBlockState().setValue(RumBottleBlock.FACING, Direction.NORTH));
		ArmorStand stand = helper.spawn(EntityTypes.ARMOR_STAND, puddle);
		Vec3 start = stand.position();
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(stand.position().distanceTo(start) < 0.01, "a still stand stays put");
			helper.succeed();
		});
	}

	@GameTest
	public void rumIsPouredOnlyOnFullBlocksAndBreaksWithoutDrops(GameTestHelper helper) {
		TestScenes.floor(helper);
		Block rum = block(GhostlyWoodIds.Blocks.RUM_BOTTLE);
		helper.setBlock(new BlockPos(5, 1, 5), Blocks.GLASS);
		place(helper, GhostlyWoodIds.Blocks.RUM_BOTTLE, new BlockPos(5, 1, 5), Direction.UP);
		helper.assertBlockNotPresent(rum, new BlockPos(5, 2, 5));

		Player player = place(helper, GhostlyWoodIds.Blocks.RUM_BOTTLE, new BlockPos(2, 0, 2), Direction.UP);
		BlockPos puddle = new BlockPos(2, 1, 2);
		helper.assertBlockPresent(rum, puddle);
		helper.assertValueEqual(helper.getBlockState(puddle).getValue(RumBottleBlock.FACING),
			player.getDirection().getOpposite(), "the puddle faces the pourer");

		helper.destroyBlock(puddle);
		helper.assertBlockNotPresent(rum, puddle);
		helper.assertItemEntityNotPresent(rum.asItem());
		helper.succeed();
	}

	@GameTest
	public void driedKelpCarpetPopsOffWhenTheBlockUnderItGoes(GameTestHelper helper) {
		TestScenes.floor(helper);
		Block carpet = block(GhostlyWoodIds.Blocks.DRIED_KELP_CARPET);
		place(helper, GhostlyWoodIds.Blocks.DRIED_KELP_CARPET, new BlockPos(3, 0, 3), Direction.UP);
		helper.assertBlockPresent(carpet, new BlockPos(3, 1, 3));
		helper.setBlock(new BlockPos(3, 0, 3), Blocks.AIR);
		helper.assertBlockNotPresent(carpet, new BlockPos(3, 1, 3));
		helper.succeed();
	}

	// --- helpers --------------------------------------------------------------------------

	private static Block block(Identifier id) {
		return BuiltInRegistries.BLOCK.getValue(id);
	}

	/** A right-click on the top of the block, as the game hands it to the use-block event. */
	private static void rightClick(GameTestHelper helper, Player player, BlockPos pos) {
		BlockPos absolute = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
		UseBlockCallback.EVENT.invoker().interact(player, helper.getLevel(), InteractionHand.MAIN_HAND, hit);
	}

	/** A survival player places the block's item on {@code face} of {@code pos}; returns the player. */
	private static Player place(GameTestHelper helper, Identifier blockId, BlockPos pos, Direction face) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(block(blockId).asItem()));
		TestScenes.useItemOn(helper, player, pos, face);
		return player;
	}

	/** Whether a fresh player stepping on the block earns "walk on ghostly planks". */
	private static boolean stepAwards(GameTestHelper helper, Identifier blockId) {
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, block(blockId));
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(pos);
		BlockState state = helper.getLevel().getBlockState(absolute);
		state.getBlock().stepOn(helper.getLevel(), absolute, state, player);
		AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(MoreCritters.id("walk_on_ghostly_planks"));
		helper.assertTrue(advancement != null, "the walk_on_ghostly_planks advancement exists");
		return player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	private static void assertLookBetween(GameTestHelper helper, BlockPos pos, int min, int max, String what) {
		int look = helper.getBlockState(pos).getValue(TatteredFlagBlock.LOOK);
		helper.assertTrue(look >= min && look <= max, what + ": look " + look + " should be " + min + " to " + max);
	}
}
