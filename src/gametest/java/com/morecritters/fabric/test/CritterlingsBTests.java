package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.BunbugIds;
import com.morecritters.fabric.ids.CritterlingsBIds;
import com.morecritters.fabric.module.critterling_system.Critterling;
import com.morecritters.fabric.module.critterling_system.CritterlingRarity;
import com.morecritters.fabric.module.critterling_system.CritterlingSystemModule;
import com.morecritters.fabric.module.critterling_system.EvolutionTableMenu;
import com.morecritters.fabric.module.critterlings_b.CritterlingsBModule;
import com.morecritters.fabric.module.critterlings_b.ExpyEntity;
import com.morecritters.fabric.module.critterlings_b.OpalcrabEntity;
import com.morecritters.fabric.module.critterlings_b.RollballEntity;
import com.morecritters.fabric.module.critterlings_b.ScowlEntity;
import java.util.List;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Expy, scowl, rollball and opalcrab: sacks and evolution, and what each one does on its own. */
public class CritterlingsBTests {
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
				List<? extends Critterling> released = helper.getEntities(kind.type(), at, 0.6);
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
		evolves(helper, CritterlingsBIds.Items.CRITTERLING_SACK_EXPY, CritterlingsBIds.Items.CRITTERLING_SACK_EXPY_RARE, Items.GLOWSTONE_DUST, Items.QUARTZ);
		evolves(helper, CritterlingsBIds.Items.CRITTERLING_SACK_EXPY_RARE, CritterlingsBIds.Items.CRITTERLING_SACK_EXPY_EPIC, Items.REDSTONE, Items.GLOWSTONE, Items.PHANTOM_MEMBRANE);
		evolves(helper, CritterlingsBIds.Items.CRITTERLING_SACK_SCOWL, CritterlingsBIds.Items.CRITTERLING_SACK_SCOWL_RARE, Items.RABBIT_HIDE, Items.GLOW_BERRIES);
		evolves(helper, CritterlingsBIds.Items.CRITTERLING_SACK_SCOWL_RARE, CritterlingsBIds.Items.CRITTERLING_SACK_SCOWL_EPIC, Items.INK_SAC, Items.PRISMARINE_CRYSTALS, Items.LAPIS_LAZULI);
		evolves(helper, CritterlingsBIds.Items.CRITTERLING_SACK_ROLLBALL, CritterlingsBIds.Items.CRITTERLING_SACK_ROLLBALL_RARE, Items.WOOL.pick(DyeColor.CYAN), Items.STRING);
		evolves(helper, CritterlingsBIds.Items.CRITTERLING_SACK_ROLLBALL_RARE, CritterlingsBIds.Items.CRITTERLING_SACK_ROLLBALL_EPIC,
			Items.BREAD, item(BunbugIds.Items.RAW_BUNBUG_MEAT), item(BunbugIds.Items.BUNBUG_CRUST));
		evolves(helper, CritterlingsBIds.Items.CRITTERLING_SACK_OPALCRAB, CritterlingsBIds.Items.CRITTERLING_SACK_OPALCRAB_RARE, Items.GUNPOWDER, Items.FEATHER);
		evolves(helper, CritterlingsBIds.Items.CRITTERLING_SACK_OPALCRAB_RARE, CritterlingsBIds.Items.CRITTERLING_SACK_OPALCRAB_EPIC, Items.CRYING_OBSIDIAN, Items.GLOW_INK_SAC, Items.AMETHYST_SHARD);
		helper.succeed();
	}

	// --- expy -------------------------------------------------------------------------

	@GameTest
	public void anExpyFloatsAndDropsOneExperienceWhenKilled(GameTestHelper helper) {
		TestScenes.floor(helper);
		ExpyEntity expy = helper.spawnWithNoFreeWill(CritterlingsBModule.EXPY, new BlockPos(3, 3, 3));
		helper.assertTrue(expy.isNoGravity(), "the expy ignores gravity");
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.hurt(expy, helper.getLevel().damageSources().playerAttack(player), 20.0F);
		helper.assertTrue(expy.isDeadOrDying(), "the expy died");
		int xp = helper.getEntities(EntityTypes.EXPERIENCE_ORB).stream().mapToInt(ExperienceOrb::getValue).sum();
		helper.assertValueEqual(xp, 1, "experience dropped");
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void anExpyOnTheGroundLiftsOff(GameTestHelper helper) {
		TestScenes.floor(helper);
		ExpyEntity expy = helper.spawn(CritterlingsBModule.EXPY, new BlockPos(3, 1, 3));
		// It has no gravity: a freshly spawned expy only counts as on the ground once it moves into it.
		expy.setDeltaMovement(0.0, -0.1, 0.0);
		double ground = expy.getY();
		helper.succeedWhen(() -> helper.assertTrue(expy.getY() > ground + 0.2, "the expy is still on the ground"));
	}

	// --- scowl ------------------------------------------------------------------------

	@GameTest(maxTicks = 240)
	public void aScowlHopsAndGlidesDownSlowly(GameTestHelper helper) {
		TestScenes.floor(helper);
		List<ScowlEntity> scowls = List.of(
			helper.spawn(CritterlingsBModule.SCOWL, new BlockPos(1, 1, 1), EntitySpawnReason.MOB_SUMMONED),
			helper.spawn(CritterlingsBModule.SCOWL, new BlockPos(6, 1, 1), EntitySpawnReason.MOB_SUMMONED),
			helper.spawn(CritterlingsBModule.SCOWL, new BlockPos(1, 1, 6), EntitySpawnReason.MOB_SUMMONED));
		double ground = scowls.getFirst().getY();
		// The original rolls the first hop 60-200 ticks after spawning, like every later one.
		helper.startSequence()
			.thenExecuteFor(55, () -> scowls.forEach(scowl ->
				helper.assertTrue(scowl.getY() < ground + 0.05, "a scowl hopped within 3 seconds of spawning")))
			.thenWaitUntil(() -> helper.assertTrue(scowls.stream().anyMatch(scowl -> scowl.getY() > ground + 0.3), "no scowl has hopped"))
			// Slow falling is given on the tick after take-off, once the scowl is off the ground.
			.thenIdle(2)
			.thenExecute(() -> {
				ScowlEntity flying = scowls.stream().filter(scowl -> scowl.getY() > ground + 0.3).findFirst().orElseThrow();
				helper.assertTrue(flying.hasEffect(MobEffects.SLOW_FALLING), "the airborne scowl has slow falling");
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 120)
	public void aScowlDoesNotHopWhileDancing(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(new BlockPos(1, 1, 3), Blocks.JUKEBOX.defaultBlockState().setValue(JukeboxBlock.HAS_RECORD, true));
		ScowlEntity scowl = helper.spawn(CritterlingsBModule.SCOWL, new BlockPos(3, 1, 3));
		setSaved(scowl, "HopTimer", 10);
		double ground = scowl.getY();
		helper.startSequence()
			.thenExecuteFor(60, () -> helper.assertTrue(scowl.getY() < ground + 0.05, "the dancing scowl hopped"))
			.thenSucceed();
	}

	// --- rollball ---------------------------------------------------------------------

	@GameTest(maxTicks = 100)
	public void aRollballCurlsUpAndUncurlsOnItsRolls(GameTestHelper helper) {
		TestScenes.floor(helper);
		RollballEntity rollball = helper.spawnWithNoFreeWill(CritterlingsBModule.ROLLBALL, new BlockPos(3, 1, 3));
		rollball.setRarity(CritterlingRarity.RARE);
		helper.assertValueEqual(rollball.textureName(), "rare_rollball", "texture before rolling");
		setSaved(rollball, "RollTimer", 2);
		// The shape changes 10 ticks after the roll starts.
		helper.startSequence()
			.thenExecuteAfter(5, () -> helper.assertFalse(rollball.isRolled(), "curled before the roll animation is half way"))
			.thenWaitUntil(() -> helper.assertTrue(rollball.isRolled(), "the rollball has not curled up"))
			.thenExecute(() -> {
				helper.assertValueEqual(rollball.textureName(), "rare_rollball_rolled", "texture when curled");
				setSaved(rollball, "RollTimer", 2);
			})
			.thenWaitUntil(() -> helper.assertFalse(rollball.isRolled(), "the rollball has not uncurled"))
			.thenExecute(() -> helper.assertValueEqual(rollball.textureName(), "rare_rollball", "texture when uncurled"))
			.thenSucceed();
	}

	@GameTest
	public void aCurledRollballStaysCurledThroughSaving(GameTestHelper helper) {
		TestScenes.floor(helper);
		RollballEntity rollball = helper.spawnWithNoFreeWill(CritterlingsBModule.ROLLBALL, new BlockPos(3, 1, 3));
		setSaved(rollball, "Rolled", 1);
		helper.assertTrue(rollball.isRolled(), "a rollball saved curled loads curled");
		RollballEntity copy = helper.spawnWithNoFreeWill(CritterlingsBModule.ROLLBALL, new BlockPos(5, 1, 5));
		copy.load(TagValueInput.create(ProblemReporter.DISCARDING, copy.registryAccess(), saved(rollball)));
		helper.assertTrue(copy.isRolled(), "the reloaded rollball is curled");
		helper.succeed();
	}

	@GameTest(maxTicks = 80)
	public void aRollballDoesNotRollWhileDancing(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(new BlockPos(1, 1, 3), Blocks.JUKEBOX.defaultBlockState().setValue(JukeboxBlock.HAS_RECORD, true));
		RollballEntity rollball = helper.spawnWithNoFreeWill(CritterlingsBModule.ROLLBALL, new BlockPos(3, 1, 3));
		setSaved(rollball, "RollTimer", 5);
		helper.startSequence()
			.thenExecuteFor(40, () -> helper.assertFalse(rollball.isRolled(), "the dancing rollball curled up"))
			.thenSucceed();
	}

	@GameTest(maxTicks = 195)
	public void aRollballFirstRollsWithinTenSecondsOfSpawning(GameTestHelper helper) {
		TestScenes.floor(helper);
		// The original rolls the first roll 60-200 ticks after spawning: of four rollballs at
		// least one curls up before tick 195 (all four rolling 185 or more: about 1 in 27 million).
		List<RollballEntity> rollballs = List.of(
			helper.spawn(CritterlingsBModule.ROLLBALL, new BlockPos(1, 1, 1), EntitySpawnReason.MOB_SUMMONED),
			helper.spawn(CritterlingsBModule.ROLLBALL, new BlockPos(6, 1, 1), EntitySpawnReason.MOB_SUMMONED),
			helper.spawn(CritterlingsBModule.ROLLBALL, new BlockPos(1, 1, 6), EntitySpawnReason.MOB_SUMMONED),
			helper.spawn(CritterlingsBModule.ROLLBALL, new BlockPos(6, 1, 6), EntitySpawnReason.MOB_SUMMONED));
		helper.succeedWhen(() -> helper.assertTrue(rollballs.stream().anyMatch(RollballEntity::isRolled), "no rollball has curled up"));
	}

	// --- opalcrab ---------------------------------------------------------------------

	@GameTest(maxTicks = 60)
	public void anOpalcrabWalksTheBottomFastUnderwater(GameTestHelper helper) {
		TestScenes.floor(helper);
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				for (int y = 1; y <= 4; y++) helper.setBlock(x, y, z, Blocks.WATER);
			}
		}
		OpalcrabEntity crab = helper.spawn(CritterlingsBModule.OPALCRAB, new BlockPos(3, 1, 3));
		helper.startSequence()
			.thenIdle(5)
			.thenExecute(() -> {
				helper.assertTrue(crab.hasEffect(MobEffects.SPEED), "the crab in water has Speed");
				helper.assertValueEqual(crab.getEffect(MobEffects.SPEED).getAmplifier(), 1, "Speed amplifier");
			})
			.thenExecuteFor(40, () -> helper.assertTrue(crab.getY() < helper.absoluteVec(new net.minecraft.world.phys.Vec3(0, 2, 0)).y,
				"the crab rose off the bottom"))
			.thenSucceed();
	}

	@GameTest(maxTicks = 30)
	public void anOpalcrabOnLandHasNoSpeed(GameTestHelper helper) {
		TestScenes.floor(helper);
		OpalcrabEntity crab = helper.spawn(CritterlingsBModule.OPALCRAB, new BlockPos(3, 1, 3));
		helper.startSequence()
			.thenExecuteFor(20, () -> helper.assertFalse(crab.hasEffect(MobEffects.SPEED), "the crab on land has Speed"))
			.thenSucceed();
	}

	// --- helpers ----------------------------------------------------------------------

	private static final List<Rarity> ITEM_RARITIES = List.of(Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC);

	private record Kind(EntityType<? extends Critterling> type, List<Identifier> sacks) {}

	private static List<Kind> kinds() {
		return List.of(
			new Kind(CritterlingsBModule.EXPY, List.of(CritterlingsBIds.Items.CRITTERLING_SACK_EXPY,
				CritterlingsBIds.Items.CRITTERLING_SACK_EXPY_RARE, CritterlingsBIds.Items.CRITTERLING_SACK_EXPY_EPIC)),
			new Kind(CritterlingsBModule.SCOWL, List.of(CritterlingsBIds.Items.CRITTERLING_SACK_SCOWL,
				CritterlingsBIds.Items.CRITTERLING_SACK_SCOWL_RARE, CritterlingsBIds.Items.CRITTERLING_SACK_SCOWL_EPIC)),
			new Kind(CritterlingsBModule.ROLLBALL, List.of(CritterlingsBIds.Items.CRITTERLING_SACK_ROLLBALL,
				CritterlingsBIds.Items.CRITTERLING_SACK_ROLLBALL_RARE, CritterlingsBIds.Items.CRITTERLING_SACK_ROLLBALL_EPIC)),
			new Kind(CritterlingsBModule.OPALCRAB, List.of(CritterlingsBIds.Items.CRITTERLING_SACK_OPALCRAB,
				CritterlingsBIds.Items.CRITTERLING_SACK_OPALCRAB_RARE, CritterlingsBIds.Items.CRITTERLING_SACK_OPALCRAB_EPIC)));
	}

	/** The entity's saved data, as a world save would write it. */
	private static CompoundTag saved(Entity entity) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
		entity.saveWithoutId(output);
		return output.buildResult();
	}

	/** Fast-forwards a saved timer (or flag) by saving the entity, changing one value and loading it back. */
	private static void setSaved(Entity entity, String key, int value) {
		CompoundTag tag = saved(entity);
		if (key.equals("Rolled")) {
			tag.putBoolean(key, value != 0);
		} else {
			tag.putInt(key, value);
		}
		entity.load(TagValueInput.create(ProblemReporter.DISCARDING, entity.registryAccess(), tag));
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
