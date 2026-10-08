package com.morecritters.fabric.test;

import com.morecritters.fabric.module.ship_fittings.CannonBlock;
import com.morecritters.fabric.module.ship_fittings.CannonBlockEntity;
import com.morecritters.fabric.module.ship_fittings.CannonMenu;
import com.morecritters.fabric.module.ship_fittings.Infusion;
import com.morecritters.fabric.module.ship_fittings.JollyRogerBlock;
import com.morecritters.fabric.module.ship_fittings.ShipFittingsModule;
import com.morecritters.fabric.module.ship_fittings.ShipWheelBlock;
import com.morecritters.fabric.module.ship_fittings.TreasureChestBlockEntity;
import com.morecritters.fabric.ids.ShipFittingsIds;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** The ship's fittings: the cannon and its balls, the treasure chest, the ship wheel and the jolly roger. */
public class ShipFittingsTests {
	private static final BlockPos CANNON = new BlockPos(1, 1, 3);

	@GameTest
	public void aCannonFiresOneBallOnEachRisingEdge(GameTestHelper helper) {
		TestScenes.floor(helper);
		CannonBlockEntity cannon = cannon(helper, new ItemStack(ShipFittingsModule.CANNON_BALLS.get(Infusion.NONE), 4));
		EntityType<?> ballType = ShipFittingsModule.CANNON_BALL_PROJECTILES.get(Infusion.NONE);

		helper.setBlock(CANNON.north(), Blocks.REDSTONE_BLOCK);
		helper.assertValueEqual(cannon.getItem(0).getCount(), 3, "balls left after the first signal");
		helper.assertEntityPresent(ballType, CANNON.east());
		clearBalls(helper);

		helper.setBlock(CANNON.south(), Blocks.REDSTONE_BLOCK);
		helper.assertValueEqual(cannon.getItem(0).getCount(), 3, "balls left after a second source while still powered");

		helper.setBlock(CANNON.north(), Blocks.AIR);
		helper.setBlock(CANNON.south(), Blocks.AIR);
		helper.setBlock(CANNON.north(), Blocks.REDSTONE_BLOCK);
		helper.assertValueEqual(cannon.getItem(0).getCount(), 2, "balls left after the signal went off and on again");
		clearBalls(helper);
		helper.succeed();
	}

	@GameTest
	public void theBallFiredMatchesTheAmmunition(GameTestHelper helper) {
		TestScenes.floor(helper);
		cannon(helper, new ItemStack(ShipFittingsModule.CANNON_BALLS.get(Infusion.SLIME), 1));
		helper.setBlock(CANNON.north(), Blocks.REDSTONE_BLOCK);
		helper.assertEntityPresent(ShipFittingsModule.CANNON_BALL_PROJECTILES.get(Infusion.SLIME), CANNON.east());
		helper.assertEntityNotPresent(ShipFittingsModule.CANNON_BALL_PROJECTILES.get(Infusion.NONE));
		clearBalls(helper);
		helper.succeed();
	}

	@GameTest
	public void aCannonLoadedWithSomethingElseDoesNotFire(GameTestHelper helper) {
		TestScenes.floor(helper);
		CannonBlockEntity cannon = cannon(helper, new ItemStack(Items.COBBLESTONE, 4));
		helper.setBlock(CANNON.north(), Blocks.REDSTONE_BLOCK);
		helper.assertValueEqual(cannon.getItem(0).getCount(), 4, "cobblestone left");
		for (Infusion infusion : Infusion.values()) {
			helper.assertEntityNotPresent(ShipFittingsModule.CANNON_BALL_PROJECTILES.get(infusion));
		}
		helper.succeed();
	}

	@GameTest
	public void theCannonScreenTakesOnlyCannonBallsAndAComparatorReadsTheMagazine(GameTestHelper helper) {
		TestScenes.floor(helper);
		CannonBlockEntity cannon = cannon(helper, ItemStack.EMPTY);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		CannonMenu menu = new CannonMenu(1, player.getInventory(), cannon);
		helper.assertTrue(menu.getSlot(0).mayPlace(new ItemStack(ShipFittingsModule.CANNON_BALLS.get(Infusion.COLD))), "the slot takes a cold ball");
		helper.assertFalse(menu.getSlot(0).mayPlace(new ItemStack(Items.COBBLESTONE)), "the slot takes cobblestone");

		BlockPos abs = helper.absolutePos(CANNON);
		BlockState state = helper.getBlockState(CANNON);
		helper.assertValueEqual(state.getAnalogOutputSignal(helper.getLevel(), abs, Direction.WEST), 0, "comparator reading when empty");
		cannon.setItem(0, new ItemStack(ShipFittingsModule.CANNON_BALLS.get(Infusion.NONE), 16));
		helper.assertValueEqual(state.getAnalogOutputSignal(helper.getLevel(), abs, Direction.WEST), 15, "comparator reading with 16 balls");
		helper.succeed();
	}

	@GameTest
	public void aSlimeBallLandingSlowsEverythingNearby(GameTestHelper helper) {
		obsidianFloor(helper);
		LivingEntity golem = helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new BlockPos(6, 1, 3));
		Entity ball = helper.spawn(ShipFittingsModule.CANNON_BALL_PROJECTILES.get(Infusion.SLIME), new Vec3(2.5, 3.0, 3.5));
		ball.setDeltaMovement(0.0, -0.8, 0.0);
		helper.succeedWhen(() -> {
			helper.assertTrue(ball.isRemoved(), "the ball is gone");
			MobEffectInstance slowness = golem.getEffect(MobEffects.SLOWNESS);
			helper.assertTrue(slowness != null, "the golem is slowed");
			helper.assertValueEqual(slowness.getAmplifier(), 3, "slowness amplifier");
			helper.assertTrue(slowness.getDuration() > 180 && slowness.getDuration() <= 200, "slowness lasts 200 ticks, has " + slowness.getDuration());
		});
	}

	@GameTest(maxTicks = 120)
	public void aKeyUnlocksTheChestWhichSpillsItsTreasureAndCrumbles(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos pos = new BlockPos(3, 1, 3);
		helper.setBlock(pos, ShipFittingsModule.TREASURE_CHEST);
		TreasureChestBlockEntity chest = helper.getBlockEntity(pos, TreasureChestBlockEntity.class);
		chest.setItem(0, new ItemStack(Items.DIAMOND, 3));
		chest.setItem(8, new ItemStack(Items.GOLD_INGOT));
		chest.setItem(9, new ItemStack(Items.EMERALD));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.getValue(ShipFittingsIds.Items.TREASURE_KEY), 2));

		helper.useBlock(pos, player);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "keys left");
		helper.assertBlockPresent(ShipFittingsModule.TREASURE_CHEST_OPENING, pos);
		helper.startSequence()
			.thenExecuteAfter(15, () -> helper.assertItemEntityNotPresent(Items.DIAMOND))
			.thenWaitUntil(() -> {
				helper.assertBlockPresent(ShipFittingsModule.TREASURE_CHEST_OPEN, pos);
				helper.assertItemEntityPresent(Items.DIAMOND);
				helper.assertItemEntityPresent(Items.GOLD_INGOT);
				helper.assertItemEntityNotPresent(Items.EMERALD);
			})
			.thenExecuteAfter(30, () -> helper.assertBlockPresent(ShipFittingsModule.TREASURE_CHEST_OPEN, pos))
			.thenWaitUntil(() -> helper.assertBlockPresent(Blocks.AIR, pos))
			.thenSucceed();
	}

	@GameTest(maxTicks = 60)
	public void withoutAKeyTheChestStaysLocked(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos pos = new BlockPos(3, 1, 3);
		helper.setBlock(pos, ShipFittingsModule.TREASURE_CHEST);
		helper.getBlockEntity(pos, TreasureChestBlockEntity.class).setItem(0, new ItemStack(Items.DIAMOND));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		helper.useBlock(pos, player);
		helper.runAfterDelay(30, () -> {
			helper.assertBlockPresent(ShipFittingsModule.TREASURE_CHEST, pos);
			helper.assertItemEntityNotPresent(Items.DIAMOND);
			helper.succeed();
		});
	}

	@GameTest
	public void breakingALockedChestDoesNotSpillIt(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos pos = new BlockPos(3, 1, 3);
		helper.setBlock(pos, ShipFittingsModule.TREASURE_CHEST);
		helper.getBlockEntity(pos, TreasureChestBlockEntity.class).setItem(0, new ItemStack(Items.DIAMOND));
		helper.destroyBlock(pos);
		helper.runAfterDelay(5, () -> {
			helper.assertItemEntityNotPresent(Items.DIAMOND);
			helper.succeed();
		});
	}

	@GameTest
	public void theShipWheelDialsRedstoneUpAndDown(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos wheel = new BlockPos(3, 1, 3);
		BlockPos dust = new BlockPos(4, 1, 3);
		helper.setBlock(wheel, ShipFittingsModule.SHIP_WHEEL);
		helper.setBlock(dust, Blocks.REDSTONE_WIRE);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);

		for (int i = 0; i < 3; i++) helper.useBlock(wheel, player);
		helper.assertValueEqual(helper.getBlockState(dust).getValue(RedstoneWireBlock.POWER), 3, "dust power after three turns");

		player.setShiftKeyDown(true);
		helper.useBlock(wheel, player);
		helper.assertValueEqual(helper.getBlockState(dust).getValue(RedstoneWireBlock.POWER), 2, "dust power after a sneaking turn");
		for (int i = 0; i < 5; i++) helper.useBlock(wheel, player);
		helper.assertValueEqual(helper.getBlockState(dust).getValue(RedstoneWireBlock.POWER), 0, "dust power never goes below 0");

		player.setShiftKeyDown(false);
		for (int i = 0; i < 20; i++) helper.useBlock(wheel, player);
		helper.assertValueEqual(helper.getBlockState(dust).getValue(RedstoneWireBlock.POWER), 15, "dust power stops at 15");
		helper.runAfterDelay(2, () -> {
			helper.assertValueEqual(helper.getBlockState(wheel).getValue(ShipWheelBlock.ANIMATION), 1, "spin animation after turning up");
			helper.succeed();
		});
	}

	@GameTest
	public void rightClickingTheFlagCyclesItsWave(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos flag = hangFlag(helper, Blocks.STONE);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		int[] expected = {1, 2, 0, 1};
		for (int wave : expected) {
			helper.useBlock(flag, player);
			helper.assertValueEqual(helper.getBlockState(flag).getValue(JollyRogerBlock.ANIMATION), wave, "wave animation");
		}
		helper.assertValueEqual(helper.getBlockState(flag).getValue(JollyRogerBlock.LARGE), 0, "small flag on a wall");
		helper.succeed();
	}

	@GameTest
	public void onAScrewTheFlagIsLargeAndItDropsWhenTheScrewGoes(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos flag = hangFlag(helper, ShipFittingsModule.ECTOMETAL_SCREW);
		helper.assertValueEqual(helper.getBlockState(flag).getValue(JollyRogerBlock.LARGE), 1, "large flag on a screw");
		helper.setBlock(flag.south(), Blocks.AIR);
		helper.assertBlockPresent(Blocks.AIR, flag);
		helper.succeedWhen(() -> helper.assertItemEntityPresent(ShipFittingsModule.TATTERED_JOLLY_ROGER_ITEM));
	}

	// --- Helpers -------------------------------------------------------------------------

	/** A cannon at {@link #CANNON} facing east, loaded with {@code ammo}. */
	private static CannonBlockEntity cannon(GameTestHelper helper, ItemStack ammo) {
		helper.setBlock(CANNON, ShipFittingsModule.CANNON.defaultBlockState().setValue(CannonBlock.FACING, Direction.EAST));
		CannonBlockEntity cannon = helper.getBlockEntity(CANNON, CannonBlockEntity.class);
		cannon.setItem(0, ammo);
		return cannon;
	}

	/** Removes the balls in flight, so nothing explodes outside the test. */
	private static void clearBalls(GameTestHelper helper) {
		for (Infusion infusion : Infusion.values()) {
			helper.getEntities(ShipFittingsModule.CANNON_BALL_PROJECTILES.get(infusion)).forEach(Entity::discard);
		}
	}

	private static void obsidianFloor(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(x, 0, z, Blocks.OBSIDIAN);
			}
		}
	}

	/** {@code support} at (3, 1, 4) and a flag facing north hung on it at (3, 1, 3). */
	private static BlockPos hangFlag(GameTestHelper helper, net.minecraft.world.level.block.Block support) {
		BlockPos flag = new BlockPos(3, 1, 3);
		helper.setBlock(flag.south(), support);
		helper.setBlock(flag, ShipFittingsModule.TATTERED_JOLLY_ROGER.defaultBlockState().setValue(JollyRogerBlock.FACING, Direction.NORTH));
		return flag;
	}
}
