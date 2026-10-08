package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.CritterlingsCIds;
import com.morecritters.fabric.module.critterling_system.Critterling;
import com.morecritters.fabric.module.critterling_system.CritterlingRarity;
import com.morecritters.fabric.module.critterling_system.CritterlingSystemModule;
import com.morecritters.fabric.module.critterlings_c.CritterlingsCModule;
import com.morecritters.fabric.module.critterlings_c.MothkidEntity;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;

/** Mothkid, gillmunch, dominic and olmer: the mothkid's hops, drowning, rarity looks and their sacks. */
public class CritterlingsCTests {
	private static final BlockPos MIDDLE = new BlockPos(3, 1, 3);

	@GameTest(maxTicks = 300)
	public void theMothkidHopsAfterTwoSecondsAndAgainFiveToTenSecondsLater(GameTestHelper helper) {
		TestScenes.floor(helper);
		pen(helper, MIDDLE);
		MothkidEntity mothkid = helper.spawn(CritterlingsCModule.MOTHKID, MIDDLE);
		double ground = helper.absolutePos(MIDDLE).getY();
		long[] hops = {-1, -1};
		boolean[] airborne = {false};
		helper.onEachTick(() -> {
			boolean up = mothkid.getY() > ground + 0.5;
			if (up && !airborne[0]) {
				if (hops[0] < 0) hops[0] = helper.getTick();
				else if (hops[1] < 0) hops[1] = helper.getTick();
			}
			airborne[0] = up;
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(hops[1] >= 0, "the mothkid has hopped twice");
			helper.assertTrue(hops[0] >= 35 && hops[0] <= 50, "first hop about 2 s after appearing, was at tick " + hops[0]);
			long gap = hops[1] - hops[0];
			helper.assertTrue(gap >= 95 && gap <= 205, "second hop 5-10 s after the first, was " + gap + " ticks");
		});
	}

	@GameTest(maxTicks = 120)
	public void aDancingMothkidStaysOnTheGround(GameTestHelper helper) {
		TestScenes.floor(helper);
		pen(helper, MIDDLE);
		helper.setBlock(MIDDLE.east(), Blocks.JUKEBOX.defaultBlockState().setValue(JukeboxBlock.HAS_RECORD, true));
		MothkidEntity mothkid = helper.spawn(CritterlingsCModule.MOTHKID, MIDDLE);
		double ground = helper.absolutePos(MIDDLE).getY();
		helper.onEachTick(() -> helper.assertTrue(mothkid.getY() < ground + 0.5, "a dancing mothkid does not hop"));
		helper.runAtTickTime(100, () -> {
			helper.assertTrue(mothkid.isDancing(), "the mothkid dances by a playing jukebox");
			helper.succeed();
		});
	}

	@GameTest
	public void mothkidAndGillmunchDrownButDominicAndOlmerDoNot(GameTestHelper helper) {
		TestScenes.floor(helper);
		Map<EntityType<? extends Critterling>, Float> expected = Map.of(
			CritterlingsCModule.MOTHKID, 2.0F, CritterlingsCModule.GILLMUNCH, 2.0F,
			CritterlingsCModule.DOMINIC, 3.0F, CritterlingsCModule.OLMER, 3.0F);
		int x = 1;
		for (var entry : expected.entrySet()) {
			Critterling critterling = helper.spawnWithNoFreeWill(entry.getKey(), new BlockPos(x, 1, 3));
			x += 2;
			helper.hurt(critterling, helper.getLevel().damageSources().drown(), 1.0F);
			helper.assertValueEqual(critterling.getHealth(), entry.getValue(), "health of a " + entry.getKey().getDescriptionId() + " after drowning damage");
		}
		helper.succeed();
	}

	@GameTest
	public void noneOfThemTakeFallDamageOrDespawn(GameTestHelper helper) {
		TestScenes.floor(helper);
		int x = 1;
		for (EntityType<? extends Critterling> type : List.of(CritterlingsCModule.MOTHKID, CritterlingsCModule.GILLMUNCH, CritterlingsCModule.DOMINIC, CritterlingsCModule.OLMER)) {
			Critterling critterling = helper.spawnWithNoFreeWill(type, new BlockPos(x, 1, 3));
			x += 2;
			helper.hurt(critterling, helper.getLevel().damageSources().fall(), 2.0F);
			helper.assertValueEqual(critterling.getHealth(), 3.0F, "health of a " + type.getDescriptionId() + " after a fall");
			helper.assertFalse(critterling.removeWhenFarAway(1000.0), type.getDescriptionId() + " despawns far from players");
		}
		helper.succeed();
	}

	@GameTest
	public void eachRarityHasItsOwnLook(GameTestHelper helper) {
		TestScenes.floor(helper);
		assertTextures(helper, CritterlingsCModule.MOTHKID, "mothkid", "rare_mothkid", "epic_mothkid");
		assertTextures(helper, CritterlingsCModule.GILLMUNCH, "gillmunch", "rare_gillmunch", "epic_gillmunch");
		assertTextures(helper, CritterlingsCModule.DOMINIC, "dominic", "dominic_rare", "dominic_epic");
		assertTextures(helper, CritterlingsCModule.OLMER, "olmer", "olmer_rare", "olmer_epic");
		helper.succeed();
	}

	@GameTest
	public void anEpicMothkidSackReleasesAnEpicMothkidThatCanBeCaughtAgain(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(CritterlingsCIds.Items.CRITTERLING_SACK_MOTHKID_EPIC)));
		TestScenes.useItemOn(helper, player, new BlockPos(3, 0, 3), Direction.UP);
		helper.assertValueEqual(player.getMainHandItem().getItem(), CritterlingSystemModule.CRITTERLING_SACK, "item left in hand after releasing");
		MothkidEntity mothkid = helper.findOneEntity(CritterlingsCModule.MOTHKID);
		helper.assertValueEqual(mothkid.blockPosition(), helper.absolutePos(MIDDLE), "where the mothkid appears");
		helper.assertValueEqual(mothkid.rarity(), CritterlingRarity.EPIC, "rarity of the released mothkid");

		UseEntityCallback.EVENT.invoker().interact(player, helper.getLevel(), InteractionHand.MAIN_HAND, mothkid, null);
		helper.assertTrue(mothkid.isRemoved(), "the caught mothkid is gone");
		helper.assertValueEqual(player.getMainHandItem().getItem(), item(CritterlingsCIds.Items.CRITTERLING_SACK_MOTHKID_EPIC), "sack after catching");
		helper.succeed();
	}

	@GameTest
	public void caughtDominicsAndOlmersGoIntoTheSackForTheirRarity(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Critterling dominic = helper.spawnWithNoFreeWill(CritterlingsCModule.DOMINIC, new BlockPos(2, 1, 3));
		dominic.setRarity(CritterlingRarity.RARE);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CritterlingSystemModule.CRITTERLING_SACK));
		UseEntityCallback.EVENT.invoker().interact(player, helper.getLevel(), InteractionHand.MAIN_HAND, dominic, null);
		helper.assertValueEqual(player.getMainHandItem().getItem(), item(CritterlingsCIds.Items.CRITTERLING_SACK_DOMINIC_RARE), "sack for a rare dominic");

		Critterling olmer = helper.spawnWithNoFreeWill(CritterlingsCModule.OLMER, new BlockPos(5, 1, 3));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CritterlingSystemModule.CRITTERLING_SACK));
		UseEntityCallback.EVENT.invoker().interact(player, helper.getLevel(), InteractionHand.MAIN_HAND, olmer, null);
		helper.assertValueEqual(player.getMainHandItem().getItem(), item(CritterlingsCIds.Items.CRITTERLING_SACK_OLMER), "sack for a normal olmer");
		helper.succeed();
	}

	@GameTest
	public void sacksAreUnstackableAndRatedUncommonRareEpic(GameTestHelper helper) {
		List<Identifier[]> sacks = List.of(
			new Identifier[] {CritterlingsCIds.Items.CRITTERLING_SACK_MOTHKID, CritterlingsCIds.Items.CRITTERLING_SACK_MOTHKID_RARE, CritterlingsCIds.Items.CRITTERLING_SACK_MOTHKID_EPIC},
			new Identifier[] {CritterlingsCIds.Items.CRITTERLING_SACK_GILLMUNCH, CritterlingsCIds.Items.CRITTERLING_SACK_GILLMUNCH_RARE, CritterlingsCIds.Items.CRITTERLING_SACK_GILLMUNCH_EPIC},
			new Identifier[] {CritterlingsCIds.Items.CRITTERLING_SACK_DOMINIC, CritterlingsCIds.Items.CRITTERLING_SACK_DOMINIC_RARE, CritterlingsCIds.Items.CRITTERLING_SACK_DOMINIC_EPIC},
			new Identifier[] {CritterlingsCIds.Items.CRITTERLING_SACK_OLMER, CritterlingsCIds.Items.CRITTERLING_SACK_OLMER_RARE, CritterlingsCIds.Items.CRITTERLING_SACK_OLMER_EPIC});
		Rarity[] rarities = {Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC};
		for (Identifier[] forms : sacks) {
			for (int i = 0; i < 3; i++) {
				ItemStack stack = new ItemStack(item(forms[i]));
				helper.assertValueEqual(stack.get(DataComponents.RARITY), rarities[i], "rarity of " + forms[i]);
				helper.assertValueEqual(stack.getMaxStackSize(), 1, "stack size of " + forms[i]);
			}
		}
		helper.succeed();
	}

	private static void assertTextures(GameTestHelper helper, EntityType<? extends Critterling> type, String normal, String rare, String epic) {
		Critterling critterling = helper.spawnWithNoFreeWill(type, MIDDLE);
		helper.assertValueEqual(critterling.textureName(), normal, "normal texture");
		critterling.setRarity(CritterlingRarity.RARE);
		helper.assertValueEqual(critterling.textureName(), rare, "rare texture");
		critterling.setRarity(CritterlingRarity.EPIC);
		helper.assertValueEqual(critterling.textureName(), epic, "epic texture");
		critterling.discard();
	}

	/** Glass walls three blocks high around one block, so a hopping mothkid cannot wander off. */
	private static void pen(GameTestHelper helper, BlockPos inside) {
		for (Direction side : Direction.Plane.HORIZONTAL) {
			for (int y = 0; y < 3; y++) {
				helper.setBlock(inside.relative(side).above(y), Blocks.GLASS);
			}
		}
	}

	private static Item item(Identifier id) {
		return BuiltInRegistries.ITEM.getValue(id);
	}
}
