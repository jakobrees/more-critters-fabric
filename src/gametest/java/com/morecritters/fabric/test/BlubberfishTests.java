package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.BlubberfishIds;
import com.morecritters.fabric.module.blubberfish.BlubberBlock;
import com.morecritters.fabric.module.blubberfish.BlubberfishEntity;
import com.morecritters.fabric.module.blubberfish.BlubberfishFryEntity;
import com.morecritters.fabric.module.blubberfish.BlubberfishModule;
import java.lang.reflect.Field;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** The blubberfish: buckets, the sprinkles burst, breeding into fry, drops, drying out and the blubber sheet. */
public class BlubberfishTests {
	private static final BlockPos MID = new BlockPos(3, 1, 3);

	@GameTest
	public void aWaterBucketScoopsUpABlubberfish(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlubberfishEntity fish = helper.spawnWithNoFreeWill(BlubberfishModule.BLUBBERFISH, MID);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
		fish.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.getMainHandItem().is(item(BlubberfishIds.Items.BLUBBERFISH_BUCKET_BUCKET)), "the water bucket became a blubberfish bucket");
		helper.assertTrue(fish.isRemoved(), "the scooped fish is gone");
		helper.succeed();
	}

	@GameTest
	public void aWaterBucketScoopsUpAFry(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlubberfishFryEntity fry = helper.spawnWithNoFreeWill(BlubberfishModule.BLUBBERFISH_FRY, MID);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
		fry.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.getMainHandItem().is(item(BlubberfishIds.Items.BLUBBERFISH_FRY_BUCKET_BUCKET)), "the water bucket became a fry bucket");
		helper.assertTrue(fry.isRemoved(), "the scooped fry is gone");
		helper.succeed();
	}

	@GameTest(maxTicks = 60)
	public void sprinklesMakeABeachedBlubberfishBurst(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlubberfishEntity fish = helper.spawnWithNoFreeWill(BlubberfishModule.BLUBBERFISH, MID);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(BlubberfishIds.Items.SPRINKLES), 3));
		fish.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "sprinkles left");
		helper.assertTrue(fish.isExploding(), "the fish swells up");
		helper.assertValueEqual(fish.textureName(), "blubberfish_explode", "texture of a swelling fish");

		helper.runAfterDelay(15, () -> helper.assertFalse(fish.isRemoved(), "the fish burst before its fuse ran out"));
		helper.runAfterDelay(26, () -> {
			helper.assertTrue(fish.isRemoved(), "the fish has burst");
			helper.assertValueEqual(helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds()).size(), 0, "items dropped by the burst");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void sprinklesDoNothingToAFishInWater(GameTestHelper helper) {
		waterPool(helper);
		BlubberfishEntity fish = helper.spawnWithNoFreeWill(BlubberfishModule.BLUBBERFISH, MID);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(BlubberfishIds.Items.SPRINKLES), 3));
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(fish.isInWater(), "the fish is in water");
			fish.mobInteract(player, InteractionHand.MAIN_HAND);
			helper.assertValueEqual(player.getMainHandItem().getCount(), 3, "sprinkles left");
			helper.assertFalse(fish.isExploding(), "a fish in water swells up");
			helper.succeed();
		});
	}

	@GameTest
	public void aBredBlubberfishHatchesAsAFry(GameTestHelper helper) {
		waterPool(helper);
		BlubberfishEntity newborn = helper.spawn(BlubberfishModule.BLUBBERFISH, MID);
		newborn.setAge(-24000);
		helper.succeedWhen(() -> {
			helper.assertEntityPresent(BlubberfishModule.BLUBBERFISH_FRY);
			helper.assertTrue(newborn.isRemoved(), "the bred baby is replaced by a fry");
		});
	}

	@GameTest
	public void gravelIsItsFood(GameTestHelper helper) {
		BlubberfishEntity fish = helper.spawnWithNoFreeWill(BlubberfishModule.BLUBBERFISH, MID);
		helper.assertTrue(fish.isFood(new ItemStack(Items.GRAVEL)), "gravel is blubberfish food");
		helper.assertFalse(fish.isFood(new ItemStack(Items.KELP)), "kelp is blubberfish food");
		helper.succeed();
	}

	@GameTest
	public void aFryGrowsIntoABlubberfish(GameTestHelper helper) {
		waterPool(helper);
		BlubberfishFryEntity fry = helper.spawnWithNoFreeWill(BlubberfishModule.BLUBBERFISH_FRY, MID);
		helper.assertFalse(fry.removeWhenFarAway(1000.0), "a fry despawns");
		setInt(fry, "ticksUntilGrown", 2);
		helper.succeedWhen(() -> {
			helper.assertEntityPresent(BlubberfishModule.BLUBBERFISH);
			helper.assertTrue(fry.isRemoved(), "the fry is replaced by the adult");
		});
	}

	@GameTest
	public void killedByAPlayerItDropsRawBlubberfishAndSometimesBlubber(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		int kills = 20;
		for (int i = 0; i < kills; i++) {
			BlubberfishEntity fish = helper.spawnWithNoFreeWill(BlubberfishModule.BLUBBERFISH, MID);
			fish.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 100.0F);
		}
		helper.succeedWhen(() -> {
			helper.assertValueEqual(countItems(helper, item(BlubberfishIds.Items.RAW_BLUBBERFISH)), kills, "raw blubberfish from " + kills + " kills");
			int blubber = countItems(helper, BlubberfishModule.BLUBBER.asItem());
			// Half the kills drop 1 to 3 blubber.
			helper.assertValueInBetween(1, blubber, 3 * kills, "blubber from " + kills + " kills");
			List<ItemEntity> other = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds(),
				drop -> !drop.getItem().is(item(BlubberfishIds.Items.RAW_BLUBBERFISH)) && !drop.getItem().is(BlubberfishModule.BLUBBER.asItem()));
			helper.assertValueEqual(other.size(), 0, "other drops");
		});
	}

	@GameTest(maxTicks = 40)
	public void diedWithoutAKillerItDropsNothing(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlubberfishEntity fish = helper.spawnWithNoFreeWill(BlubberfishModule.BLUBBERFISH, MID);
		fish.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 100.0F);
		helper.runAfterDelay(25, () -> {
			helper.assertTrue(fish.isRemoved(), "the fish died");
			helper.assertValueEqual(helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds()).size(), 0, "items dropped");
			helper.succeed();
		});
	}

	@GameTest
	public void drowningDoesNotHurtIt(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlubberfishEntity fish = helper.spawnWithNoFreeWill(BlubberfishModule.BLUBBERFISH, MID);
		BlubberfishFryEntity fry = helper.spawnWithNoFreeWill(BlubberfishModule.BLUBBERFISH_FRY, new BlockPos(5, 1, 5));
		fish.hurtServer(helper.getLevel(), helper.getLevel().damageSources().drown(), 2.0F);
		fry.hurtServer(helper.getLevel(), helper.getLevel().damageSources().drown(), 2.0F);
		helper.assertValueEqual(fish.getHealth(), fish.getMaxHealth(), "blubberfish health after drowning damage");
		helper.assertValueEqual(fry.getHealth(), fry.getMaxHealth(), "fry health after drowning damage");
		helper.succeed();
	}

	@GameTest(maxTicks = 260)
	public void itDriesOutOnLand(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlubberfishEntity fish = helper.spawnWithNoFreeWill(BlubberfishModule.BLUBBERFISH, MID);
		helper.runAfterDelay(2, () -> helper.assertValueEqual(fish.textureName(), "blubberfish_land", "texture of a beached fish"));
		helper.runAfterDelay(190, () -> helper.assertValueEqual(fish.getHealth(), fish.getMaxHealth(), "health after 190 ticks on land"));
		helper.runAfterDelay(215, () -> {
			helper.assertValueEqual(fish.getHealth(), fish.getMaxHealth() - 1.0F, "health once it has started drying out");
			helper.succeed();
		});
	}

	@GameTest
	public void blubberSticksToAWallFacingAway(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos wall = new BlockPos(3, 1, 3);
		helper.setBlock(wall, Blocks.STONE);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BlubberfishModule.BLUBBER, 2));
		// Clicking the south face of the wall while looking north.
		player.setYRot(180.0F);
		TestScenes.useItemOn(helper, player, wall, Direction.SOUTH);
		BlockPos sheet = wall.south();
		helper.assertBlockPresent(BlubberfishModule.BLUBBER, sheet);
		helper.assertValueEqual(helper.getBlockState(sheet).getValue(BlubberBlock.FACING), Direction.SOUTH,
			"the sheet faces away from the wall");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "blubber left");
		helper.succeed();
	}

	@GameTest
	public void blubberFallsOffWhenItsWallGoes(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos wall = new BlockPos(3, 1, 3);
		helper.setBlock(wall, Blocks.STONE);
		BlockPos sheet = wall.south();
		helper.setBlock(sheet, BlubberfishModule.BLUBBER.defaultBlockState().setValue(BlubberBlock.FACING, Direction.SOUTH));
		helper.assertBlockPresent(BlubberfishModule.BLUBBER, sheet);
		helper.setBlock(wall, Blocks.AIR);
		helper.succeedWhen(() -> helper.assertBlockNotPresent(BlubberfishModule.BLUBBER, sheet));
	}

	/** Still water two blocks deep over a stone floor, walled in with glass. */
	private static void waterPool(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(x, 0, z, Blocks.STONE);
				boolean wall = x == 0 || x == 7 || z == 0 || z == 7;
				for (int y = 1; y < 4; y++) {
					helper.setBlock(x, y, z, wall ? Blocks.GLASS : Blocks.WATER);
				}
			}
		}
	}

	private static int countItems(GameTestHelper helper, Item kind) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds(), drop -> drop.getItem().is(kind))
			.stream().mapToInt(drop -> drop.getItem().getCount()).sum();
	}

	private static Item item(Identifier id) {
		return BuiltInRegistries.ITEM.getValue(id);
	}

	/** Fast-forwards one of the module's private tick counters. */
	private static void setInt(Object target, String field, int value) {
		for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
			try {
				Field f = type.getDeclaredField(field);
				f.setAccessible(true);
				f.setInt(target, value);
				return;
			} catch (NoSuchFieldException e) {
				// look in the superclass
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		}
		throw new IllegalArgumentException("no field " + field + " on " + target.getClass());
	}
}
