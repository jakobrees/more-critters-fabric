package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.CritterlingSystemIds;
import com.morecritters.fabric.ids.FossilsIds;
import com.morecritters.fabric.ids.GravediggerIds;
import com.morecritters.fabric.module.fossils.AncientSkeletonExhibitEntity;
import com.morecritters.fabric.module.fossils.FossilDisplayBlock;
import com.morecritters.fabric.module.fossils.FossilsModule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;

/** Fossil ore, the fossil display stand, the grave brush and the ancient skeleton exhibit. */
public class FossilsTests {
	private static final BlockPos STAND = new BlockPos(3, 1, 3);

	/** The seventeen fossils in the original's order: fossil n is shown by fossil_display_n. */
	private static final List<Identifier> FOSSILS = List.of(
		FossilsIds.Items.CRITTER_FOSSIL_1, FossilsIds.Items.CRITTER_FOSSIL_2, FossilsIds.Items.CRITTER_FOSSIL_3,
		FossilsIds.Items.CRITTER_FOSSIL_4, FossilsIds.Items.CRITTER_FOSSIL_5, FossilsIds.Items.CRITTER_FOSSIL_6,
		FossilsIds.Items.CRITTER_FOSSIL_7, FossilsIds.Items.CRITTER_FOSSIL_8, FossilsIds.Items.CRITTER_FOSSIL_9,
		FossilsIds.Items.CRITTER_FOSSIL_10,
		CritterlingSystemIds.Items.CRITTERLING_FOSSIL_1, CritterlingSystemIds.Items.CRITTERLING_FOSSIL_2,
		CritterlingSystemIds.Items.CRITTERLING_FOSSIL_3,
		FossilsIds.Items.PLANT_FOSSIL_1, FossilsIds.Items.PLANT_FOSSIL_2, FossilsIds.Items.PLANT_FOSSIL_3,
		FossilsIds.Items.PLANT_FOSSIL_4);

	// --- display stand ----------------------------------------------------------------

	@GameTest
	public void everyFossilGoesOnTheStandAndOneIsUsedUp(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		for (int n = 1; n <= FOSSILS.size(); n++) {
			helper.setBlock(STAND, FossilsModule.EMPTY_DISPLAY.defaultBlockState().setValue(FossilDisplayBlock.FACING, Direction.EAST));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(FOSSILS.get(n - 1)), 3));
			helper.useBlock(STAND, player);
			BlockState shown = helper.getBlockState(STAND);
			helper.assertValueEqual(BuiltInRegistries.BLOCK.getKey(shown.getBlock()).getPath(), "fossil_display_" + n, "stand after using " + FOSSILS.get(n - 1));
			helper.assertValueEqual(shown.getValue(FossilDisplayBlock.FACING), Direction.EAST, "facing of the full stand");
			helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "fossils left in a survival hand");
		}
		helper.succeed();
	}

	@GameTest
	public void inCreativeTheFossilIsNotUsedUp(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(STAND, FossilsModule.EMPTY_DISPLAY);
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		GameType.CREATIVE.updatePlayerAbilities(player.getAbilities());
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(FossilsIds.Items.PLANT_FOSSIL_2)));
		helper.useBlock(STAND, player);
		helper.assertBlockPresent(display(15), STAND);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "fossils left in a creative hand");
		helper.succeed();
	}

	@GameTest
	public void anEmptyHandTakesTheFossilBack(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(STAND, display(12).defaultBlockState().setValue(FossilDisplayBlock.FACING, Direction.WEST));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.useBlock(STAND, player);
		helper.assertBlockPresent(FossilsModule.EMPTY_DISPLAY, STAND);
		helper.assertValueEqual(helper.getBlockState(STAND).getValue(FossilDisplayBlock.FACING), Direction.WEST, "facing of the emptied stand");
		helper.assertValueEqual(player.getMainHandItem().getItem(), item(CritterlingSystemIds.Items.CRITTERLING_FOSSIL_2), "item handed back");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "fossils handed back");
		helper.succeed();
	}

	@GameTest
	public void otherItemsDoNothingToAStand(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.setBlock(STAND, FossilsModule.EMPTY_DISPLAY);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE, 4));
		helper.useBlock(STAND, player);
		helper.assertBlockPresent(FossilsModule.EMPTY_DISPLAY, STAND);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 4, "bones after using them on an empty stand");

		helper.setBlock(STAND, display(3));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(FossilsIds.Items.CRITTER_FOSSIL_5)));
		helper.useBlock(STAND, player);
		helper.assertBlockPresent(display(3), STAND);
		helper.assertValueEqual(player.getMainHandItem().getItem(), item(FossilsIds.Items.CRITTER_FOSSIL_5), "held item after using it on a full stand");
		helper.succeed();
	}

	@GameTest
	public void breakingAFullStandDropsTheFossilAndTheStand(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(STAND, display(7));
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.gameMode.destroyBlock(helper.absolutePos(STAND));
		helper.assertBlockPresent(Blocks.AIR, STAND);
		Map<Item, Integer> drops = drops(helper);
		helper.assertValueEqual(drops.get(item(FossilsIds.Items.CRITTER_FOSSIL_7)), 1, "fossils dropped");
		helper.assertValueEqual(drops.get(FossilsModule.EMPTY_DISPLAY.asItem()), 1, "stands dropped");
		helper.assertValueEqual(drops.size(), 2, "kinds of item dropped " + drops);
		helper.succeed();
	}

	@GameTest
	public void pickBlockOnAFullStandGivesTheEmptyStand(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(STAND, display(16));
		BlockState state = helper.getBlockState(STAND);
		ItemStack picked = state.getCloneItemStack(helper.getLevel(), helper.absolutePos(STAND), false);
		helper.assertValueEqual(picked.getItem(), FossilsModule.EMPTY_DISPLAY.asItem(), "picked item");
		helper.succeed();
	}

	// --- fossil ore ---------------------------------------------------------------------

	@GameTest
	public void fossilOreDropsAFossilAboutOneTimeInThree(GameTestHelper helper) {
		TestScenes.floor(helper);
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
		int broken = breakAll(helper, player, ore(false), ore(true));
		Map<Item, Integer> drops = drops(helper);
		int fossils = 0;
		for (var drop : drops.entrySet()) {
			helper.assertTrue(FOSSILS.contains(BuiltInRegistries.ITEM.getKey(drop.getKey())), "fossil ore dropped " + drop.getKey());
			fossils += drop.getValue();
		}
		helper.assertTrue(fossils >= broken / 10 && fossils <= broken * 2 / 3, fossils + " fossils from " + broken + " ores");
		helper.succeed();
	}

	@GameTest
	public void silkTouchGivesTheOreBack(GameTestHelper helper) {
		TestScenes.floor(helper);
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
		pickaxe.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
		player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
		helper.setBlock(new BlockPos(2, 1, 3), ore(false));
		helper.setBlock(new BlockPos(5, 1, 3), ore(true));
		player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(2, 1, 3)));
		player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(5, 1, 3)));
		Map<Item, Integer> drops = drops(helper);
		helper.assertValueEqual(drops.get(ore(false).asItem()), 1, "fossil ore dropped");
		helper.assertValueEqual(drops.get(ore(true).asItem()), 1, "deepslate fossil ore dropped");
		helper.assertValueEqual(drops.size(), 2, "kinds of item dropped " + drops);
		helper.succeed();
	}

	// --- grave brush --------------------------------------------------------------------

	@GameTest
	public void brushingTheTopOfDirtDigsUpOneOverworldMob(GameTestHelper helper) {
		assertBrushDigsUp(helper, List.of(EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.SPIDER, EntityTypes.CAVE_SPIDER,
			EntityTypes.ZOMBIE_VILLAGER, EntityTypes.ZOMBIE_HORSE, EntityTypes.ENDERMAN, EntityTypes.SKELETON_HORSE,
			BuiltInRegistries.ENTITY_TYPE.getValue(GravediggerIds.Entities.AMALGAM)));
	}

	@GameTest(dimension = "minecraft:the_nether")
	public void inTheNetherTheBrushDigsUpNetherMobs(GameTestHelper helper) {
		assertBrushDigsUp(helper, List.of(EntityTypes.ZOMBIFIED_PIGLIN, EntityTypes.ZOGLIN, EntityTypes.WITHER_SKELETON));
	}

	/**
	 * Twenty strokes with fresh players (no cooldown): each digs up exactly one of {@code risers}, pushed
	 * upward, and wears the brush by one. The test server is peaceful, where monsters cannot spawn at
	 * all, so the strokes run on Normal and the difficulty is put back in the same tick.
	 */
	private static void assertBrushDigsUp(GameTestHelper helper, List<EntityType<?>> risers) {
		TestScenes.floor(helper);
		BlockPos dirt = new BlockPos(3, 1, 3);
		helper.setBlock(dirt, Blocks.GRASS_BLOCK);
		var server = helper.getLevel().getServer();
		Difficulty before = helper.getLevel().getDifficulty();
		server.setDifficulty(Difficulty.NORMAL, true);
		try {
			for (int use = 0; use < 20; use++) {
				Player player = helper.makeMockPlayer(GameType.SURVIVAL);
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(FossilsIds.Items.GRAVE_BRUSH)));
				TestScenes.useItemOn(helper, player, dirt, Direction.UP);
				// A vanilla jockey (a skeleton on a spider, a baby zombie on a chicken) counts once, by its mount.
				List<Entity> mobs = helper.getLevel().getEntities((Entity) null, helper.getBounds(),
					entity -> entity instanceof Mob && entity.getVehicle() == null);
				helper.assertValueEqual(mobs.size(), 1, "mobs dug up by one brush stroke");
				Entity mob = mobs.get(0);
				helper.assertTrue(risers.contains(mob.getType()), "dug up a " + mob.getType().getDescriptionId());
				helper.assertTrue(mob.getDeltaMovement().y >= 0.5, "the mob is pushed up out of the ground");
				helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 1, "brush wear after one use");
				helper.assertTrue(player.getCooldowns().isOnCooldown(player.getMainHandItem()), "the brush is on cooldown");
				mob.getPassengers().forEach(Entity::discard);
				mob.discard();
			}
		} finally {
			server.setDifficulty(before, true);
		}
		helper.succeed();
	}

	@GameTest
	public void theBrushDoesNothingOnStoneOnTheSideOfDirtOrWhileCoolingDown(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos dirt = new BlockPos(3, 1, 3);
		helper.setBlock(dirt, Blocks.DIRT);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(FossilsIds.Items.GRAVE_BRUSH)));
		TestScenes.useItemOn(helper, player, new BlockPos(5, 0, 5), Direction.UP);
		TestScenes.useItemOn(helper, player, dirt, Direction.NORTH);
		helper.assertValueEqual(mobCount(helper), 0, "mobs after brushing stone and the side of dirt");
		helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 0, "brush wear");

		var server = helper.getLevel().getServer();
		Difficulty before = helper.getLevel().getDifficulty();
		server.setDifficulty(Difficulty.NORMAL, true);
		try {
			TestScenes.useItemOn(helper, player, dirt, Direction.UP);
			helper.assertValueEqual(mobCount(helper), 1, "mobs after the first stroke on top of dirt");
			TestScenes.useItemOn(helper, player, dirt, Direction.UP);
			helper.assertValueEqual(mobCount(helper), 1, "mobs after a second stroke during the cooldown");
			helper.getLevel().getEntities((Entity) null, helper.getBounds(), entity -> entity instanceof Mob).forEach(Entity::discard);
		} finally {
			server.setDifficulty(before, true);
		}
		helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 1, "brush wear after the second stroke");
		helper.succeed();
	}

	@GameTest
	public void theBrushWearsOutAfterTenUses(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos dirt = new BlockPos(3, 1, 3);
		helper.setBlock(dirt, Blocks.DIRT);
		ItemStack brush = new ItemStack(item(FossilsIds.Items.GRAVE_BRUSH));
		helper.assertValueEqual(brush.getMaxDamage(), 10, "brush durability");
		for (int use = 0; use < 10; use++) {
			Player player = helper.makeMockPlayer(GameType.SURVIVAL);
			player.setItemInHand(InteractionHand.MAIN_HAND, brush);
			TestScenes.useItemOn(helper, player, dirt, Direction.UP);
		}
		helper.assertTrue(brush.isEmpty(), "the brush breaks on its tenth use");
		helper.succeed();
	}

	// --- ancient skeleton exhibit ----------------------------------------------------------

	@GameTest
	public void theExhibitItemSetsUpTheExhibitOnTheClickedFace(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(FossilsIds.Items.ANCIENT_SKELETON_EXHIBIT_ITEM)));
		TestScenes.useItemOn(helper, player, new BlockPos(3, 0, 3), Direction.UP);
		helper.assertTrue(player.getMainHandItem().isEmpty(), "the item is used up");
		AncientSkeletonExhibitEntity exhibit = helper.findOneEntity(FossilsModule.ANCIENT_SKELETON_EXHIBIT);
		helper.assertValueEqual(exhibit.blockPosition(), helper.absolutePos(new BlockPos(3, 1, 3)), "where the exhibit stands");
		helper.succeed();
	}

	@GameTest
	public void rightClickingCyclesTheTenPosesAndThePoseIsSaved(GameTestHelper helper) {
		TestScenes.floor(helper);
		AncientSkeletonExhibitEntity exhibit = helper.spawn(FossilsModule.ANCIENT_SKELETON_EXHIBIT, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE));
		helper.assertValueEqual(exhibit.pose(), 0, "pose at first");
		for (int click = 1; click <= 10; click++) {
			exhibit.mobInteract(player, InteractionHand.MAIN_HAND);
			helper.assertValueEqual(exhibit.pose(), click % 10, "pose after " + click + " clicks");
		}
		exhibit.mobInteract(player, InteractionHand.MAIN_HAND);
		exhibit.mobInteract(player, InteractionHand.MAIN_HAND);
		exhibit.mobInteract(player, InteractionHand.MAIN_HAND);
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
		exhibit.saveWithoutId(output);
		CompoundTag saved = output.buildResult();
		helper.assertValueEqual(saved.getIntOr("Datapose", -1), 3, "saved pose");
		helper.succeed();
	}

	@GameTest
	public void sneakingWithAnEmptyHandPacksTheExhibitUp(GameTestHelper helper) {
		TestScenes.floor(helper);
		AncientSkeletonExhibitEntity exhibit = helper.spawn(FossilsModule.ANCIENT_SKELETON_EXHIBIT, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setShiftKeyDown(true);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE));
		exhibit.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertFalse(exhibit.isRemoved(), "sneaking with an item does not pack it up");
		helper.assertValueEqual(exhibit.pose(), 0, "sneaking with an item does not change the pose");

		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		exhibit.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertTrue(exhibit.isRemoved(), "the exhibit is packed up");
		helper.assertValueEqual(player.getMainHandItem().getItem(), item(FossilsIds.Items.ANCIENT_SKELETON_EXHIBIT_ITEM), "item in hand");
		helper.succeed();
	}

	@GameTest
	public void playersAndHazardsCannotHurtTheExhibitButOtherBlowsBreakIt(GameTestHelper helper) {
		TestScenes.floor(helper);
		AncientSkeletonExhibitEntity exhibit = helper.spawnWithNoFreeWill(FossilsModule.ANCIENT_SKELETON_EXHIBIT, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		DamageSources sources = helper.getLevel().damageSources();
		for (DamageSource source : List.of(sources.playerAttack(player), sources.inFire(), sources.fall(), sources.drown(), sources.cactus(),
				sources.lightningBolt(), sources.explosion(null, null), sources.wither(), sources.dragonBreath(), sources.inWall())) {
			helper.hurt(exhibit, source, 10.0F);
			helper.assertValueEqual(exhibit.getHealth(), 50.0F, "health after " + source.getMsgId());
		}
		helper.assertTrue(exhibit.fireImmune(), "the exhibit does not burn");
		helper.assertFalse(exhibit.removeWhenFarAway(1000.0), "the exhibit despawns");
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		helper.hurt(exhibit, sources.mobAttack(zombie), 10.0F);
		helper.assertValueEqual(exhibit.getHealth(), 40.0F, "health after a zombie's hit");
		helper.hurt(exhibit, sources.mobAttack(zombie), 100.0F);
		helper.assertTrue(exhibit.isRemoved(), "a destroyed exhibit vanishes at once");
		helper.succeed();
	}

	// --- helpers ------------------------------------------------------------------------

	private static Block ore(boolean deepslate) {
		return BuiltInRegistries.BLOCK.getValue(deepslate ? FossilsIds.Blocks.DEEPSLATE_FOSSIL_BLOCK : FossilsIds.Blocks.FOSSIL_BLOCK);
	}

	private static Block display(int n) {
		return BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("more_critters", "fossil_display_" + n));
	}

	private static Item item(Identifier id) {
		return BuiltInRegistries.ITEM.getValue(id);
	}

	/** Fills the layer above the floor with alternating ores and breaks them all; returns how many. */
	private static int breakAll(GameTestHelper helper, ServerPlayer player, Block first, Block second) {
		List<BlockPos> placed = new ArrayList<>();
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				BlockPos pos = new BlockPos(x, 1, z);
				helper.setBlock(pos, (x + z) % 2 == 0 ? first : second);
				placed.add(pos);
			}
		}
		for (BlockPos pos : placed) {
			player.gameMode.destroyBlock(helper.absolutePos(pos));
		}
		return placed.size();
	}

	private static Map<Item, Integer> drops(GameTestHelper helper) {
		Map<Item, Integer> counts = new HashMap<>();
		AABB area = helper.getBounds().inflate(1.0);
		for (ItemEntity drop : helper.getLevel().getEntities(EntityTypes.ITEM, area, Entity::isAlive)) {
			counts.merge(drop.getItem().getItem(), drop.getItem().getCount(), Integer::sum);
		}
		return counts;
	}

	private static int mobCount(GameTestHelper helper) {
		return helper.getLevel().getEntities((Entity) null, helper.getBounds(), entity -> entity instanceof Mob).size();
	}
}
