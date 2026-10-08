package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.CritterlingSystemIds;
import com.morecritters.fabric.ids.IropodIds;
import com.morecritters.fabric.ids.MiscIds;
import com.morecritters.fabric.module.misc.AnniveteranEntity;
import com.morecritters.fabric.module.misc.MiscModule;
import java.time.LocalDate;
import java.time.Month;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

/** Odds and ends: the anniveteran, the End dust bunny, the haste potion, music discs, the party hat and outpost loot. */
public class MiscTests {
	@GameTest
	public void anAnniveteranWearsOneOfFourLooksPickedAtSpawn(GameTestHelper helper) {
		TestScenes.floor(helper);
		Set<String> looks = new HashSet<>();
		for (int i = 0; i < 60; i++) {
			AnniveteranEntity anniveteran = helper.spawn(MiscModule.ANNIVETERAN, new BlockPos(3, 1, 3), EntitySpawnReason.SPAWN_ITEM_USE);
			looks.add(anniveteran.textureName());
			anniveteran.discard();
		}
		helper.assertValueEqual(looks, Set.of("anniveteran", "anniveteran1", "anniveteran2", "anniveteran3"), "looks over 60 spawns");
		helper.succeed();
	}

	@GameTest
	public void anniveteransSpawnNaturallyOnlyFromTheSeventhToTheNinthOfAugust(GameTestHelper helper) {
		LocalDate today = LocalDate.now();
		boolean anniversary = today.getMonth() == Month.AUGUST && today.getDayOfMonth() >= 7 && today.getDayOfMonth() <= 9;
		boolean allowed = AnniveteranEntity.canSpawnAt(MiscModule.ANNIVETERAN, helper.getLevel(), EntitySpawnReason.NATURAL,
			helper.absolutePos(new BlockPos(3, 1, 3)), helper.getLevel().getRandom());
		helper.assertValueEqual(helper.getLevel().dimension(), Level.OVERWORLD, "the test level");
		helper.assertValueEqual(allowed, anniversary, "natural spawning allowed on " + today);
		helper.succeed();
	}

	@GameTest
	public void theEndDustBunnyIsEdibleWhenFullAndKnocksTheEaterBackwards(GameTestHelper helper) {
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setYRot(180.0F); // looking north
		player.setXRot(0.0F);
		ItemStack bunny = new ItemStack(item(MiscIds.Items.END_DUST_BUNNY), 2);
		player.setItemInHand(InteractionHand.MAIN_HAND, bunny);

		helper.assertTrue(bunny.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "edible on a full stomach");

		player.getFoodData().setFoodLevel(10);
		ItemStack left = bunny.finishUsingItem(helper.getLevel(), player);
		helper.assertValueEqual(left.getCount(), 1, "bunnies left");
		helper.assertValueEqual(player.getFoodData().getFoodLevel(), 12, "food after eating");
		Vec3 motion = player.getDeltaMovement();
		helper.assertTrue(Math.abs(motion.z - 0.3) < 1.0E-3 && Math.abs(motion.x) < 1.0E-3,
			"pushed south (backwards) by 0.3; motion " + motion);
		helper.succeed();
	}

	@GameTest
	public void theHastePotionGivesThreeMinutesOfHasteOne(GameTestHelper helper) {
		Holder<net.minecraft.world.item.alchemy.Potion> haste = BuiltInRegistries.POTION.get(MiscIds.Potions.HASTE_POTION).orElseThrow();
		List<MobEffectInstance> effects = new java.util.ArrayList<>();
		new PotionContents(haste).getAllEffects().forEach(effects::add);
		helper.assertValueEqual(effects.size(), 1, "effects");
		helper.assertTrue(effects.get(0).is(MobEffects.HASTE), "the effect is Haste");
		helper.assertValueEqual(effects.get(0).getAmplifier(), 0, "amplifier");
		helper.assertValueEqual(effects.get(0).getDuration(), 3600, "duration");
		helper.succeed();
	}

	@GameTest
	public void everyMusicDiscPlaysInAJukebox(GameTestHelper helper) {
		List<Identifier> discs = List.of(MiscIds.Items.MUSIC_DISC_SAILS, MiscIds.Items.MUSIC_DISC_GROOVEYARD,
			MiscIds.Items.MUSIC_DISC_WADDLE, MiscIds.Items.MUSIC_DISC_PARTY);
		for (int i = 0; i < discs.size(); i++) {
			BlockPos pos = new BlockPos(1 + i, 1, 1);
			helper.setBlock(pos, Blocks.JUKEBOX);
			Player player = helper.makeMockPlayer(GameType.SURVIVAL);
			ItemStack disc = new ItemStack(item(discs.get(i)));
			helper.assertValueEqual(disc.getMaxStackSize(), 1, discs.get(i) + " stack size");
			JukeboxPlayable.tryInsertIntoJukebox(helper.getLevel(), helper.absolutePos(pos), disc, player);
			JukeboxBlockEntity jukebox = helper.getBlockEntity(pos, JukeboxBlockEntity.class);
			helper.assertTrue(jukebox.getSongPlayer().isPlaying(), discs.get(i) + " plays");
		}
		helper.succeed();
	}

	@GameTest
	public void thePartyHatIsAHelmetWithTwoArmor(GameTestHelper helper) {
		ItemStack hat = new ItemStack(item(MiscIds.Items.PARTY_HAT_HELMET));
		helper.assertValueEqual(hat.get(DataComponents.EQUIPPABLE).slot(), EquipmentSlot.HEAD, "party hat slot");
		helper.assertValueEqual(armor(hat), 2.0, "party hat armor");
		helper.assertValueEqual(hat.getMaxDamage(), 15 * 11, "party hat durability");

		ItemStack helmet = new ItemStack(item(IropodIds.Items.IROPOD_HELMET_HELMET));
		helper.assertValueEqual(armor(helmet), 3.0, "iropod helmet armor");
		helper.succeed();
	}

	@GameTest
	public void pillagerOutpostChestsSometimesHoldOneToThreeEvolite(GameTestHelper helper) {
		chestsSometimesHoldEvolite(helper, "chests/pillager_outpost");
	}

	@GameTest
	public void woodlandMansionChestsSometimesHoldOneToThreeEvolite(GameTestHelper helper) {
		chestsSometimesHoldEvolite(helper, "chests/woodland_mansion");
	}

	// --- helpers --------------------------------------------------------------------------

	/** Fills a chest from the table 40 times; the addition rolls 0 or 1 stacks of 1 to 3 evolite each time. */
	private static void chestsSometimesHoldEvolite(GameTestHelper helper, String table) {
		BlockPos pos = new BlockPos(3, 1, 3);
		helper.setBlock(pos, Blocks.CHEST);
		ChestBlockEntity chest = helper.getBlockEntity(pos, ChestBlockEntity.class);
		Item evolite = item(CritterlingSystemIds.Items.EVOLITE);
		ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace(table));
		int chestsWithEvolite = 0;
		for (int i = 0; i < 40; i++) {
			chest.clearContent();
			chest.setLootTable(key, i);
			chest.unpackLootTable(null);
			int count = 0;
			for (int slot = 0; slot < chest.getContainerSize(); slot++) {
				if (chest.getItem(slot).is(evolite)) count += chest.getItem(slot).getCount();
			}
			helper.assertTrue(count <= 3, "at most 3 evolite in one chest, found " + count);
			if (count > 0) chestsWithEvolite++;
		}
		helper.assertTrue(chestsWithEvolite > 0, "some of 40 " + table + " chests hold evolite");
		helper.assertTrue(chestsWithEvolite < 40, "not every " + table + " chest holds evolite");
		helper.succeed();
	}

	private static double armor(ItemStack stack) {
		double[] armor = {0.0};
		stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).forEach(EquipmentSlot.HEAD, (attribute, modifier) -> {
			if (attribute.is(Attributes.ARMOR)) armor[0] += modifier.amount();
		});
		return armor[0];
	}

	private static Item item(Identifier id) {
		return BuiltInRegistries.ITEM.getValue(id);
	}
}
