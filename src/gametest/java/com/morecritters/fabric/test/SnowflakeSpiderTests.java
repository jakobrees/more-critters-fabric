package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.SnowflakeSpiderIds;
import com.morecritters.fabric.module.snowflake_spider.SnowflakeSpiderEntity;
import com.morecritters.fabric.module.snowflake_spider.SnowflakeSpiderModule;
import com.morecritters.fabric.module.snowflake_spider.WebEntity;
import com.morecritters.fabric.module.snowflake_spider.WebSackProjectile;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** The snowflake spider: its frostbite bite, the freezing cobweb, brittleness, and the web sack with its webs. */
public class SnowflakeSpiderTests {
	@GameTest
	public void aBiteGivesTenSecondsOfFrostbite(GameTestHelper helper) {
		TestScenes.floor(helper);
		SnowflakeSpiderEntity spider = helper.spawnWithNoFreeWill(SnowflakeSpiderModule.SNOWFLAKE_SPIDER, new BlockPos(3, 1, 3));
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(4, 1, 3));
		helper.assertTrue(spider.doHurtTarget(helper.getLevel(), pig), "the bite lands");
		MobEffectInstance frostbite = pig.getEffect(SnowflakeSpiderModule.FROSTBITE);
		helper.assertTrue(frostbite != null, "the pig has Frostbite");
		helper.assertValueEqual(frostbite.getDuration(), 200, "Frostbite ticks");
		helper.succeed();
	}

	@GameTest
	public void frostbiteKeepsItsVictimFrozenButNotASnowflakeSpider(GameTestHelper helper) {
		TestScenes.floor(helper);
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 1, 3));
		SnowflakeSpiderEntity spider = helper.spawnWithNoFreeWill(SnowflakeSpiderModule.SNOWFLAKE_SPIDER, new BlockPos(5, 1, 3));
		pig.addEffect(new MobEffectInstance(SnowflakeSpiderModule.FROSTBITE, 100));
		spider.addEffect(new MobEffectInstance(SnowflakeSpiderModule.FROSTBITE, 100));
		helper.runAfterDelay(5, () -> {
			// Frostbite sets 200 frozen ticks every tick; thawing takes 2 off afterwards.
			helper.assertTrue(pig.getTicksFrozen() >= 198, "pig's frozen ticks " + pig.getTicksFrozen());
			helper.assertValueEqual(spider.getTicksFrozen(), 0, "snowflake spider's frozen ticks");
			helper.succeed();
		});
	}

	@GameTest
	public void aFreezingCobwebFrostsCreaturesButNotSnowflakeSpiders(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(2, 1, 3, SnowflakeSpiderModule.FREEZING_COBWEB);
		helper.setBlock(5, 1, 3, SnowflakeSpiderModule.FREEZING_COBWEB);
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 1, 3));
		SnowflakeSpiderEntity spider = helper.spawnWithNoFreeWill(SnowflakeSpiderModule.SNOWFLAKE_SPIDER, new BlockPos(5, 1, 3));
		helper.runAfterDelay(10, () -> {
			// 2-5 frozen ticks a tick in the web, less 2 a tick of thawing.
			helper.assertTrue(pig.getTicksFrozen() > 0, "the pig in the web is frosting over");
			helper.assertValueEqual(spider.getTicksFrozen(), 0, "snowflake spider's frozen ticks");
			helper.succeed();
		});
	}

	@GameTest
	public void brittlenessTakesOneToThreeExtraHealthWithEveryHit(GameTestHelper helper) {
		TestScenes.floor(helper);
		Pig brittle = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 1, 3));
		Pig plain = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(5, 1, 3));
		brittle.addEffect(new MobEffectInstance(SnowflakeSpiderModule.BRITTLENESS, 200));
		helper.hurt(brittle, helper.getLevel().damageSources().generic(), 1.0F);
		helper.hurt(plain, helper.getLevel().damageSources().generic(), 1.0F);
		helper.assertValueEqual(plain.getHealth(), 9.0F, "health of a plain pig after a 1-point hit");
		float lost = 10.0F - brittle.getHealth();
		helper.assertTrue(lost >= 2.0F && lost < 4.0F, "a brittle pig loses 1 + [1, 3) health, lost " + lost);
		helper.succeed();
	}

	@GameTest
	public void aBrittlenessCupcakeGivesTenSecondsOfBrittleness(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		new ItemStack(BuiltInRegistries.ITEM.getValue(SnowflakeSpiderIds.Items.CUPCAKE_BRITTLENESS)).finishUsingItem(helper.getLevel(), player);
		MobEffectInstance brittleness = player.getEffect(SnowflakeSpiderModule.BRITTLENESS);
		helper.assertTrue(brittleness != null, "the player has Brittleness");
		helper.assertValueEqual(brittleness.getDuration(), 200, "Brittleness ticks");
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void aWebSackLandingOnTheGroundSpinsATuftOfFourFreezingCobwebs(GameTestHelper helper) {
		TestScenes.floor(helper);
		dropSack(helper, new BlockPos(3, 4, 3));
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(SnowflakeSpiderModule.FREEZING_COBWEB, new BlockPos(3, 1, 3));
			helper.assertBlockPresent(SnowflakeSpiderModule.FREEZING_COBWEB, new BlockPos(3, 2, 3));
			// One of each pair beside them, picked at random.
			helper.assertTrue(isWeb(helper, 3, 1, 4) != isWeb(helper, 3, 1, 2), "exactly one of the low side webs");
			helper.assertTrue(isWeb(helper, 4, 2, 2) != isWeb(helper, 2, 2, 2), "exactly one of the high side webs");
			helper.assertValueEqual(countWebs(helper), 4, "freezing cobwebs");
		});
	}

	@GameTest(maxTicks = 40)
	public void aWebSackWrapsASmallMobInAWebForGood(GameTestHelper helper) {
		TestScenes.floor(helper);
		Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(3, 1, 3));
		dropSack(helper, new BlockPos(3, 4, 3));
		helper.succeedWhen(() -> {
			MobEffectInstance webbed = pig.getEffect(SnowflakeSpiderModule.WEBBED);
			helper.assertTrue(webbed != null, "the pig is webbed");
			helper.assertTrue(webbed.isInfiniteDuration() || webbed.getDuration() > 1_000_000, "webbed for good, ticks " + webbed.getDuration());
			helper.assertEntityPresent(SnowflakeSpiderModule.WEB_ENTITY, new BlockPos(3, 1, 3), 1.5);
			helper.assertTrue(pig.isNoAi(), "the webbed pig cannot think");
			helper.assertValueEqual(countWebs(helper), 0, "freezing cobwebs");
		});
	}

	@GameTest(maxTicks = 40)
	public void aWebSackSpinsCobwebsAroundAMobTooBigToWrap(GameTestHelper helper) {
		TestScenes.floor(helper);
		IronGolem golem = helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new BlockPos(3, 1, 3));
		dropSack(helper, new BlockPos(3, 6, 3));
		helper.succeedWhen(() -> {
			helper.assertValueEqual(countWebs(helper), 2, "freezing cobwebs where the sack hit and above");
			helper.assertFalse(golem.hasEffect(SnowflakeSpiderModule.WEBBED), "the iron golem is webbed");
		});
	}

	@GameTest(maxTicks = 40)
	public void shearsCutAWebAndSetItsCaptiveFree(GameTestHelper helper) {
		TestScenes.floor(helper);
		Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(3, 1, 3));
		pig.addEffect(new MobEffectInstance(SnowflakeSpiderModule.WEBBED, MobEffectInstance.INFINITE_DURATION));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
		helper.startSequence()
			.thenExecuteAfter(3, () -> {
				helper.assertTrue(pig.isNoAi(), "the webbed pig cannot think");
				List<WebEntity> webs = helper.getEntities(SnowflakeSpiderModule.WEB_ENTITY);
				helper.assertValueEqual(webs.size(), 1, "webs");
				webs.getFirst().interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
				helper.assertTrue(webs.getFirst().isRemoved(), "the web is cut");
				helper.assertFalse(pig.isNoAi(), "the freed pig can think");
			})
			.thenExecuteAfter(2, () -> helper.assertFalse(pig.hasEffect(SnowflakeSpiderModule.WEBBED), "the pig is still webbed"))
			.thenSucceed();
	}

	@GameTest(maxTicks = 40)
	public void aWebShrugsOffAPlayersHits(GameTestHelper helper) {
		TestScenes.floor(helper);
		Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(3, 1, 3));
		pig.addEffect(new MobEffectInstance(SnowflakeSpiderModule.WEBBED, MobEffectInstance.INFINITE_DURATION));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.runAfterDelay(3, () -> {
			WebEntity web = helper.getEntities(SnowflakeSpiderModule.WEB_ENTITY).getFirst();
			helper.assertFalse(web.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 5.0F), "the hit counts");
			helper.assertValueEqual(web.getHealth(), 5.0F, "web health");
			helper.succeed();
		});
	}

	/** A web sack falling straight down from {@code pos}. */
	private static void dropSack(GameTestHelper helper, BlockPos pos) {
		WebSackProjectile sack = helper.spawn(SnowflakeSpiderModule.WEB_SACK_PROJECTILE, pos);
		sack.setDeltaMovement(0.0, -1.0, 0.0);
	}

	private static boolean isWeb(GameTestHelper helper, int x, int y, int z) {
		return helper.getBlockState(new BlockPos(x, y, z)).is(SnowflakeSpiderModule.FREEZING_COBWEB);
	}

	private static int countWebs(GameTestHelper helper) {
		int webs = 0;
		for (int x = 0; x < 8; x++) {
			for (int y = 1; y < 8; y++) {
				for (int z = 0; z < 8; z++) {
					if (isWeb(helper, x, y, z)) webs++;
				}
			}
		}
		return webs;
	}
}
