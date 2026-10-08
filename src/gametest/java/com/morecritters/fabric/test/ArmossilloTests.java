package com.morecritters.fabric.test;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.module.armossillo.ArmossilloBlocks;
import com.morecritters.fabric.module.armossillo.ArmossilloEntities;
import com.morecritters.fabric.module.armossillo.ArmossilloEntity;
import com.morecritters.fabric.module.armossillo.ArmossilloItems;
import com.morecritters.fabric.module.armossillo.BabyArmossilloEntity;
import com.morecritters.fabric.module.armossillo.FlyingOozeRodEntity;
import com.morecritters.fabric.module.armossillo.GoobulbBlock;
import java.lang.reflect.Field;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.Vec3;

/**
 * The armossillo: sits down and bone-meals the ground, sneezes glowing ooze after glow berries,
 * its bred young become moss clumps that hatch with bone meal; plus its shell gear and ooze blocks.
 */
public class ArmossilloTests {
	private static final BlockPos MID = new BlockPos(3, 1, 3);

	@GameTest(maxTicks = 60)
	public void glowBerriesMakeItSneezeThreeToFiveGlowingOoze(GameTestHelper helper) {
		TestScenes.floor(helper);
		ArmossilloEntity armossillo = helper.spawnWithNoFreeWill(ArmossilloEntities.ARMOSSILLO, MID);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLOW_BERRIES, 4));

		armossillo.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 3, "glow berries left after one feeding");
		helper.assertValueEqual(countItems(helper, ArmossilloItems.GLOWING_OOZE), 0, "glowing ooze before the sneeze");

		helper.runAfterDelay(25, () -> {
			helper.assertValueInBetween(3, countItems(helper, ArmossilloItems.GLOWING_OOZE), 5, "glowing ooze sneezed out");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 60)
	public void aSecondGlowBerryIsRefusedUntilTheSneeze(GameTestHelper helper) {
		TestScenes.floor(helper);
		ArmossilloEntity armossillo = helper.spawnWithNoFreeWill(ArmossilloEntities.ARMOSSILLO, MID);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLOW_BERRIES, 4));

		armossillo.mobInteract(player, InteractionHand.MAIN_HAND);
		armossillo.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 3, "glow berries left after feeding twice in a row");

		helper.runAfterDelay(25, () -> {
			armossillo.mobInteract(player, InteractionHand.MAIN_HAND);
			helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "glow berries left after feeding again once it sneezed");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 80)
	public void sittingDownBoneMealsTheCropItSitsOn(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(MID.below(), Blocks.FARMLAND);
		helper.setBlock(MID, Blocks.WHEAT);
		ArmossilloEntity armossillo = helper.spawnWithNoFreeWill(ArmossilloEntities.ARMOSSILLO, MID);
		setInt(armossillo, "restTimer", 1);

		helper.runAfterDelay(3, () -> helper.assertTrue(armossillo.isSitting(), "a still armossillo sits down when its rest timer runs out"));
		// Bone-mealed 15 ticks after sitting down and on each of the next two ticks: at least 2 stages each time.
		helper.runAfterDelay(25, () -> {
			int age = helper.getBlockState(MID).getValue(CropBlock.AGE);
			helper.assertTrue(age >= 6, "wheat under a sitting armossillo bone-mealed three times, age " + age);
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void aSittingArmossilloGetsUpAtTheNextRest(GameTestHelper helper) {
		TestScenes.floor(helper);
		ArmossilloEntity armossillo = helper.spawnWithNoFreeWill(ArmossilloEntities.ARMOSSILLO, MID);
		setInt(armossillo, "restTimer", 1);
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(armossillo.isSitting(), "sat down");
			setInt(armossillo, "restTimer", 1);
		});
		helper.runAfterDelay(6, () -> {
			helper.assertFalse(armossillo.isSitting(), "got up again at the next rest");
			helper.succeed();
		});
	}

	@GameTest
	public void aBredArmossilloBecomesAMossClump(GameTestHelper helper) {
		TestScenes.floor(helper);
		ArmossilloEntity newborn = helper.spawn(ArmossilloEntities.ARMOSSILLO, MID);
		newborn.setAge(-24000);
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(ArmossilloBlocks.MOSS_CLUMP, MID);
			helper.assertTrue(newborn.isRemoved(), "the bred baby is replaced by the moss clump");
		});
	}

	@GameTest
	public void boneMealHatchesAMossClumpIntoABabyArmossillo(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(MID, ArmossilloBlocks.MOSS_CLUMP);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL, 2));

		TestScenes.useItemOn(helper, player, MID, Direction.UP);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "bone meal left");
		helper.assertBlockNotPresent(ArmossilloBlocks.MOSS_CLUMP, MID);
		helper.succeedWhen(() -> helper.assertEntityPresent(ArmossilloEntities.BABY_ARMOSSILLO, MID));
	}

	@GameTest
	public void aBabyArmossilloGrowsUpWhenItsTimerRunsOut(GameTestHelper helper) {
		TestScenes.floor(helper);
		BabyArmossilloEntity baby = helper.spawnWithNoFreeWill(ArmossilloEntities.BABY_ARMOSSILLO, MID);
		helper.assertFalse(baby.removeWhenFarAway(1000.0), "a baby armossillo despawns");
		setInt(baby, "ticksUntilGrown", 2);
		helper.succeedWhen(() -> {
			helper.assertEntityPresent(ArmossilloEntities.ARMOSSILLO);
			helper.assertTrue(baby.isRemoved(), "the baby is replaced by the adult");
		});
	}

	@GameTest
	public void goobulbsHangingInAVinePickTheirLook(GameTestHelper helper) {
		BlockPos top = new BlockPos(3, 5, 3);
		helper.setBlock(top.above(), Blocks.STONE);
		helper.setBlock(top, ArmossilloBlocks.GOOBULB);
		helper.assertValueEqual(helper.getBlockState(top).getValue(GoobulbBlock.STAGE), 3, "look of a single goobulb");

		helper.setBlock(top.below(), ArmossilloBlocks.GOOBULB);
		helper.setBlock(top.below(2), ArmossilloBlocks.GOOBULB);
		helper.assertValueEqual(helper.getBlockState(top).getValue(GoobulbBlock.STAGE), 2, "look of the top of a vine (cap)");
		helper.assertValueEqual(helper.getBlockState(top.below()).getValue(GoobulbBlock.STAGE), 1, "look of the middle (stem)");
		helper.assertValueEqual(helper.getBlockState(top.below(2)).getValue(GoobulbBlock.STAGE), 0, "look of the bottom (bulb)");
		helper.succeed();
	}

	@GameTest
	public void aGoobulbVineFallsWhenItsCeilingGoes(GameTestHelper helper) {
		BlockPos top = new BlockPos(3, 5, 3);
		helper.setBlock(top.above(), Blocks.STONE);
		helper.setBlock(top, ArmossilloBlocks.GOOBULB);
		helper.setBlock(top.below(), ArmossilloBlocks.GOOBULB);
		helper.setBlock(top.below(2), ArmossilloBlocks.GOOBULB);

		helper.setBlock(top.above(), Blocks.AIR);
		helper.succeedWhen(() -> {
			for (int i = 0; i < 3; i++) {
				helper.assertBlockNotPresent(ArmossilloBlocks.GOOBULB, top.below(i));
			}
		});
	}

	@GameTest(maxTicks = 40)
	public void aGlowFlashesIntoLightAndGoesOut(GameTestHelper helper) {
		BlockPos pos = new BlockPos(3, 3, 3);
		helper.setBlock(pos, ArmossilloBlocks.GLOW);
		helper.startSequence()
			.thenExecuteAfter(2, () -> helper.assertBlockPresent(Blocks.LIGHT, pos))
			.thenExecuteAfter(8, () -> helper.assertBlockPresent(Blocks.AIR, pos))
			.thenSucceed();
	}

	@GameTest
	public void throwingAnOozeRodLaunchesItFromAboveTheHead(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Vec3 feet = helper.absoluteVec(new Vec3(3.5, 1.0, 3.5));
		player.snapTo(feet.x, feet.y, feet.z, 0.0F, 0.0F);
		ItemStack rods = new ItemStack(ArmossilloItems.OOZE_ROD, 3);
		player.setItemInHand(InteractionHand.MAIN_HAND, rods);

		rods.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "ooze rods left");
		helper.assertTrue(player.getCooldowns().isOnCooldown(player.getMainHandItem()), "the ooze rod is on cooldown");
		FlyingOozeRodEntity thrown = helper.getEntities(ArmossilloItems.FLYING_OOZE_ROD).stream().findFirst().orElse(null);
		helper.assertTrue(thrown != null, "a flying ooze rod was thrown");
		helper.assertValueEqual(thrown.blockPosition().getY(), BlockPos.containing(feet).getY() + 1, "block height of the thrown rod (above the head)");
		helper.assertTrue(thrown.getDeltaMovement().z > 2.9, "the rod flies along the gaze at speed 3, was " + thrown.getDeltaMovement());
		thrown.discard();
		helper.succeed();
	}

	@GameTest(maxTicks = 60)
	public void aFlyingOozeRodLightsItsPathAndLandsAsAnItem(GameTestHelper helper) {
		TestScenes.floor(helper);
		FlyingOozeRodEntity rod = helper.spawn(ArmossilloItems.FLYING_OOZE_ROD, new BlockPos(3, 5, 3));
		boolean[] lit = {false};
		helper.onEachTick(() -> {
			for (int y = 2; y <= 5; y++) {
				if (helper.getBlockState(new BlockPos(3, y, 3)).is(Blocks.LIGHT)) lit[0] = true;
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(lit[0], "the rod left light along its path");
			helper.assertTrue(rod.isRemoved(), "the rod is gone once over ground");
			helper.assertValueEqual(countItems(helper, ArmossilloItems.OOZE_ROD), 1, "ooze rod items dropped");
		});
	}

	@GameTest
	public void theSturdyChestplateShrugsOffAboutOneHitInFive(GameTestHelper helper) {
		TestScenes.floor(helper);
		Husk wearer = helper.spawnWithNoFreeWill(EntityTypes.HUSK, MID);
		wearer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ArmossilloItems.STURDY_CHESTPLATE));
		Husk bare = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(5, 1, 5));
		int hits = 200;
		helper.assertValueInBetween(15, nullifiedHits(helper, wearer, hits), 70, "hits nullified out of " + hits + " in a sturdy chestplate");
		helper.assertValueEqual(nullifiedHits(helper, bare, hits), 0, "hits nullified without the chestplate");
		helper.succeed();
	}

	@GameTest(maxTicks = 60)
	public void theBitingShieldBitesWhoeverHitsIt(GameTestHelper helper) {
		biteTest(helper, 0, 2.0F, 4.0F);
	}

	@GameTest(maxTicks = 60)
	public void sharpTeethMultipliesTheBite(GameTestHelper helper) {
		biteTest(helper, 2, 4.0F, 8.0F);
	}

	/** A husk raises the shield towards a pig, the pig hits it, and the pig should lose min to max health. */
	private static void biteTest(GameTestHelper helper, int sharpTeeth, float min, float max) {
		TestScenes.floor(helper);
		Husk defender = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(3, 1, 2));
		Pig attacker = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 1, 4));
		ItemStack shield = new ItemStack(ArmossilloItems.BITING_SHIELD);
		if (sharpTeeth > 0) {
			var enchantment = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
				.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, MoreCritters.id("sharp_teeth")));
			shield.enchant(enchantment, sharpTeeth);
		}
		defender.setItemInHand(InteractionHand.OFF_HAND, shield);
		faceSouth(defender);
		defender.startUsingItem(InteractionHand.OFF_HAND);

		helper.runAfterDelay(10, () -> {
			faceSouth(defender);
			helper.assertTrue(defender.isBlocking(), "the husk is raising the biting shield");
			float before = attacker.getHealth();
			defender.setInvulnerableTime(0);
			defender.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(attacker), 2.0F);
			helper.assertValueInBetween(min, before - attacker.getHealth(), max, "damage the shield bit the attacker for");
			helper.succeed();
		});
	}

	/** Towards +z, where the attacker stands. */
	private static void faceSouth(Husk husk) {
		husk.setYRot(0.0F);
		husk.setYHeadRot(0.0F);
		husk.setYBodyRot(0.0F);
	}

	private static int nullifiedHits(GameTestHelper helper, Husk target, int hits) {
		ServerLevel level = helper.getLevel();
		int nullified = 0;
		for (int i = 0; i < hits; i++) {
			target.setInvulnerableTime(0);
			target.damageCooldownTime = 0;
			target.setHealth(target.getMaxHealth());
			target.hurtServer(level, level.damageSources().generic(), 4.0F);
			if (target.getHealth() == target.getMaxHealth()) nullified++;
		}
		return nullified;
	}

	private static int countItems(GameTestHelper helper, Item kind) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds(), item -> item.getItem().is(kind))
			.stream().mapToInt(item -> item.getItem().getCount()).sum();
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
