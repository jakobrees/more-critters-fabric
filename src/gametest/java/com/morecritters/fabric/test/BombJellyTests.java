package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.BombJellyIds;
import com.morecritters.fabric.module.bomb_jelly.BombJellyEntity;
import com.morecritters.fabric.module.bomb_jelly.BombJellyModule;
import com.morecritters.fabric.module.bomb_jelly.JellyTorpedoEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Bomb jellies: they blow up when something that is not a sea creature touches them, can be scooped up
 * or lit by hand; the explosive jelly they leave sets off Combustion; the jelly torpedo runs and explodes underwater.
 */
public class BombJellyTests {
	private static final BlockPos MIDDLE = new BlockPos(3, 2, 3);

	@GameTest(maxTicks = 80)
	public void somethingThatIsNotASeaCreatureTouchingAJellySetsItOffAndItLeavesExplosiveJelly(GameTestHelper helper) {
		pool(helper);
		BombJellyEntity jelly = helper.spawn(BombJellyModule.BOMB_JELLY_SMALL, MIDDLE);
		helper.spawnWithNoFreeWill(EntityTypes.PIG, MIDDLE);
		// A 15-tick fuse: still there a moment after the touch.
		helper.runAfterDelay(8, () -> helper.assertTrue(jelly.isAlive(), "the jelly waits out its fuse before bursting"));
		helper.succeedWhen(() -> {
			helper.assertTrue(jelly.isRemoved(), "the jelly is gone after bursting");
			// 2-3 thrown out by the burst, plus the one its own blast knocks out of it (loot table).
			int drops = count(helper, item(BombJellyIds.Items.EXPLOSIVE_JELLY));
			helper.assertTrue(drops >= 3 && drops <= 4, "explosive jelly dropped, 3-4, was " + drops);
		});
	}

	@GameTest(maxTicks = 60)
	public void aSeaCreatureTouchingAJellyDoesNotSetItOff(GameTestHelper helper) {
		pool(helper);
		BombJellyEntity jelly = helper.spawnWithNoFreeWill(BombJellyModule.BOMB_JELLY_SMALL, MIDDLE);
		helper.spawnWithNoFreeWill(EntityTypes.COD, MIDDLE);
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(jelly.isAlive(), "a cod does not set the jelly off");
			helper.assertValueEqual(count(helper, item(BombJellyIds.Items.EXPLOSIVE_JELLY)), 0, "explosive jelly dropped");
			helper.succeed();
		});
	}

	@GameTest
	public void aWaterBucketScoopsEachSizeIntoItsOwnBucket(GameTestHelper helper) {
		pool(helper);
		scoop(helper, BombJellyModule.BOMB_JELLY_SMALL, BombJellyIds.Items.SMALL_BOMB_JELLY_BUCKET);
		scoop(helper, BombJellyModule.BOMB_JELLY_MEDIUM, BombJellyIds.Items.MEDIUM_BOMB_JELLY_BUCKET);
		scoop(helper, BombJellyModule.BOMB_JELLY_LARGE, BombJellyIds.Items.LARGE_BOMB_JELLY_BUCKET);
		helper.succeed();
	}

	@GameTest
	public void emptyingAJellyBucketReleasesThatSize(GameTestHelper helper) {
		pool(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack bucket = new ItemStack(item(BombJellyIds.Items.MEDIUM_BOMB_JELLY_BUCKET));
		((MobBucketItem) bucket.getItem()).checkExtraContent(player, helper.getLevel(), bucket, helper.absolutePos(MIDDLE));
		helper.assertValueEqual(helper.getEntities(BombJellyModule.BOMB_JELLY_MEDIUM).size(), 1, "medium jellies released");
		helper.assertValueEqual(helper.getEntities(BombJellyModule.BOMB_JELLY_SMALL).size()
			+ helper.getEntities(BombJellyModule.BOMB_JELLY_LARGE).size(), 0, "jellies of another size");
		helper.succeed();
	}

	@GameTest(maxTicks = 60)
	public void flintAndSteelLightsAJellyWithoutTheBurstsDrops(GameTestHelper helper) {
		TestScenes.floor(helper);
		BombJellyEntity jelly = helper.spawnWithNoFreeWill(BombJellyModule.BOMB_JELLY_SMALL, new BlockPos(3, 1, 3));
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(4, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
		jelly.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 1, "flint and steel wear");
		helper.runAfterDelay(8, () -> helper.assertTrue(jelly.isAlive(), "the jelly waits out its fuse"));
		helper.succeedWhen(() -> {
			helper.assertTrue(jelly.isRemoved(), "the lit jelly is gone");
			helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "the blast hurt the pig beside it");
			// No 2-3 burst drops; only the loot of the jelly its own blast killed.
			helper.assertTrue(count(helper, item(BombJellyIds.Items.EXPLOSIVE_JELLY)) <= 1, "explosive jelly dropped, at most 1");
		});
	}

	@GameTest
	public void aSpawnedBombJellyTurnsIntoOneOfTheThreeSizes(GameTestHelper helper) {
		pool(helper);
		helper.spawn(BombJellyModule.BOMB_JELLY, MIDDLE);
		helper.succeedWhen(() -> {
			helper.assertEntityNotPresent(BombJellyModule.BOMB_JELLY);
			int sized = helper.getEntities(BombJellyModule.BOMB_JELLY_SMALL).size()
				+ helper.getEntities(BombJellyModule.BOMB_JELLY_MEDIUM).size()
				+ helper.getEntities(BombJellyModule.BOMB_JELLY_LARGE).size();
			helper.assertValueEqual(sized, 1, "sized jellies");
		});
	}

	@GameTest
	public void eatingExplosiveJellyLightsATenSecondCombustion(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack jelly = new ItemStack(item(BombJellyIds.Items.EXPLOSIVE_JELLY), 2);
		ItemStack left = jelly.finishUsingItem(helper.getLevel(), player);
		helper.assertValueEqual(left.getCount(), 1, "explosive jelly left");
		MobEffectInstance combustion = player.getEffect(BombJellyModule.COMBUSTION);
		helper.assertTrue(combustion != null, "eating gives Combustion");
		helper.assertValueEqual(combustion.getDuration(), 200, "Combustion ticks");
		helper.succeed();
	}

	@GameTest(maxTicks = 80)
	public void combustionRunningOutBlowsUpItsBearerAndBreaksBlocks(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos at = new BlockPos(3, 1, 3);
		BlockPos glass = new BlockPos(4, 1, 3);
		helper.setBlock(glass, Blocks.GLASS);
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, at);
		pig.addEffect(new MobEffectInstance(BombJellyModule.COMBUSTION, 30));
		helper.runAfterDelay(10, () -> helper.assertBlockPresent(Blocks.GLASS, glass));
		helper.succeedWhen(() -> {
			helper.assertBlockNotPresent(Blocks.GLASS, glass);
			helper.assertTrue(pig.isDeadOrDying(), "the pig was blown up");
		});
	}

	@GameTest(maxTicks = 40)
	public void combustionOnlyFizzlesForACreativePlayer(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos glass = new BlockPos(4, 1, 3);
		helper.setBlock(glass, Blocks.GLASS);
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		Vec3 at = helper.absoluteVec(new Vec3(3.5, 1, 3.5));
		player.setPos(at.x, at.y, at.z);
		// The fuse's last tick, applied by hand: the mock player is not ticked by the level.
		player.addEffect(new MobEffectInstance(BombJellyModule.COMBUSTION, 1));
		BombJellyModule.COMBUSTION.value().applyEffectTick(helper.getLevel(), player, 0);
		helper.assertBlockPresent(Blocks.GLASS, glass);
		helper.assertValueEqual(player.getHealth(), player.getMaxHealth(), "creative player health");
		helper.succeed();
	}

	@GameTest
	public void theTorpedoDoesNothingOutOfWater(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(BombJellyIds.Items.JELLY_TORPEDO_ITEM), 4));
		InteractionResult result = player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(result, InteractionResult.PASS, "use on land");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 4, "torpedoes left");
		helper.assertFalse(player.getCooldowns().isOnCooldown(player.getMainHandItem()), "cooldown after use on land");
		helper.assertEntityNotPresent(BombJellyModule.JELLY_TORPEDO);
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void aTorpedoOutOfWaterBurstsHarmlessly(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos glass = new BlockPos(4, 1, 3);
		helper.setBlock(glass, Blocks.GLASS);
		JellyTorpedoEntity torpedo = helper.spawn(BombJellyModule.JELLY_TORPEDO, new BlockPos(3, 1, 3));
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 1, 4));
		helper.succeedWhen(() -> {
			helper.assertTrue(torpedo.isRemoved(), "the torpedo is gone on land");
			helper.assertBlockPresent(Blocks.GLASS, glass);
			helper.assertValueEqual(pig.getHealth(), pig.getMaxHealth(), "pig health");
		});
	}

	@GameTest(maxTicks = 60)
	public void anArmedTorpedoExplodesOnACreatureUnderwater(GameTestHelper helper) {
		pool(helper);
		JellyTorpedoEntity torpedo = helper.spawn(BombJellyModule.JELLY_TORPEDO, new Vec3(3.5, 2, 1.5));
		// Facing south (+z), towards the pig three blocks on.
		torpedo.setYRot(0.0F);
		torpedo.setYHeadRot(0.0F);
		torpedo.setYBodyRot(0.0F);
		torpedo.setXRot(0.0F);
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(3.5, 2, 4.5));
		helper.succeedWhen(() -> {
			helper.assertTrue(torpedo.isRemoved(), "the torpedo exploded");
			helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "the pig was caught in the blast");
		});
	}

	/** A pool of still water five deep, walled in with glass. */
	private static void pool(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(x, 0, z, Blocks.STONE);
				boolean wall = x == 0 || x == 7 || z == 0 || z == 7;
				for (int y = 1; y <= 5; y++) {
					helper.setBlock(x, y, z, wall ? Blocks.GLASS : Blocks.WATER);
				}
			}
		}
	}

	private static void scoop(GameTestHelper helper, EntityType<BombJellyEntity> type, Identifier bucketId) {
		BombJellyEntity jelly = helper.spawnWithNoFreeWill(type, MIDDLE);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
		jelly.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()), bucketId, "bucket in hand");
		helper.assertTrue(jelly.isRemoved(), "the scooped jelly is gone");
	}

	private static Item item(Identifier id) {
		return BuiltInRegistries.ITEM.getValue(id);
	}

	private static int count(GameTestHelper helper, Item item) {
		return helper.getEntities(EntityTypes.ITEM).stream()
			.filter(drop -> drop.getItem().is(item))
			.mapToInt(drop -> drop.getItem().getCount())
			.sum();
	}
}
