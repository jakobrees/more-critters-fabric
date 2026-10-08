package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.BlubberfishIds;
import com.morecritters.fabric.ids.BombJellyIds;
import com.morecritters.fabric.ids.CritterlingsAIds;
import com.morecritters.fabric.ids.ShadeletIds;
import com.morecritters.fabric.ids.ShimmerwingIds;
import com.morecritters.fabric.module.critterling_system.Critterling;
import com.morecritters.fabric.module.critterling_system.CritterlingRarity;
import com.morecritters.fabric.module.critterling_system.CritterlingSystemModule;
import com.morecritters.fabric.module.critterling_system.EvolutionTableMenu;
import com.morecritters.fabric.module.critterlings_a.CritterlingsAModule;
import com.morecritters.fabric.module.critterlings_a.CubefrogEntity;
import com.morecritters.fabric.module.critterlings_a.DungerEntity;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;

/** Cubefrog, plainswyrm, dunger and snek: caught and let out in all three forms, evolved, and the cubefrog's hop. */
public class CritterlingsATests {
	@GameTest
	public void eachCritterlingGoesIntoAndComesOutOfTheSackForItsForm(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		int x = 0;
		for (Kind kind : kinds()) {
			for (CritterlingRarity rarity : CritterlingRarity.values()) {
				BlockPos at = new BlockPos(x % 8, 1, 1 + 2 * (x / 8));
				x += 2;
				Critterling critterling = helper.spawnWithNoFreeWill(kind.type(), at);
				critterling.setRarity(rarity);
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CritterlingSystemModule.CRITTERLING_SACK));
				UseEntityCallback.EVENT.invoker().interact(player, helper.getLevel(), InteractionHand.MAIN_HAND, critterling, null);

				String what = rarity + " " + kind.type().toShortString();
				ItemStack sack = player.getMainHandItem();
				helper.assertTrue(critterling.isRemoved(), what + " still out after catching");
				helper.assertValueEqual(itemId(sack), kind.sacks().get(rarity.ordinal()), "sack for a " + what);
				helper.assertValueEqual(sack.get(DataComponents.RARITY), ITEM_RARITIES.get(rarity.ordinal()), "item rarity of the sack for a " + what);
				helper.assertValueEqual(sack.getMaxStackSize(), 1, "stack size of the sack for a " + what);

				TestScenes.useItemOn(helper, player, at.below(), Direction.UP);
				List<? extends Critterling> released = helper.getEntities(kind.type(), at, 0.5);
				helper.assertValueEqual(released.size(), 1, what + "s released");
				helper.assertValueEqual(released.getFirst().rarity(), rarity, "rarity of the released " + what);
				helper.assertTrue(player.getMainHandItem().is(CritterlingSystemModule.CRITTERLING_SACK), "empty sack back after releasing a " + what);
			}
		}
		helper.succeed();
	}

	@GameTest
	public void theEvolutionTableEvolvesEachCritterlingWithTheOriginalsIngredients(GameTestHelper helper) {
		// From the original's EvoTableProcedure1Procedure.
		evolves(helper, CritterlingsAIds.Items.CRITTERLING_SACK_CUBEFROG, CritterlingsAIds.Items.CRITTERLING_SACK_CUBEFROG_RARE, Items.INK_SAC, Items.HONEYCOMB);
		evolves(helper, CritterlingsAIds.Items.CRITTERLING_SACK_CUBEFROG_RARE, CritterlingsAIds.Items.CRITTERLING_SACK_CUBEFROG_EPIC, Items.SLIME_BALL, Items.QUARTZ, Items.FERMENTED_SPIDER_EYE);
		evolves(helper, CritterlingsAIds.Items.CRITTERLING_SACK_PLAINSWYRM, CritterlingsAIds.Items.CRITTERLING_SACK_PLAINSWYRM_RARE,
			item(BombJellyIds.Items.EXPLOSIVE_JELLY), item(BlubberfishIds.Items.SPRINKLES));
		evolves(helper, CritterlingsAIds.Items.CRITTERLING_SACK_PLAINSWYRM_RARE, CritterlingsAIds.Items.CRITTERLING_SACK_PLAINSWYRM_EPIC,
			item(ShadeletIds.Items.SHARK_TOOTH), Items.CHORUS_FRUIT, Items.BONE);
		evolves(helper, CritterlingsAIds.Items.CRITTERLING_SACK_DUNGER, CritterlingsAIds.Items.CRITTERLING_SACK_DUNGER_RARE, Items.COPPER_INGOT, Items.BEETROOT);
		evolves(helper, CritterlingsAIds.Items.CRITTERLING_SACK_DUNGER_RARE, CritterlingsAIds.Items.CRITTERLING_SACK_DUNGER_EPIC, Items.GLOW_INK_SAC, Items.PRISMARINE_CRYSTALS, Items.AMETHYST_SHARD);
		evolves(helper, CritterlingsAIds.Items.CRITTERLING_SACK_SNEK, CritterlingsAIds.Items.CRITTERLING_SACK_SNEK_RARE, Items.GOLD_INGOT, Items.REDSTONE);
		evolves(helper, CritterlingsAIds.Items.CRITTERLING_SACK_SNEK_RARE, CritterlingsAIds.Items.CRITTERLING_SACK_SNEK_EPIC,
			item(ShimmerwingIds.Items.END_DUST), Items.CHERRY_LEAVES, Items.POPPY);
		helper.succeed();
	}

	@GameTest(maxTicks = 240)
	public void aCubefrogHopsEveryFewSeconds(GameTestHelper helper) {
		TestScenes.floor(helper);
		CubefrogEntity cubefrog = helper.spawn(CritterlingsAModule.CUBEFROG, new BlockPos(3, 1, 3), EntitySpawnReason.MOB_SUMMONED);
		double ground = cubefrog.getY();
		// The first hop timer is rolled 60-200 on spawn, and the leap comes 5 ticks after the crouch.
		helper.startSequence()
			.thenExecuteFor(60, () -> helper.assertTrue(cubefrog.getY() < ground + 0.05, "the cubefrog hopped within 3 seconds of spawning"))
			.thenWaitUntil(() -> helper.assertTrue(cubefrog.getY() > ground + 0.3, "the cubefrog has not hopped"))
			.thenSucceed();
	}

	@GameTest(maxTicks = 120)
	public void aCubefrogDoesNotHopWhileDancing(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(new BlockPos(1, 1, 3), Blocks.JUKEBOX.defaultBlockState().setValue(JukeboxBlock.HAS_RECORD, true));
		// Spawned without the spawn roll: its first hop would come at tick 40.
		CubefrogEntity cubefrog = helper.spawn(CritterlingsAModule.CUBEFROG, new BlockPos(3, 1, 3));
		double ground = cubefrog.getY();
		helper.startSequence()
			.thenExecuteFor(100, () -> helper.assertTrue(cubefrog.getY() < ground + 0.05, "the dancing cubefrog hopped"))
			.thenExecute(() -> helper.assertTrue(cubefrog.isDancing(), "the cubefrog dances"))
			.thenSucceed();
	}

	@GameTest
	public void critterlingsDropNoExperience(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		DungerEntity dunger = helper.spawnWithNoFreeWill(CritterlingsAModule.DUNGER, new BlockPos(3, 1, 3));
		helper.hurt(dunger, helper.getLevel().damageSources().playerAttack(player), 20.0F);
		helper.assertTrue(dunger.isDeadOrDying(), "the dunger died");
		helper.assertEntityNotPresent(EntityTypes.EXPERIENCE_ORB);
		helper.succeed();
	}

	// --- helpers ----------------------------------------------------------------------

	private static final List<Rarity> ITEM_RARITIES = List.of(Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC);

	private record Kind(EntityType<? extends Critterling> type, List<Identifier> sacks) {}

	private static List<Kind> kinds() {
		return List.of(
			new Kind(CritterlingsAModule.CUBEFROG, List.of(CritterlingsAIds.Items.CRITTERLING_SACK_CUBEFROG,
				CritterlingsAIds.Items.CRITTERLING_SACK_CUBEFROG_RARE, CritterlingsAIds.Items.CRITTERLING_SACK_CUBEFROG_EPIC)),
			new Kind(CritterlingsAModule.PLAINSWYRM, List.of(CritterlingsAIds.Items.CRITTERLING_SACK_PLAINSWYRM,
				CritterlingsAIds.Items.CRITTERLING_SACK_PLAINSWYRM_RARE, CritterlingsAIds.Items.CRITTERLING_SACK_PLAINSWYRM_EPIC)),
			new Kind(CritterlingsAModule.DUNGER, List.of(CritterlingsAIds.Items.CRITTERLING_SACK_DUNGER,
				CritterlingsAIds.Items.CRITTERLING_SACK_DUNGER_RARE, CritterlingsAIds.Items.CRITTERLING_SACK_DUNGER_EPIC)),
			new Kind(CritterlingsAModule.SNEK, List.of(CritterlingsAIds.Items.CRITTERLING_SACK_SNEK,
				CritterlingsAIds.Items.CRITTERLING_SACK_SNEK_RARE, CritterlingsAIds.Items.CRITTERLING_SACK_SNEK_EPIC)));
	}

	/** Puts the sack, an evolite and the ingredients in an evolution table and takes the result. */
	private static void evolves(GameTestHelper helper, Identifier sack, Identifier result, Item... ingredients) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		EvolutionTableMenu menu = new EvolutionTableMenu(1, player.getInventory(),
			ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1))));
		menu.getSlot(EvolutionTableMenu.SACK_SLOT).set(new ItemStack(item(sack)));
		menu.getSlot(EvolutionTableMenu.EVOLITE_SLOT).set(new ItemStack(CritterlingSystemModule.EVOLITE));
		for (int i = 0; i < ingredients.length; i++) {
			helper.assertValueEqual(menu.getSlot(EvolutionTableMenu.PREVIEW_SLOTS[i]).getItem().getItem(), ingredients[i], "preview " + i + " for " + sack);
			menu.getSlot(EvolutionTableMenu.INGREDIENT_SLOTS[i]).set(new ItemStack(ingredients[i]));
		}
		helper.assertValueEqual(itemId(menu.getSlot(EvolutionTableMenu.RESULT_SLOT).getItem()), result, "evolution of " + sack);
		menu.clicked(EvolutionTableMenu.RESULT_SLOT, 0, ContainerInput.PICKUP, player);
		helper.assertValueEqual(itemId(menu.getCarried()), result, "taken evolution of " + sack);
		helper.assertTrue(menu.getSlot(EvolutionTableMenu.SACK_SLOT).getItem().isEmpty(), "sack used up for " + sack);
	}

	private static Item item(Identifier id) {
		return BuiltInRegistries.ITEM.getValue(id);
	}

	private static Identifier itemId(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem());
	}
}
