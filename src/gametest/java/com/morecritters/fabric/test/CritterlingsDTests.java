package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.CritterlingsDIds;
import com.morecritters.fabric.module.critterling_system.Critterling;
import com.morecritters.fabric.module.critterling_system.CritterlingRarity;
import com.morecritters.fabric.module.critterling_system.CritterlingSystemModule;
import com.morecritters.fabric.module.critterlings_d.CritterlingsDModule;
import java.util.List;
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
import net.minecraft.world.phys.Vec3;

/** Flarg, stalk, piranheed and mangotrice: rarity looks, hardiness, dancing, and their sacks. */
public class CritterlingsDTests {
	private static final BlockPos MIDDLE = new BlockPos(3, 1, 3);

	@GameTest
	public void eachRarityHasItsOwnLook(GameTestHelper helper) {
		TestScenes.floor(helper);
		assertTextures(helper, CritterlingsDModule.FLARG, "flarg", "flarg_rare", "flarg_epic");
		assertTextures(helper, CritterlingsDModule.STALK, "stalk", "rare_stalk", "epic_stalk");
		assertTextures(helper, CritterlingsDModule.PIRANHEED, "piranheed", "piranheed_rare", "piranheed_epic");
		assertTextures(helper, CritterlingsDModule.MANGOTRICE, "mangotrice", "mangotrice_rare", "mangotrice_epic");
		helper.succeed();
	}

	@GameTest
	public void noneOfThemDrownTakeFallDamageOrDespawn(GameTestHelper helper) {
		TestScenes.floor(helper);
		int x = 1;
		for (EntityType<? extends Critterling> type : List.of(CritterlingsDModule.FLARG, CritterlingsDModule.STALK, CritterlingsDModule.PIRANHEED, CritterlingsDModule.MANGOTRICE)) {
			Critterling critterling = helper.spawnWithNoFreeWill(type, new BlockPos(x, 1, 3));
			x += 2;
			helper.assertValueEqual(critterling.getMaxHealth(), 3.0F, "max health of " + type.getDescriptionId());
			helper.hurt(critterling, helper.getLevel().damageSources().fall(), 2.0F);
			helper.hurt(critterling, helper.getLevel().damageSources().drown(), 2.0F);
			helper.assertValueEqual(critterling.getHealth(), 3.0F, "health of " + type.getDescriptionId() + " after falling and drowning");
			helper.assertFalse(critterling.removeWhenFarAway(1000.0), type.getDescriptionId() + " despawns far from players");
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 140)
	public void aFlargByAPlayingJukeboxDancesOnTheSpot(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(MIDDLE.east(2), Blocks.JUKEBOX.defaultBlockState().setValue(JukeboxBlock.HAS_RECORD, true));
		Critterling flarg = helper.spawn(CritterlingsDModule.FLARG, MIDDLE);
		Vec3 start = flarg.position();
		helper.onEachTick(() -> helper.assertTrue(flarg.position().subtract(start).horizontalDistance() < 0.05, "a dancing flarg does not wander"));
		helper.runAtTickTime(120, () -> {
			helper.assertTrue(flarg.isDancing(), "the flarg dances to the jukebox two blocks away");
			helper.succeed();
		});
	}

	@GameTest
	public void theFlargStopsDancingWhenTheDiscIsTakenOut(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos jukebox = MIDDLE.west(2);
		helper.setBlock(jukebox, Blocks.JUKEBOX.defaultBlockState().setValue(JukeboxBlock.HAS_RECORD, true));
		Critterling flarg = helper.spawnWithNoFreeWill(CritterlingsDModule.FLARG, MIDDLE);
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(flarg.isDancing(), "dancing while the disc plays"))
			.thenExecute(() -> helper.setBlock(jukebox, Blocks.JUKEBOX.defaultBlockState()))
			.thenWaitUntil(() -> helper.assertFalse(flarg.isDancing(), "dancing after the disc is out"))
			.thenSucceed();
	}

	@GameTest
	public void aRareFlargGoesBackIntoItsRareSackAndComesOutRare(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Critterling flarg = helper.spawnWithNoFreeWill(CritterlingsDModule.FLARG, new BlockPos(1, 1, 1));
		flarg.setRarity(CritterlingRarity.RARE);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CritterlingSystemModule.CRITTERLING_SACK));
		UseEntityCallback.EVENT.invoker().interact(player, helper.getLevel(), InteractionHand.MAIN_HAND, flarg, null);
		helper.assertTrue(flarg.isRemoved(), "the caught flarg is gone");
		helper.assertValueEqual(player.getMainHandItem().getItem(), item(CritterlingsDIds.Items.CRITTERLING_SACK_FLARG_RARE), "sack after catching a rare flarg");

		TestScenes.useItemOn(helper, player, new BlockPos(3, 0, 3), Direction.UP);
		Critterling released = helper.findOneEntity(CritterlingsDModule.FLARG);
		helper.assertValueEqual(released.blockPosition(), helper.absolutePos(MIDDLE), "where the flarg comes out");
		helper.assertValueEqual(released.rarity(), CritterlingRarity.RARE, "rarity of the released flarg");
		helper.assertValueEqual(player.getMainHandItem().getItem(), CritterlingSystemModule.CRITTERLING_SACK, "item left in hand");
		helper.succeed();
	}

	@GameTest
	public void epicStalksPiranheedsAndMangotricesComeOutOfTheirSacksEpic(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		List<Identifier> sacks = List.of(CritterlingsDIds.Items.CRITTERLING_SACK_STALK_EPIC,
			CritterlingsDIds.Items.CRITTERLING_SACK_PIRANHEED_EPIC, CritterlingsDIds.Items.CRITTERLING_SACK_MANGOTRICE_EPIC);
		List<EntityType<? extends Critterling>> types = List.of(CritterlingsDModule.STALK, CritterlingsDModule.PIRANHEED, CritterlingsDModule.MANGOTRICE);
		for (int i = 0; i < sacks.size(); i++) {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(sacks.get(i))));
			TestScenes.useItemOn(helper, player, new BlockPos(1 + 2 * i, 0, 3), Direction.UP);
			Critterling released = helper.findOneEntity(types.get(i));
			helper.assertValueEqual(released.rarity(), CritterlingRarity.EPIC, "rarity of the released " + types.get(i).getDescriptionId());
		}
		helper.succeed();
	}

	@GameTest
	public void sackRaritiesAreUncommonExceptTheStalksRareAndEpicSacks(GameTestHelper helper) {
		assertSack(helper, CritterlingsDIds.Items.CRITTERLING_SACK_FLARG, Rarity.UNCOMMON);
		assertSack(helper, CritterlingsDIds.Items.CRITTERLING_SACK_FLARG_RARE, Rarity.UNCOMMON);
		assertSack(helper, CritterlingsDIds.Items.CRITTERLING_SACK_FLARG_EPIC, Rarity.UNCOMMON);
		assertSack(helper, CritterlingsDIds.Items.CRITTERLING_SACK_STALK, Rarity.UNCOMMON);
		assertSack(helper, CritterlingsDIds.Items.CRITTERLING_SACK_STALK_RARE, Rarity.RARE);
		assertSack(helper, CritterlingsDIds.Items.CRITTERLING_SACK_STALK_EPIC, Rarity.EPIC);
		assertSack(helper, CritterlingsDIds.Items.CRITTERLING_SACK_PIRANHEED, Rarity.UNCOMMON);
		assertSack(helper, CritterlingsDIds.Items.CRITTERLING_SACK_PIRANHEED_RARE, Rarity.UNCOMMON);
		assertSack(helper, CritterlingsDIds.Items.CRITTERLING_SACK_PIRANHEED_EPIC, Rarity.UNCOMMON);
		assertSack(helper, CritterlingsDIds.Items.CRITTERLING_SACK_MANGOTRICE, Rarity.UNCOMMON);
		assertSack(helper, CritterlingsDIds.Items.CRITTERLING_SACK_MANGOTRICE_RARE, Rarity.UNCOMMON);
		assertSack(helper, CritterlingsDIds.Items.CRITTERLING_SACK_MANGOTRICE_EPIC, Rarity.UNCOMMON);
		helper.succeed();
	}

	private static void assertSack(GameTestHelper helper, Identifier id, Rarity rarity) {
		ItemStack stack = new ItemStack(item(id));
		helper.assertValueEqual(stack.get(DataComponents.RARITY), rarity, "rarity of " + id);
		helper.assertValueEqual(stack.getMaxStackSize(), 1, "stack size of " + id);
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

	private static Item item(Identifier id) {
		return BuiltInRegistries.ITEM.getValue(id);
	}
}
