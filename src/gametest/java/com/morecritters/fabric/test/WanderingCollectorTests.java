package com.morecritters.fabric.test;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.ids.BunbugIds;
import com.morecritters.fabric.ids.CorpseGearIds;
import com.morecritters.fabric.ids.FossilsIds;
import com.morecritters.fabric.ids.ShriekbatIds;
import com.morecritters.fabric.ids.SnowflakeSpiderIds;
import com.morecritters.fabric.ids.TreepletIds;
import com.morecritters.fabric.module.wandering_collector.CarrybugEntity;
import com.morecritters.fabric.module.wandering_collector.CarrybugNoSaddleEntity;
import com.morecritters.fabric.module.wandering_collector.CarrybugRegistry;
import com.morecritters.fabric.module.wandering_collector.WanderingCollector;
import com.morecritters.fabric.module.wandering_collector.WanderingCollectorEntity;
import com.morecritters.fabric.module.wandering_collector.WanderingCollectorMenu;
import com.morecritters.fabric.module.wandering_collector.WanderingCollectorModule;
import com.morecritters.fabric.module.wandering_collector.WanderingCollectorTrades;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** The wandering collector's trades, his carrybug, and the critter kebab. */
public class WanderingCollectorTests {
	private static final BlockPos COLLECTOR = new BlockPos(3, 1, 3);

	@GameTest
	public void theCollectorPaysByHisPriceTable(GameTestHelper helper) {
		price(helper, BunbugIds.Items.BUNBUG_EGGS, new ItemStack(Items.EMERALD, 1));
		price(helper, ShriekbatIds.Items.SHRIEK_BOMB, new ItemStack(Items.EMERALD, 2));
		price(helper, SnowflakeSpiderIds.Items.SACKOF_FREEZING, new ItemStack(Items.EMERALD, 3));
		// Reached only since the port straightened out the original's nested chain (NOTES "Deviations").
		price(helper, TreepletIds.Items.BIRCH_SNOW_CONE, new ItemStack(Items.EMERALD, 2));
		price(helper, FossilsIds.Items.CRITTER_FOSSIL_1, new ItemStack(Items.DIAMOND, 3));
		price(helper, CorpseGearIds.Items.PIRATE_HELMET, new ItemStack(Items.GOLD_INGOT, 3));
		helper.assertTrue(WanderingCollectorTrades.priceFor(new ItemStack(Items.DIRT)).isEmpty(), "dirt is not bought");
		helper.succeed();
	}

	@GameTest
	public void tradeAllPaysCountTimesPriceAndTakesTheGoods(GameTestHelper helper) {
		TestScenes.floor(helper);
		WanderingCollectorEntity collector = helper.spawnWithNoFreeWill(WanderingCollector.ENTITY, COLLECTOR);
		ServerPlayer player = playerInLevel(helper);
		WanderingCollectorMenu menu = new WanderingCollectorMenu(1, player.getInventory(), collector);
		menu.getSlot(0).set(new ItemStack(item(TreepletIds.Items.BLACK_RESIN_CLUMP), 40));
		helper.assertTrue(ItemStack.matches(menu.getSlot(1).getItem(), new ItemStack(Items.EMERALD, 2)), "the price shows 2 emeralds, got " + menu.getSlot(1).getItem());
		menu.clickMenuButton(player, WanderingCollectorMenu.TRADE_BUTTON);
		helper.assertValueEqual(player.getInventory().countItem(Items.EMERALD), 80, "emeralds paid for 40 clumps at 2 each");
		helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "the goods are taken");
		helper.assertTrue(isDone(helper, player, "trade_with_collector"), "trade_with_collector is awarded");
		helper.assertTrue(helper.getLevel().getEntities(EntityTypes.EXPERIENCE_ORB, collector.getBoundingBox().inflate(1.0), orb -> true).size() >= 1,
			"experience drops at the collector");
		leave(helper, player);
		helper.succeed();
	}

	@GameTest
	public void sellingFiveDifferentItemsEarnsTheCarrybugAdvancement(GameTestHelper helper) {
		TestScenes.floor(helper);
		WanderingCollectorEntity collector = helper.spawnWithNoFreeWill(WanderingCollector.ENTITY, COLLECTOR);
		ServerPlayer player = playerInLevel(helper);
		WanderingCollectorMenu menu = new WanderingCollectorMenu(1, player.getInventory(), collector);
		List<Identifier> goods = List.of(BunbugIds.Items.BUNBUG_EGGS, TreepletIds.Items.EERIE_DART, ShriekbatIds.Items.SHRIEKBAT_WING,
			SnowflakeSpiderIds.Items.FREEZING_STRING, TreepletIds.Items.BLACK_RESIN_CLUMP);
		for (int i = 0; i < goods.size(); i++) {
			// Selling the same thing again does not count.
			sell(player, menu, BunbugIds.Items.BUNBUG_EGGS);
			helper.assertFalse(isDone(helper, player, "gain_access_to_carrybug"), "awarded after " + i + " kinds");
			sell(player, menu, goods.get(i));
		}
		helper.assertTrue(isDone(helper, player, "gain_access_to_carrybug"), "gain_access_to_carrybug is awarded after five kinds");
		leave(helper, player);
		helper.succeed();
	}

	@GameTest
	public void closingTheTradeScreenGivesBackWhatIsLeftInTheSlot(GameTestHelper helper) {
		TestScenes.floor(helper);
		WanderingCollectorEntity collector = helper.spawnWithNoFreeWill(WanderingCollector.ENTITY, COLLECTOR);
		ServerPlayer player = playerInLevel(helper);
		WanderingCollectorMenu menu = new WanderingCollectorMenu(1, player.getInventory(), collector);
		menu.getSlot(0).set(new ItemStack(Items.DIRT, 7));
		helper.assertTrue(menu.getSlot(1).getItem().isEmpty(), "no price for dirt");
		menu.clickMenuButton(player, WanderingCollectorMenu.TRADE_BUTTON);
		menu.removed(player);
		helper.assertValueEqual(player.getInventory().countItem(Items.DIRT), 7, "dirt given back");
		helper.assertValueEqual(player.getInventory().countItem(Items.EMERALD), 0, "emeralds paid for dirt");
		leave(helper, player);
		helper.succeed();
	}

	@GameTest
	public void aCarrybugKicksOffARiderItDoesNotTrust(GameTestHelper helper) {
		TestScenes.floor(helper);
		CarrybugEntity bug = helper.spawnWithNoFreeWill(CarrybugRegistry.CARRYBUG, COLLECTOR);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(new Vec3(1.5, 1.0, 1.5)), 0.0F, 0.0F);
		bug.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
		helper.assertTrue(player.getVehicle() == bug, "the player climbs on");
		helper.succeedWhen(() -> helper.assertTrue(player.getVehicle() == null, "the player is kicked off"));
	}

	@GameTest
	public void shearsTakeTheSaddleAndChestsOffACarrybug(GameTestHelper helper) {
		TestScenes.floor(helper);
		CarrybugEntity bug = helper.spawnWithNoFreeWill(CarrybugRegistry.CARRYBUG, COLLECTOR);
		bug.setVariant("desert");
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
		bug.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
		helper.assertTrue(bug.isRemoved(), "the saddled carrybug is replaced");
		helper.assertTrue(player.getVehicle() == null, "the shearer is not riding");
		helper.assertItemEntityCountIs(Items.SADDLE, COLLECTOR, 2.0, 1);
		helper.assertItemEntityCountIs(Blocks.CHEST.asItem(), COLLECTOR, 2.0, 3);
		List<CarrybugNoSaddleEntity> bare = helper.getEntities(CarrybugRegistry.CARRYBUG_NO_SADDLE);
		helper.assertValueEqual(bare.size(), 1, "bare carrybugs");
		helper.assertValueEqual(bare.getFirst().variant(), "desert", "the bare carrybug's colour");
		helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 1, "shears damage");
		helper.succeed();
	}

	@GameTest
	public void aCritterKebabIsEatenInThreeBitesDownToTheStick(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.getFoodData().setFoodLevel(0);
		ItemStack kebab = new ItemStack(WanderingCollectorModule.CRITTER_KEBAB);
		kebab = kebab.finishUsingItem(helper.getLevel(), player);
		helper.assertTrue(kebab.is(WanderingCollectorModule.CRITTER_KEBAB_2), "after one bite: " + kebab);
		kebab = kebab.finishUsingItem(helper.getLevel(), player);
		helper.assertTrue(kebab.is(WanderingCollectorModule.CRITTER_KEBAB_3), "after two bites: " + kebab);
		kebab = kebab.finishUsingItem(helper.getLevel(), player);
		helper.assertTrue(kebab.is(Items.STICK), "after three bites: " + kebab);
		helper.assertValueEqual(player.getFoodData().getFoodLevel(), 18, "food level after three bites of 6");
		helper.succeed();
	}

	private static void price(GameTestHelper helper, Identifier id, ItemStack expected) {
		Item item = item(id);
		helper.assertFalse(item == Items.AIR, id + " is registered");
		ItemStack price = WanderingCollectorTrades.priceFor(new ItemStack(item));
		helper.assertTrue(ItemStack.matches(price, expected), "price of " + id + ": " + price + ", expected " + expected);
	}

	private static void sell(ServerPlayer player, WanderingCollectorMenu menu, Identifier id) {
		menu.getSlot(0).set(new ItemStack(item(id)));
		menu.clickMenuButton(player, WanderingCollectorMenu.TRADE_BUTTON);
	}

	/**
	 * A player joined to the server (advancements are sent over its connection). Creative, so the monsters
	 * of neighbouring tests leave it alone; it leaves the server when the test ends.
	 */
	@SuppressWarnings("removal")
	private static ServerPlayer playerInLevel(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.snapTo(helper.absoluteVec(new Vec3(1.5, 1.0, 1.5)), 0.0F, 0.0F);
		helper.runBeforeTestEnd(() -> leave(helper, player));
		return player;
	}

	private static void leave(GameTestHelper helper, ServerPlayer player) {
		if (!player.isRemoved()) helper.getLevel().getServer().getPlayerList().remove(player);
	}

	private static boolean isDone(GameTestHelper helper, ServerPlayer player, String advancement) {
		AdvancementHolder holder = helper.getLevel().getServer().getAdvancements().get(MoreCritters.id(advancement));
		helper.assertTrue(holder != null, advancement + " exists");
		return player.getAdvancements().getOrStartProgress(holder).isDone();
	}

	private static Item item(Identifier id) {
		return BuiltInRegistries.ITEM.getValue(id);
	}
}
