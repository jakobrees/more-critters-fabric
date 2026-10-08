package com.morecritters.fabric.test;

import com.morecritters.fabric.module.shock_cube.ShockCubeEntity;
import com.morecritters.fabric.module.shock_cube.ShockCubeModule;
import com.morecritters.fabric.module.shock_cube.ThunderballProjectile;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Shock cubes, the electrocuted effect, the taser, the tazegun and the bottle o' electricity. */
public class ShockCubeTests {
	@GameTest
	public void aGlassBottleScoopsUpABigShockCube(GameTestHelper helper) {
		TestScenes.floor(helper);
		ShockCubeEntity cube = helper.spawn(ShockCubeModule.SHOCK_CUBE, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));
		cube.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertTrue(cube.isRemoved(), "the cube is bottled");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "glass bottles left");
		helper.assertTrue(player.getInventory().contains(stack -> stack.is(ShockCubeModule.BOTTLE_OF_ELECTRICITY)), "the player gets a bottle o' electricity");
		helper.succeed();
	}

	@GameTest
	public void aSmallShockCubeCannotBeBottled(GameTestHelper helper) {
		TestScenes.floor(helper);
		ShockCubeEntity cube = helper.spawn(ShockCubeModule.SHOCK_CUBE_SMALL, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));
		cube.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertFalse(cube.isRemoved(), "the small cube stays");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "glass bottles left");
		helper.succeed();
	}

	@GameTest(maxTicks = 60)
	public void aShockCubeFadesAndVanishesAfterTwoSeconds(GameTestHelper helper) {
		TestScenes.floor(helper);
		ShockCubeEntity cube = helper.spawn(ShockCubeModule.SHOCK_CUBE_SMALL, new BlockPos(3, 1, 3));
		helper.assertValueEqual(cube.textureName(), "shock_cube1", "texture when fresh");
		helper.startSequence()
			.thenIdle(15)
			.thenExecute(() -> helper.assertValueEqual(cube.textureName(), "shock_cube2", "texture after 15 ticks"))
			.thenIdle(10)
			.thenExecute(() -> helper.assertValueEqual(cube.textureName(), "shock_cube3", "texture after 25 ticks"))
			.thenIdle(10)
			.thenExecute(() -> {
				helper.assertValueEqual(cube.textureName(), "shock_cube4", "texture after 35 ticks");
				helper.assertFalse(cube.isRemoved(), "the cube is still there after 35 ticks");
			})
			.thenIdle(8)
			.thenExecute(() -> helper.assertTrue(cube.isRemoved(), "the cube is gone after 43 ticks"))
			.thenSucceed();
	}

	@GameTest(maxTicks = 60)
	public void shockCubesZapNearbyMobsButSpareItemsAndEachOther(GameTestHelper helper) {
		TestScenes.floor(helper);
		Cow cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(3, 1, 3));
		ItemEntity item = helper.spawnItem(Items.STICK, new BlockPos(4, 1, 4));
		// Five small cubes: one zap roll in ten per cube per tick, so the cow is struck for practically every run.
		ShockCubeEntity[] cubes = new ShockCubeEntity[5];
		for (int i = 0; i < cubes.length; i++) {
			cubes[i] = helper.spawn(ShockCubeModule.SHOCK_CUBE_SMALL, new BlockPos(2 + i % 3, 1, 2 + i / 3));
		}
		helper.startSequence()
			.thenIdle(30)
			.thenExecute(() -> {
				helper.assertTrue(cow.getHealth() < cow.getMaxHealth() || cow.isDeadOrDying(), "the cow is zapped");
				helper.assertFalse(item.isRemoved(), "a dropped item is spared");
				for (ShockCubeEntity cube : cubes) {
					helper.assertValueEqual(cube.getHealth(), cube.getMaxHealth(), "a shock cube's health next to other cubes");
				}
			})
			.thenSucceed();
	}

	@GameTest
	public void aShockCubeShrugsOffPlayersHitsButNotOtherDamage(GameTestHelper helper) {
		TestScenes.floor(helper);
		ShockCubeEntity cube = helper.spawn(ShockCubeModule.SHOCK_CUBE_SMALL, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		cube.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 4.0F);
		cube.hurtServer(helper.getLevel(), helper.getLevel().damageSources().lightningBolt(), 4.0F);
		cube.hurtServer(helper.getLevel(), helper.getLevel().damageSources().fall(), 4.0F);
		helper.assertValueEqual(cube.getHealth(), 10.0F, "health after a player's hit, lightning and a fall");
		cube.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 4.0F);
		helper.assertValueEqual(cube.getHealth(), 6.0F, "health after generic damage");
		helper.succeed();
	}

	@GameTest
	public void drinkingABottleOElectricityElectrocutesAndReturnsTheBottle(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack bottle = new ItemStack(ShockCubeModule.BOTTLE_OF_ELECTRICITY);
		player.setItemInHand(InteractionHand.MAIN_HAND, bottle);
		ItemStack left = bottle.finishUsingItem(helper.getLevel(), player);
		MobEffectInstance charge = player.getEffect(ShockCubeModule.ELECTROCUTED);
		helper.assertTrue(charge != null, "the drinker is electrocuted");
		helper.assertValueEqual(charge.getDuration(), 2000, "electrocuted ticks");
		helper.assertValueEqual(left.getItem(), Items.GLASS_BOTTLE, "what is left after drinking");
		helper.succeed();
	}

	@GameTest
	public void anElectrocutedAttackerPassesItsChargeToItsVictim(GameTestHelper helper) {
		TestScenes.floor(helper);
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		Cow cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(5, 1, 5));
		zombie.addEffect(new MobEffectInstance(ShockCubeModule.ELECTROCUTED, 500));
		cow.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(zombie), 1.0F);
		helper.assertFalse(zombie.hasEffect(ShockCubeModule.ELECTROCUTED), "the attacker loses its charge");
		MobEffectInstance charge = cow.getEffect(ShockCubeModule.ELECTROCUTED);
		helper.assertTrue(charge != null, "the victim takes the charge");
		helper.assertValueEqual(charge.getDuration(), 500, "electrocuted ticks passed on");
		helper.assertTrue(cow.getHealth() < cow.getMaxHealth() - 1.0F, "the charge hurts beyond the hit itself");
		helper.succeed();
	}

	@GameTest
	public void dyingElectrocutedLeavesAShockCube(GameTestHelper helper) {
		TestScenes.floor(helper);
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 1, 3));
		pig.addEffect(new MobEffectInstance(ShockCubeModule.ELECTROCUTED, 500));
		pig.kill(helper.getLevel());
		helper.succeedWhen(() -> helper.assertEntityPresent(ShockCubeModule.SHOCK_CUBE));
	}

	@GameTest
	public void dyingWithoutChargeLeavesNoShockCube(GameTestHelper helper) {
		TestScenes.floor(helper);
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 1, 3));
		pig.kill(helper.getLevel());
		helper.runAfterDelay(5, () -> {
			helper.assertEntityNotPresent(ShockCubeModule.SHOCK_CUBE);
			helper.succeed();
		});
	}

	@GameTest
	public void theTaserShocksNearbyMobsButSparesPetsAndCubes(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Vec3 at = helper.absoluteVec(new Vec3(3.5, 1.0, 3.5));
		player.setPos(at.x, at.y, at.z);
		Cow cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(1, 1, 1));
		Wolf pet = helper.spawnWithNoFreeWill(EntityTypes.WOLF, new BlockPos(6, 1, 6));
		pet.tame(player);
		ShockCubeEntity cube = helper.spawn(ShockCubeModule.SHOCK_CUBE_SMALL, new BlockPos(5, 1, 1));
		ItemStack taser = new ItemStack(ShockCubeModule.TASER);
		player.setItemInHand(InteractionHand.MAIN_HAND, taser);

		taser.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		float cowAfterFirst = cow.getHealth();
		helper.assertValueInBetween(1.0F, cowAfterFirst, 5.0F, "cow health after a 5-9 damage tase");
		helper.assertValueEqual(pet.getHealth(), pet.getMaxHealth(), "own pet's health");
		helper.assertValueEqual(cube.getHealth(), cube.getMaxHealth(), "shock cube's health");
		helper.assertTrue(taser.getDamageValue() > 0, "the taser wears");
		helper.assertTrue(player.getCooldowns().isOnCooldown(taser), "the taser cools down");

		Cow freshCow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(1, 1, 5));
		taser.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(freshCow.getHealth(), freshCow.getMaxHealth(), "cow health after a tase during the cooldown");
		helper.succeed();
	}

	@GameTest
	public void theTazegunFiresAThunderballAndCoolsDown(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Vec3 at = helper.absoluteVec(new Vec3(3.5, 1.0, 3.5));
		player.setPos(at.x, at.y, at.z);
		ItemStack gun = new ItemStack(ShockCubeModule.TAZEGUN);
		player.setItemInHand(InteractionHand.MAIN_HAND, gun);
		gun.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertEntityPresent(ShockCubeModule.THUNDERBALL);
		helper.assertTrue(player.getCooldowns().isOnCooldown(gun), "the tazegun cools down");
		helper.assertValueEqual(gun.getDamageValue(), 0, "a shot does not wear the tazegun (as in the original)");
		helper.succeed();
	}

	@GameTest
	public void aSpentTazegunRefusesThenReloadsFromABottleOElectricity(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Vec3 at = helper.absoluteVec(new Vec3(3.5, 1.0, 3.5));
		player.setPos(at.x, at.y, at.z);
		ItemStack gun = new ItemStack(ShockCubeModule.TAZEGUN);
		gun.setDamageValue(249);
		player.setItemInHand(InteractionHand.MAIN_HAND, gun);

		gun.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertEntityNotPresent(ShockCubeModule.THUNDERBALL);
		helper.assertValueEqual(gun.getDamageValue(), 249, "a spent gun without a bottle stays spent");

		player.getInventory().add(new ItemStack(ShockCubeModule.BOTTLE_OF_ELECTRICITY));
		gun.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertEntityNotPresent(ShockCubeModule.THUNDERBALL);
		helper.assertValueEqual(gun.getDamageValue(), 0, "a reloaded gun is fresh");
		helper.assertFalse(player.getInventory().contains(stack -> stack.is(ShockCubeModule.BOTTLE_OF_ELECTRICITY)), "the reload uses the bottle");
		helper.succeed();
	}

	@GameTest
	public void aThunderballBurstsIntoAShockCubeAndElectrocutesAround(GameTestHelper helper) {
		TestScenes.floor(helper);
		Cow cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(5, 1, 3));
		ThunderballProjectile ball = new ThunderballProjectile(ShockCubeModule.THUNDERBALL, helper.getLevel());
		Vec3 from = helper.absoluteVec(new Vec3(3.5, 4.0, 3.5));
		ball.setPos(from.x, from.y, from.z);
		ball.shoot(0.0, -1.0, 0.0, 1.0F, 0.0F);
		helper.getLevel().addFreshEntity(ball);
		helper.succeedWhen(() -> {
			helper.assertEntityPresent(ShockCubeModule.SHOCK_CUBE);
			helper.assertTrue(cow.hasEffect(ShockCubeModule.ELECTROCUTED) || cow.isDeadOrDying(), "a cow beside the burst is electrocuted");
			helper.assertTrue(ball.isRemoved(), "the thunderball is gone after bursting");
		});
	}
}
