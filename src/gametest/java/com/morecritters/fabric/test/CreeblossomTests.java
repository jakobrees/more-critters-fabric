package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.CreeblossomIds;
import com.morecritters.fabric.module.creeblossom.CreeblossomEntity;
import com.morecritters.fabric.module.creeblossom.CreeblossomModule;
import java.lang.reflect.Field;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * The creeblossom: a short-lived walking flower bomb whose side depends on where it grew; the blossombushes that open
 * at night and grow them; the potted bush; and the blossoming infection that bursts into creeblossoms.
 */
public class CreeblossomTests {
	private static final BlockPos AT = new BlockPos(3, 1, 3);

	@GameTest
	public void whereItGrowsDecidesItsKind(GameTestHelper helper) {
		dirtFloor(helper);
		CreeblossomEntity wild = helper.spawn(CreeblossomModule.CREEBLOSSOM, new BlockPos(1, 1, 1), EntitySpawnReason.MOB_SUMMONED);
		helper.setBlock(new BlockPos(3, 1, 3), block(CreeblossomIds.Blocks.CLOSED_BLOSSOMBUSH));
		CreeblossomEntity friendly = helper.spawn(CreeblossomModule.CREEBLOSSOM, new BlockPos(3, 1, 3), EntitySpawnReason.MOB_SUMMONED);
		helper.setBlock(new BlockPos(5, 1, 5), block(CreeblossomIds.Blocks.ELECTRIC_BLOSSOMBUSH));
		CreeblossomEntity electric = helper.spawn(CreeblossomModule.CREEBLOSSOM, new BlockPos(5, 1, 5), EntitySpawnReason.MOB_SUMMONED);
		helper.assertValueEqual(wild.kind(), CreeblossomEntity.Kind.WILD, "kind grown outside a bush");
		helper.assertValueEqual(wild.textureName(), "creeblossom", "wild texture");
		helper.assertValueEqual(friendly.kind(), CreeblossomEntity.Kind.FRIENDLY, "kind grown in a blossombush");
		helper.assertValueEqual(friendly.textureName(), "creeblossom_friendly", "friendly texture");
		helper.assertValueEqual(electric.kind(), CreeblossomEntity.Kind.ELECTRIC, "kind grown in an electric blossombush");
		helper.assertValueEqual(electric.textureName(), "creeblossom_electric", "electric texture");
		helper.succeed();
	}

	@GameTest(maxTicks = 80)
	public void itBlowsUpWhenItsTwentySecondsAreUpWithoutBreakingBlocks(GameTestHelper helper) {
		TestScenes.floor(helper);
		CreeblossomEntity flower = helper.spawnWithNoFreeWill(CreeblossomModule.CREEBLOSSOM, AT);
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(4, 1, 3));
		BlockPos glass = new BlockPos(3, 1, 4);
		helper.setBlock(glass, Blocks.GLASS);
		setField(flower, "lifeLeft", 5);
		// Primed when its life runs out, then a one-second fuse.
		helper.runAfterDelay(15, () -> helper.assertTrue(flower.isAlive(), "the fuse is still burning"));
		helper.succeedWhen(() -> {
			helper.assertTrue(flower.isRemoved(), "the creeblossom blew up");
			helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "the blast hurt the pig beside it");
			helper.assertBlockPresent(Blocks.GLASS, glass);
		});
	}

	@GameTest(maxTicks = 60)
	public void threeLandedHitsPrimeItEarly(GameTestHelper helper) {
		TestScenes.floor(helper);
		CreeblossomEntity flower = helper.spawnWithNoFreeWill(CreeblossomModule.CREEBLOSSOM, AT);
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(4, 1, 3));
		flower.doHurtTarget(helper.getLevel(), zombie);
		flower.doHurtTarget(helper.getLevel(), zombie);
		helper.runAfterDelay(5, () -> {
			helper.assertTrue(flower.isAlive(), "two hits do not prime it");
			flower.doHurtTarget(helper.getLevel(), zombie);
		});
		helper.runAfterDelay(15, () -> helper.assertTrue(flower.isAlive(), "the fuse is still burning"));
		helper.succeedWhen(() -> helper.assertTrue(flower.isRemoved(), "the creeblossom blew up after its third hit"));
	}

	@GameTest
	public void itsHitDealsTenExtraDamageToACreeper(GameTestHelper helper) {
		TestScenes.floor(helper);
		CreeblossomEntity flower = helper.spawnWithNoFreeWill(CreeblossomModule.CREEBLOSSOM, AT);
		Creeper creeper = helper.spawnWithNoFreeWill(EntityTypes.CREEPER, new BlockPos(4, 1, 3));
		flower.doHurtTarget(helper.getLevel(), creeper);
		// The extra 10 lands first; the 5-point bite falls inside the hurt cooldown and adds nothing.
		helper.assertValueEqual(creeper.getHealth(), creeper.getMaxHealth() - 10.0F, "creeper health after one hit");
		helper.succeed();
	}

	@GameTest
	public void aWildOneDropsSeedsAndGunpowderARecruitedOneNothing(GameTestHelper helper) {
		dirtFloor(helper);
		int kills = 16;
		for (int i = 0; i < kills; i++) {
			CreeblossomEntity wild = helper.spawnWithNoFreeWill(CreeblossomModule.CREEBLOSSOM, new BlockPos(1 + i % 6, 1, 1));
			wild.kill(helper.getLevel());
		}
		int seeds = count(helper, item(CreeblossomIds.Items.BLOSSOMBUSH_SEED));
		int gunpowder = count(helper, Items.GUNPOWDER);
		// 0-1 seeds and 0-3 gunpowder each.
		helper.assertTrue(seeds >= 1 && seeds <= kills, "seeds from " + kills + " wild creeblossoms: " + seeds);
		helper.assertTrue(gunpowder >= 1 && gunpowder <= 3 * kills, "gunpowder from " + kills + " wild creeblossoms: " + gunpowder);
		helper.killAllEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class);

		helper.setBlock(AT, block(CreeblossomIds.Blocks.CLOSED_BLOSSOMBUSH));
		CreeblossomEntity friendly = helper.spawn(CreeblossomModule.CREEBLOSSOM, AT, EntitySpawnReason.MOB_SUMMONED);
		friendly.kill(helper.getLevel());
		helper.assertValueEqual(count(helper, item(CreeblossomIds.Items.BLOSSOMBUSH_SEED)) + count(helper, Items.GUNPOWDER), 0,
			"items dropped by a recruited creeblossom");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void aBushOpensAtNightAndClosesByDay(GameTestHelper helper) {
		dirtFloor(helper);
		BlockPos open = new BlockPos(2, 1, 3);
		BlockPos closed = new BlockPos(5, 1, 3);
		helper.setBlock(open, block(CreeblossomIds.Blocks.BLOSSOMBUSH));
		helper.setBlock(closed, block(CreeblossomIds.Blocks.CLOSED_BLOSSOMBUSH));
		helper.runAfterDelay(3, () -> {
			boolean day = helper.getLevel().isBrightOutside();
			Block expected = block(day ? CreeblossomIds.Blocks.CLOSED_BLOSSOMBUSH : CreeblossomIds.Blocks.BLOSSOMBUSH);
			helper.assertBlockPresent(expected, open);
			helper.assertBlockPresent(expected, closed);
			helper.succeed();
		});
	}

	@GameTest
	public void aBushNeedsDirtUnderIt(GameTestHelper helper) {
		dirtFloor(helper);
		helper.setBlock(AT, block(CreeblossomIds.Blocks.CLOSED_BLOSSOMBUSH));
		helper.setBlock(AT.below(), Blocks.STONE);
		helper.assertBlockNotPresent(block(CreeblossomIds.Blocks.CLOSED_BLOSSOMBUSH), AT);
		helper.succeed();
	}

	@GameTest
	public void aBushDoesNotStandBesideAPotOfItsKind(GameTestHelper helper) {
		dirtFloor(helper);
		helper.setBlock(AT, block(CreeblossomIds.Blocks.CLOSED_BLOSSOMBUSH));
		helper.setBlock(new BlockPos(5, 1, 3), block(CreeblossomIds.Blocks.CLOSED_ELECTRIC_BLOSSOMBUSH));
		helper.setBlock(new BlockPos(5, 1, 4), block(CreeblossomIds.Blocks.POT_BLOSSOMBUSH));
		helper.setBlock(AT.east(), block(CreeblossomIds.Blocks.POT_BLOSSOMBUSH));
		helper.assertBlockNotPresent(block(CreeblossomIds.Blocks.CLOSED_BLOSSOMBUSH), AT);
		helper.assertBlockPresent(block(CreeblossomIds.Blocks.CLOSED_ELECTRIC_BLOSSOMBUSH), new BlockPos(5, 1, 3));
		helper.succeed();
	}

	@GameTest
	public void aBushGoesIntoAFlowerPotAndComesBackOut(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(AT, Blocks.FLOWER_POT);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(CreeblossomIds.Blocks.CLOSED_BLOSSOMBUSH), 2));
		BlockPos absolute = helper.absolutePos(AT);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
		UseBlockCallback.EVENT.invoker().interact(player, helper.getLevel(), InteractionHand.MAIN_HAND, hit);
		helper.assertBlockPresent(block(CreeblossomIds.Blocks.POT_BLOSSOMBUSH), AT);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "bushes left in a survival hand");

		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.useBlock(AT, player);
		helper.assertBlockPresent(Blocks.FLOWER_POT, AT);
		helper.assertTrue(player.getMainHandItem().is(item(CreeblossomIds.Blocks.BLOSSOMBUSH)), "an open blossombush comes back out");
		helper.succeed();
	}

	@GameTest
	public void breakingAPottedBushGivesTheBushAndThePot(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(AT, block(CreeblossomIds.Blocks.POT_ELECTRIC_BLOSSOMBUSH));
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		Vec3 stand = helper.absoluteVec(new Vec3(3.5, 1, 1.5));
		player.setPos(stand.x, stand.y, stand.z);
		player.gameMode.destroyBlock(helper.absolutePos(AT));
		helper.assertBlockNotPresent(block(CreeblossomIds.Blocks.POT_ELECTRIC_BLOSSOMBUSH), AT);
		helper.assertValueEqual(count(helper, item(CreeblossomIds.Blocks.ELECTRIC_BLOSSOMBUSH)), 1, "electric blossombushes dropped");
		helper.assertValueEqual(count(helper, Items.FLOWER_POT), 1, "flower pots dropped");
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void aBlossomingMobKilledByACreatureBurstsIntoFourCreeblossoms(GameTestHelper helper) {
		TestScenes.floor(helper);
		Zombie infected = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, AT);
		Zombie killer = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		infected.addEffect(new MobEffectInstance(CreeblossomModule.BLOSSOMING, MobEffectInstance.INFINITE_DURATION));
		infected.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(killer), 100.0F);
		helper.assertTrue(infected.isRemoved(), "the infected zombie burst");
		helper.assertValueEqual(helper.getEntities(CreeblossomModule.CREEBLOSSOM).size(), 4, "creeblossoms out of the burst");
		helper.assertValueEqual(count(helper, Items.ROTTEN_FLESH), 0, "rotten flesh dropped by the burst zombie");
		helper.succeed();
	}

	@GameTest
	public void aBlossomingMobKilledByNothingDiesNormally(GameTestHelper helper) {
		TestScenes.floor(helper);
		Zombie infected = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, AT);
		infected.addEffect(new MobEffectInstance(CreeblossomModule.BLOSSOMING, MobEffectInstance.INFINITE_DURATION));
		infected.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 100.0F);
		helper.assertTrue(infected.isDeadOrDying(), "the zombie died");
		helper.assertValueEqual(helper.getEntities(CreeblossomModule.CREEBLOSSOM).size(), 0, "creeblossoms");
		helper.succeed();
	}

	private static void dirtFloor(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(x, 0, z, Blocks.DIRT);
			}
		}
	}

	private static void setField(Object target, String name, int value) {
		try {
			Field field = target.getClass().getDeclaredField(name);
			field.setAccessible(true);
			field.setInt(target, value);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static Block block(Identifier id) {
		return BuiltInRegistries.BLOCK.getValue(id);
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
