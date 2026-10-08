package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.ShockCubeIds;
import com.morecritters.fabric.ids.StincarpIds;
import com.morecritters.fabric.module.stincarp.StincarpEntity;
import com.morecritters.fabric.module.stincarp.StincarpModule;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** The stincarp: its discharges, the shock cube a hit leaves, drying out, the bucket, and asphyxiation. */
public class StincarpTests {
	private static final BlockPos FISH = new BlockPos(3, 1, 3);

	@GameTest(maxTicks = 60)
	public void hitInWaterItDischargesAtOnceAndLeavesAShockCube(GameTestHelper helper) {
		pool(helper);
		StincarpEntity fish = helper.spawn(StincarpModule.STINCARP, FISH);
		// A new stincarp ignores hits for its first 20 ticks.
		helper.startSequence()
			.thenExecuteAfter(25, () -> {
				helper.assertTrue(fish.isInWater(), "the stincarp is in water");
				helper.hurt(fish, helper.getLevel().damageSources().generic(), 1.0F);
				helper.assertValueEqual(helper.getEntities(shockCube()).size(), 1, "shock cubes after the hit");
			})
			.thenExecuteAfter(1, () -> {
				helper.assertTrue(fish.textureName().startsWith("stincarp_skeleton"), "flashing its skeleton, texture " + fish.textureName());
				MobEffectInstance stun = fish.getEffect(MobEffects.SLOWNESS);
				helper.assertTrue(stun != null && stun.getAmplifier() == 30, "held still by Slowness 31");
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 60)
	public void aSecondHitWithinASecondLeavesNoSecondShockCube(GameTestHelper helper) {
		pool(helper);
		StincarpEntity fish = helper.spawn(StincarpModule.STINCARP, FISH);
		helper.startSequence()
			.thenExecuteAfter(25, () -> helper.hurt(fish, helper.getLevel().damageSources().generic(), 1.0F))
			.thenExecuteAfter(12, () -> {
				helper.hurt(fish, helper.getLevel().damageSources().generic(), 1.0F);
				helper.assertValueEqual(helper.getEntities(shockCube()).size(), 1, "shock cubes after two quick hits");
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 60)
	public void hitOnLandItLeavesNoShockCube(GameTestHelper helper) {
		TestScenes.floor(helper);
		StincarpEntity fish = helper.spawn(StincarpModule.STINCARP, FISH);
		helper.runAfterDelay(25, () -> {
			helper.hurt(fish, helper.getLevel().damageSources().generic(), 1.0F);
			helper.assertEntityNotPresent(shockCube());
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 240)
	public void itDischargesOnItsOwnEveryFiveToTenSeconds(GameTestHelper helper) {
		pool(helper);
		StincarpEntity fish = helper.spawn(StincarpModule.STINCARP, FISH);
		helper.succeedWhen(() -> {
			helper.assertTrue(fish.textureName().startsWith("stincarp_skeleton"), "flashing its skeleton, texture " + fish.textureName());
			helper.assertEntityNotPresent(shockCube());
		});
	}

	@GameTest(maxTicks = 40)
	public void betweenDischargesItsStripesMoveOnEveryThreeTicks(GameTestHelper helper) {
		pool(helper);
		StincarpEntity fish = helper.spawn(StincarpModule.STINCARP, FISH);
		String[] seen = new String[1];
		helper.startSequence()
			.thenExecuteAfter(2, () -> seen[0] = fish.textureName())
			.thenExecuteAfter(3, () -> {
				helper.assertTrue(seen[0].startsWith("stincarp_loop"), "texture " + seen[0]);
				helper.assertTrue(fish.textureName().startsWith("stincarp_loop"), "texture " + fish.textureName());
				helper.assertFalse(fish.textureName().equals(seen[0]), "the stripes stayed on " + seen[0] + " for three ticks");
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 260)
	public void outOfWaterItDriesOutAfterTenSeconds(GameTestHelper helper) {
		TestScenes.floor(helper);
		StincarpEntity fish = helper.spawn(StincarpModule.STINCARP, FISH);
		helper.startSequence()
			.thenExecuteAfter(190, () -> helper.assertValueEqual(fish.getHealth(), 30.0F, "health before ten seconds on land"))
			.thenWaitUntil(() -> helper.assertTrue(fish.getHealth() <= 29.0F && fish.getHealth() >= 28.0F,
				"one dry-out hit of 1-2, health " + fish.getHealth()))
			.thenSucceed();
	}

	@GameTest
	public void lightningAndDrowningDoNotHurtIt(GameTestHelper helper) {
		TestScenes.floor(helper);
		StincarpEntity fish = helper.spawn(StincarpModule.STINCARP, FISH);
		fish.hurtServer(helper.getLevel(), helper.getLevel().damageSources().lightningBolt(), 5.0F);
		fish.hurtServer(helper.getLevel(), helper.getLevel().damageSources().drown(), 5.0F);
		helper.assertValueEqual(fish.getHealth(), 30.0F, "health");
		helper.succeed();
	}

	@GameTest
	public void aWaterBucketScoopsItUp(GameTestHelper helper) {
		pool(helper);
		StincarpEntity fish = helper.spawn(StincarpModule.STINCARP, FISH);
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
		fish.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
		helper.assertTrue(player.getMainHandItem().is(bucket()), "the hand holds a bucket of stincarp, got " + player.getMainHandItem());
		helper.assertTrue(fish.isRemoved(), "the stincarp is in the bucket");
		helper.succeed();
	}

	@GameTest
	public void aBucketOfStincarpReleasesAStincarpThatStays(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		BucketItem item = (BucketItem) bucket();
		item.checkExtraContent(player, helper.getLevel(), new ItemStack(item), helper.absolutePos(FISH));
		helper.assertEntityPresent(StincarpModule.STINCARP, FISH);
		StincarpEntity fish = helper.getEntities(StincarpModule.STINCARP).getFirst();
		helper.assertTrue(fish.fromBucket(), "the released stincarp remembers the bucket");
		helper.assertFalse(fish.removeWhenFarAway(10_000.0), "the released stincarp despawns");
		helper.succeed();
	}

	@GameTest
	public void aPoisonedCupcakeGivesTenSecondsOfAsphyxiation(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		new ItemStack(BuiltInRegistries.ITEM.getValue(StincarpIds.Items.CUPCAKE_ASPHYXIATION)).finishUsingItem(helper.getLevel(), player);
		MobEffectInstance asphyxiation = player.getEffect(StincarpModule.ASPHYXIATION);
		helper.assertTrue(asphyxiation != null, "the player has Asphyxiation");
		helper.assertValueEqual(asphyxiation.getDuration(), 200, "Asphyxiation ticks");
		helper.succeed();
	}

	@GameTest(maxTicks = 60)
	public void onLandAsphyxiationDrownsAPointEverySecond(GameTestHelper helper) {
		TestScenes.floor(helper);
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, FISH);
		pig.addEffect(new MobEffectInstance(StincarpModule.ASPHYXIATION, 200));
		helper.startSequence()
			.thenExecuteAfter(25, () -> helper.assertValueEqual(pig.getHealth(), 9.0F, "health after about a second"))
			.thenExecuteAfter(20, () -> helper.assertValueEqual(pig.getHealth(), 8.0F, "health after about two seconds"))
			.thenSucceed();
	}

	@GameTest(maxTicks = 60)
	public void inWaterAsphyxiationDrainsAirTwiceAsFast(GameTestHelper helper) {
		pool(helper);
		Pig choking = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 1, 2));
		Pig plain = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(5, 1, 5));
		choking.addEffect(new MobEffectInstance(StincarpModule.ASPHYXIATION, 200));
		helper.runAfterDelay(40, () -> {
			int extra = plain.getAirSupply() - choking.getAirSupply();
			// One more point a tick, each taken 10 ticks late: about 30 by now.
			helper.assertTrue(extra >= 25 && extra <= 32, "extra air lost " + extra);
			helper.succeed();
		});
	}

	/** A walled pool three blocks deep over a stone floor. */
	private static void pool(GameTestHelper helper) {
		TestScenes.floor(helper);
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				boolean wall = x == 0 || z == 0 || x == 7 || z == 7;
				for (int y = 1; y <= 4; y++) {
					helper.setBlock(x, y, z, wall ? Blocks.STONE : y == 4 ? Blocks.AIR : Blocks.WATER);
				}
			}
		}
	}

	private static EntityType<?> shockCube() {
		return BuiltInRegistries.ENTITY_TYPE.getValue(ShockCubeIds.Entities.SHOCK_CUBE);
	}

	private static Item bucket() {
		return BuiltInRegistries.ITEM.getValue(StincarpIds.Items.STINARP_BUCKET_BUCKET);
	}
}
