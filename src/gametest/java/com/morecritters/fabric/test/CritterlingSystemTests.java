package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.CritterlingSystemIds;
import com.morecritters.fabric.ids.CritterlingsAIds;
import com.morecritters.fabric.ids.WanderingCollectorIds;
import com.morecritters.fabric.module.critterling_system.ConfettiPopperBlock;
import com.morecritters.fabric.module.critterling_system.ConfettiTrailBlock;
import com.morecritters.fabric.module.critterling_system.CritterEaterEntity;
import com.morecritters.fabric.module.critterling_system.CritterlingCollection;
import com.morecritters.fabric.module.critterling_system.CritterlingRarity;
import com.morecritters.fabric.module.critterling_system.CritterlingSystemModule;
import com.morecritters.fabric.module.critterling_system.EvoliteMawEntity;
import com.morecritters.fabric.module.critterling_system.EvolutionTableMenu;
import com.morecritters.fabric.module.critterlings_a.CritterlingsAModule;
import com.morecritters.fabric.module.critterlings_a.CubefrogEntity;
import com.morecritters.fabric.module.critterlings_a.DungerEntity;
import com.morecritters.fabric.module.critterlings_e.CritterlingsEModule;
import com.morecritters.fabric.module.critterlings_e.LightflyEntity;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The machinery shared by all critterlings: catching and releasing with sacks, the closed mystery
 * sack and gamble mode, the collection, dancing, the evolution table, the evolite chandelier, the
 * critter eater, the evolite maw, the lightfly strike and the confetti blocks.
 */
public class CritterlingSystemTests {
	private static final TagKey<Item> RANDOM_COMMON = itemTag("random_common"), RANDOM_RARE = itemTag("random_rare"), RANDOM_EPIC = itemTag("random_epic");

	// --- sacks ------------------------------------------------------------------------

	@GameTest
	public void anEmptySackCatchesACritterlingInItsRarity(GameTestHelper helper) {
		TestScenes.floor(helper);
		CubefrogEntity cubefrog = helper.spawn(CritterlingsAModule.CUBEFROG, new BlockPos(3, 1, 3));
		cubefrog.setRarity(CritterlingRarity.EPIC);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CritterlingSystemModule.CRITTERLING_SACK));

		InteractionResult result = rightClick(helper, player, cubefrog);
		helper.assertTrue(result.consumesAction(), "catching consumes the click");
		helper.assertTrue(cubefrog.isRemoved(), "the caught cubefrog is gone");
		helper.assertValueEqual(itemId(player.getMainHandItem()), CritterlingsAIds.Items.CRITTERLING_SACK_CUBEFROG_EPIC, "sack in hand");
		helper.succeed();
	}

	@GameTest
	public void anEmptySackDoesNotCatchOtherMobs(GameTestHelper helper) {
		TestScenes.floor(helper);
		Cow cow = helper.spawn(EntityTypes.COW, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CritterlingSystemModule.CRITTERLING_SACK));

		rightClick(helper, player, cow);
		helper.assertFalse(cow.isRemoved(), "the cow stays");
		helper.assertValueEqual(itemId(player.getMainHandItem()), CritterlingSystemIds.Items.CRITTERLING_SACK, "sack in hand");
		helper.succeed();
	}

	@GameTest
	public void aFilledSackReleasesBesideTheClickedFace(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(CritterlingsAIds.Items.CRITTERLING_SACK_DUNGER_RARE)));

		TestScenes.useItemOn(helper, player, new BlockPos(3, 1, 3), Direction.EAST);
		helper.assertValueEqual(itemId(player.getMainHandItem()), CritterlingSystemIds.Items.CRITTERLING_SACK, "empty sack left in hand");
		helper.assertEntityPresent(CritterlingsAModule.DUNGER, new BlockPos(4, 1, 3));
		DungerEntity dunger = helper.getEntities(CritterlingsAModule.DUNGER).getFirst();
		helper.assertValueEqual(dunger.rarity(), CritterlingRarity.RARE, "released dunger's rarity");
		helper.assertValueEqual(dunger.textureName(), "rare_dunger", "released dunger's texture");
		helper.succeed();
	}

	@GameTest
	public void aSackInTheOffHandReleasesNothing(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(item(CritterlingsAIds.Items.CRITTERLING_SACK_DUNGER)));
		BlockPos absolute = helper.absolutePos(new BlockPos(3, 0, 3));
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
		player.getOffhandItem().useOn(new UseOnContext(player, InteractionHand.OFF_HAND, hit));
		helper.assertEntityNotPresent(CritterlingsAModule.DUNGER);
		helper.assertValueEqual(itemId(player.getOffhandItem()), CritterlingsAIds.Items.CRITTERLING_SACK_DUNGER, "off-hand sack");
		helper.succeed();
	}

	@GameTest
	public void holdingAFilledSackCollectsItsCritterling(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.assertFalse(CritterlingCollection.hasCollected(player, CritterlingsAIds.Entities.SNEK), "snek collected before");
		player.getInventory().add(new ItemStack(item(CritterlingsAIds.Items.CRITTERLING_SACK_SNEK)));
		player.getInventory().tick();
		helper.assertTrue(CritterlingCollection.hasCollected(player, CritterlingsAIds.Entities.SNEK), "snek collected after holding its sack");
		helper.assertFalse(CritterlingCollection.hasCollected(player, CritterlingsAIds.Entities.DUNGER), "dunger collected");
		helper.succeed();
	}

	@GameTest
	public void aClosedSackOpensToAnEmptySackOrARandomCritterlingSack(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		int empty = 0, prizes = 0;
		for (int i = 0; i < 200; i++) {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CritterlingSystemModule.CLOSED_CRITTERLING_SACK));
			CritterlingSystemModule.CLOSED_CRITTERLING_SACK.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
			ItemStack got = player.getMainHandItem();
			if (got.is(CritterlingSystemModule.CRITTERLING_SACK)) {
				empty++;
			} else if (got.is(RANDOM_COMMON) || got.is(RANDOM_RARE) || got.is(RANDOM_EPIC)) {
				prizes++;
			} else {
				helper.assertTrue(false, "a closed sack opened to " + itemId(got) + ", neither an empty sack nor a #random_* sack");
			}
		}
		// A fair coin: 200 flips both land above 60 with overwhelming probability.
		helper.assertTrue(empty > 60 && prizes > 60, "both outcomes come up (empty " + empty + ", prizes " + prizes + ")");
		helper.succeed();
	}

	@GameTest
	public void gambleModeMakesNonCritterlingsDropAClosedSack(GameTestHelper helper) {
		TestScenes.floor(helper);
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		DungerEntity dunger = helper.spawnWithNoFreeWill(CritterlingsAModule.DUNGER, new BlockPos(6, 1, 6));
		Zombie ruleOffZombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(6, 1, 1));
		helper.kill(ruleOffZombie);
		// Gamble mode is a world-wide rule: switched on only around these two deaths, within this tick.
		setGambleMode(helper, true);
		try {
			helper.kill(zombie);
			helper.kill(dunger);
		} finally {
			setGambleMode(helper, false);
		}
		helper.assertItemEntityPresent(CritterlingSystemModule.CLOSED_CRITTERLING_SACK, new BlockPos(1, 1, 1), 1.0);
		helper.assertItemEntityNotPresent(CritterlingSystemModule.CLOSED_CRITTERLING_SACK, new BlockPos(6, 1, 6), 1.5);
		helper.assertItemEntityNotPresent(CritterlingSystemModule.CLOSED_CRITTERLING_SACK, new BlockPos(6, 1, 1), 1.5);
		helper.succeed();
	}

	// --- shared critterling behaviour -------------------------------------------------

	@GameTest
	public void critterlingsDanceToAJukeboxWithinReach(GameTestHelper helper) {
		TestScenes.floor(helper);
		// The original searched from -3 to +2 blocks on each axis around the critterling.
		DungerEntity near = helper.spawnWithNoFreeWill(CritterlingsAModule.DUNGER, new BlockPos(4, 1, 1));
		DungerEntity far = helper.spawnWithNoFreeWill(CritterlingsAModule.DUNGER, new BlockPos(1, 1, 6));
		helper.setBlock(new BlockPos(1, 1, 1), Blocks.JUKEBOX.defaultBlockState().setValue(JukeboxBlock.HAS_RECORD, true));
		helper.setBlock(new BlockPos(4, 1, 6), Blocks.JUKEBOX.defaultBlockState().setValue(JukeboxBlock.HAS_RECORD, true));
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(near.isDancing(), "a dunger 3 blocks east of a playing jukebox dances");
			helper.assertFalse(far.isDancing(), "a dunger 3 blocks west of a playing jukebox dances");
			helper.setBlock(new BlockPos(1, 1, 1), Blocks.JUKEBOX.defaultBlockState().setValue(JukeboxBlock.HAS_RECORD, false));
			helper.runAfterDelay(2, () -> {
				helper.assertFalse(near.isDancing(), "the dunger still dances after the disc is out");
				helper.succeed();
			});
		});
	}

	@GameTest
	public void critterlingsTakeNoFallOrDrowningDamage(GameTestHelper helper) {
		TestScenes.floor(helper);
		DungerEntity dunger = helper.spawnWithNoFreeWill(CritterlingsAModule.DUNGER, new BlockPos(3, 1, 3));
		float health = dunger.getHealth();
		helper.hurt(dunger, helper.getLevel().damageSources().fall(), 2.0F);
		helper.hurt(dunger, helper.getLevel().damageSources().drown(), 2.0F);
		helper.assertValueEqual(dunger.getHealth(), health, "health after falling and drowning");
		helper.hurt(dunger, helper.getLevel().damageSources().generic(), 1.0F);
		helper.assertValueEqual(dunger.getHealth(), health - 1.0F, "health after a generic hit");
		helper.succeed();
	}

	// --- evolution table --------------------------------------------------------------

	@GameTest
	public void theEvolutionTableEvolvesANormalSackToRare(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		EvolutionTableMenu menu = new EvolutionTableMenu(1, player.getInventory(),
			ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1))));
		menu.getSlot(EvolutionTableMenu.SACK_SLOT).set(new ItemStack(item(CritterlingsAIds.Items.CRITTERLING_SACK_CUBEFROG)));
		helper.assertTrue(menu.getSlot(EvolutionTableMenu.PREVIEW_SLOTS[0]).getItem().isEmpty(), "no preview without evolite");

		menu.getSlot(EvolutionTableMenu.EVOLITE_SLOT).set(new ItemStack(CritterlingSystemModule.EVOLITE, 3));
		helper.assertValueEqual(menu.getSlot(EvolutionTableMenu.PREVIEW_SLOTS[0]).getItem().getItem(), Items.INK_SAC, "first preview");
		helper.assertValueEqual(menu.getSlot(EvolutionTableMenu.PREVIEW_SLOTS[1]).getItem().getItem(), Items.HONEYCOMB, "second preview");
		helper.assertTrue(menu.getSlot(EvolutionTableMenu.PREVIEW_SLOTS[2]).getItem().isEmpty(), "third preview empty for a normal sack");

		menu.getSlot(EvolutionTableMenu.INGREDIENT_SLOTS[0]).set(new ItemStack(Items.INK_SAC, 2));
		helper.assertTrue(menu.getSlot(EvolutionTableMenu.RESULT_SLOT).getItem().isEmpty(), "no result with one ingredient");
		menu.getSlot(EvolutionTableMenu.INGREDIENT_SLOTS[1]).set(new ItemStack(Items.HONEYCOMB, 2));
		helper.assertValueEqual(itemId(menu.getSlot(EvolutionTableMenu.RESULT_SLOT).getItem()), CritterlingsAIds.Items.CRITTERLING_SACK_CUBEFROG_RARE, "result");

		menu.clicked(EvolutionTableMenu.RESULT_SLOT, 0, ContainerInput.PICKUP, player);
		helper.assertValueEqual(itemId(menu.getCarried()), CritterlingsAIds.Items.CRITTERLING_SACK_CUBEFROG_RARE, "taken result");
		helper.assertTrue(menu.getSlot(EvolutionTableMenu.SACK_SLOT).getItem().isEmpty(), "sack used up");
		helper.assertValueEqual(menu.getSlot(EvolutionTableMenu.EVOLITE_SLOT).getItem().getCount(), 2, "evolite left");
		helper.assertValueEqual(menu.getSlot(EvolutionTableMenu.INGREDIENT_SLOTS[0]).getItem().getCount(), 1, "ink sacs left");
		helper.assertValueEqual(menu.getSlot(EvolutionTableMenu.INGREDIENT_SLOTS[1]).getItem().getCount(), 1, "honeycomb left");
		helper.assertTrue(menu.getSlot(EvolutionTableMenu.RESULT_SLOT).getItem().isEmpty(), "no second result without a sack");
		helper.succeed();
	}

	@GameTest
	public void theEvolutionTableEvolvesARareSackToEpicWithThreeIngredients(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		EvolutionTableMenu menu = new EvolutionTableMenu(1, player.getInventory(),
			ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1))));
		menu.getSlot(EvolutionTableMenu.SACK_SLOT).set(new ItemStack(item(CritterlingsAIds.Items.CRITTERLING_SACK_CUBEFROG_RARE)));
		menu.getSlot(EvolutionTableMenu.EVOLITE_SLOT).set(new ItemStack(CritterlingSystemModule.EVOLITE));
		menu.getSlot(EvolutionTableMenu.INGREDIENT_SLOTS[0]).set(new ItemStack(Items.SLIME_BALL));
		menu.getSlot(EvolutionTableMenu.INGREDIENT_SLOTS[1]).set(new ItemStack(Items.QUARTZ));
		helper.assertValueEqual(menu.getSlot(EvolutionTableMenu.PREVIEW_SLOTS[2]).getItem().getItem(), Items.FERMENTED_SPIDER_EYE, "third preview");
		helper.assertTrue(menu.getSlot(EvolutionTableMenu.RESULT_SLOT).getItem().isEmpty(), "no result without the third ingredient");
		menu.getSlot(EvolutionTableMenu.INGREDIENT_SLOTS[2]).set(new ItemStack(Items.FERMENTED_SPIDER_EYE));
		helper.assertValueEqual(itemId(menu.getSlot(EvolutionTableMenu.RESULT_SLOT).getItem()), CritterlingsAIds.Items.CRITTERLING_SACK_CUBEFROG_EPIC, "result");
		menu.clicked(EvolutionTableMenu.RESULT_SLOT, 0, ContainerInput.PICKUP, player);
		for (int slot : new int[] {EvolutionTableMenu.SACK_SLOT, EvolutionTableMenu.EVOLITE_SLOT,
				EvolutionTableMenu.INGREDIENT_SLOTS[0], EvolutionTableMenu.INGREDIENT_SLOTS[1], EvolutionTableMenu.INGREDIENT_SLOTS[2]}) {
			helper.assertTrue(menu.getSlot(slot).getItem().isEmpty(), "menu slot " + slot + " used up");
		}
		helper.succeed();
	}

	@GameTest
	public void theEvolutionTableOnlyTakesEvolvableSacksAndEvolite(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		EvolutionTableMenu menu = new EvolutionTableMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
		helper.assertFalse(menu.getSlot(EvolutionTableMenu.SACK_SLOT).mayPlace(new ItemStack(item(CritterlingsAIds.Items.CRITTERLING_SACK_CUBEFROG_EPIC))), "epic sack accepted");
		helper.assertFalse(menu.getSlot(EvolutionTableMenu.SACK_SLOT).mayPlace(new ItemStack(Items.DIRT)), "dirt accepted as a sack");
		helper.assertFalse(menu.getSlot(EvolutionTableMenu.EVOLITE_SLOT).mayPlace(new ItemStack(Items.AMETHYST_SHARD)), "amethyst accepted as evolite");
		helper.assertTrue(menu.getSlot(EvolutionTableMenu.SACK_SLOT).mayPlace(new ItemStack(item(CritterlingsAIds.Items.CRITTERLING_SACK_CUBEFROG_RARE))), "rare sack accepted");
		helper.succeed();
	}

	// --- chandelier, critter eater, maw, lightfly -------------------------------------

	@GameTest
	public void theEvoliteChandelierMakesNearbyCritterlingsInvulnerable(GameTestHelper helper) {
		TestScenes.floor(helper);
		DungerEntity dunger = helper.spawnWithNoFreeWill(CritterlingsAModule.DUNGER, new BlockPos(2, 1, 2));
		Cow cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(5, 1, 5));
		helper.setBlock(new BlockPos(3, 5, 3), block(CritterlingSystemIds.Blocks.EVOLITE_CHANDELIER));
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(dunger.hasEffect(CritterlingSystemModule.EVOLIGHTENED), "the dunger is evolightened");
			helper.assertFalse(cow.hasEffect(CritterlingSystemModule.EVOLIGHTENED), "the cow is evolightened");
			float health = dunger.getHealth();
			helper.hurt(dunger, helper.getLevel().damageSources().generic(), 2.0F);
			helper.assertValueEqual(dunger.getHealth(), health, "the evolightened dunger's health after a hit");
			helper.succeed();
		});
	}

	@GameTest
	public void theCritterEaterIsCaughtInItsOwnSack(GameTestHelper helper) {
		TestScenes.floor(helper);
		CritterEaterEntity eater = helper.spawnWithNoFreeWill(CritterlingSystemModule.CRITTER_EATER, new BlockPos(2, 1, 2));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CritterlingSystemModule.CRITTERLING_SACK));
		rightClick(helper, player, eater);
		helper.assertTrue(eater.isRemoved(), "the critter eater is in the sack");
		helper.assertValueEqual(itemId(player.getMainHandItem()), CritterlingSystemIds.Items.CRITTERLING_SACK_CRITTER_EATER, "sack in hand");

		TestScenes.useItemOn(helper, player, new BlockPos(5, 0, 5), Direction.UP);
		helper.assertEntityPresent(CritterlingSystemModule.CRITTER_EATER, new BlockPos(5, 1, 5));
		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void theCritterEaterEatsACritterlingInReach(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.spawn(CritterlingSystemModule.CRITTER_EATER, new BlockPos(3, 1, 3), EntitySpawnReason.MOB_SUMMONED);
		DungerEntity dunger = helper.spawnWithNoFreeWill(CritterlingsAModule.DUNGER, new BlockPos(4, 1, 3));
		// 10 attack damage against a 3-health dunger.
		helper.succeedWhen(() -> helper.assertTrue(dunger.isDeadOrDying(), "the dunger was eaten"));
	}

	@GameTest(maxTicks = 100)
	public void anEvoliteMawBitesWhatStandsOnItsSpotButSparesIllagers(GameTestHelper helper) {
		TestScenes.floor(helper);
		// Cows rather than zombies: the test world is in daylight.
		Cow cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(3, 1, 3));
		Pillager pillager = helper.spawnWithNoFreeWill(EntityTypes.PILLAGER, new BlockPos(3, 1, 3));
		Cow bystander = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(5, 1, 3));
		EvoliteMawEntity maw = helper.spawn(CritterlingSystemModule.EVOLITE_MAW, new BlockPos(3, 1, 3), EntitySpawnReason.MOB_SUMMONED);
		// Rises after 10 ticks, bites 15 ticks later.
		helper.startSequence()
			.thenIdle(20)
			.thenExecute(() -> helper.assertValueEqual(cow.getHealth(), cow.getMaxHealth(), "cow health before the bite"))
			.thenIdle(10)
			.thenExecute(() -> {
				helper.assertValueEqual(cow.getHealth(), cow.getMaxHealth() - 7.0F, "cow health after the bite");
				helper.assertValueEqual(pillager.getHealth(), pillager.getMaxHealth(), "pillager health");
				helper.assertValueEqual(bystander.getHealth(), bystander.getMaxHealth(), "health of a cow two blocks away");
			})
			.thenWaitUntil(() -> helper.assertTrue(maw.isRemoved(), "the maw is gone after its 40 ticks"))
			.thenSucceed();
	}

	@GameTest
	public void aLightflyBurnsOutOnItsVictim(GameTestHelper helper) {
		TestScenes.floor(helper);
		Cow cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(3, 1, 3));
		LightflyEntity lightfly = helper.spawn(CritterlingsEModule.LIGHTFLY, new BlockPos(4, 2, 3));
		helper.hurt(cow, helper.getLevel().damageSources().mobAttack(lightfly), 1.0F);
		helper.assertTrue(lightfly.isRemoved(), "the lightfly is gone");
		helper.assertTrue(cow.getRemainingFireTicks() > 0, "the cow is on fire");
		helper.succeed();
	}

	// --- party blocks -----------------------------------------------------------------

	@GameTest
	public void theConfettiPopperPopsWhilePowered(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos popper = new BlockPos(3, 1, 3);
		helper.setBlock(popper, CritterlingSystemModule.CONFETTI_POPPER);
		helper.assertBlockProperty(popper, ConfettiPopperBlock.POPPED, false);
		helper.setBlock(new BlockPos(4, 1, 3), Blocks.REDSTONE_BLOCK);
		helper.assertBlockProperty(popper, ConfettiPopperBlock.POPPED, true);
		helper.setBlock(new BlockPos(4, 1, 3), Blocks.AIR);
		helper.assertBlockProperty(popper, ConfettiPopperBlock.POPPED, false);
		helper.succeed();
	}

	@GameTest
	public void confettiTrailPicksARandomPatternAndNeedsSupport(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		Set<Integer> patterns = new HashSet<>();
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(CritterlingSystemIds.Blocks.CONFETTI_TRAIL)));
				TestScenes.useItemOn(helper, player, new BlockPos(x, 0, z), Direction.UP);
				helper.assertBlockPresent(block(CritterlingSystemIds.Blocks.CONFETTI_TRAIL), new BlockPos(x, 1, z));
				patterns.add(helper.getBlockState(new BlockPos(x, 1, z)).getValue(ConfettiTrailBlock.PATTERN));
			}
		}
		helper.assertValueEqual(patterns, Set.of(0, 1, 2), "patterns over 64 placements");

		// A trail cannot sit on another trail, and breaks when its floor goes.
		helper.assertFalse(helper.getBlockState(new BlockPos(3, 1, 3)).canSurvive(helper.getLevel(), helper.absolutePos(new BlockPos(3, 2, 3))), "trail on a trail survives");
		helper.setBlock(new BlockPos(3, 0, 3), Blocks.AIR);
		helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, 1, 3));
		helper.succeed();
	}

	// --- helpers ----------------------------------------------------------------------

	/** A right-click on an entity, the way the server's interaction handler fires it. */
	private static InteractionResult rightClick(GameTestHelper helper, Player player, Entity target) {
		return UseEntityCallback.EVENT.invoker().interact(player, helper.getLevel(), InteractionHand.MAIN_HAND, target, null);
	}

	@SuppressWarnings("unchecked")
	private static void setGambleMode(GameTestHelper helper, boolean on) {
		GameRule<Boolean> rule = (GameRule<Boolean>) BuiltInRegistries.GAME_RULE.getValue(WanderingCollectorIds.GameRules.GAMBLEMODE);
		helper.getLevel().getGameRules().set(rule, on, helper.getLevel().getServer());
	}

	private static Item item(Identifier id) {
		return BuiltInRegistries.ITEM.getValue(id);
	}

	private static Block block(Identifier id) {
		return BuiltInRegistries.BLOCK.getValue(id);
	}

	private static Identifier itemId(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem());
	}

	private static TagKey<Item> itemTag(String name) {
		return TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace(name));
	}
}
