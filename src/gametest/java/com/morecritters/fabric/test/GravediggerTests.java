package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.GravediggerIds;
import com.morecritters.fabric.module.gravedigger.AmalgamEntity;
import com.morecritters.fabric.module.gravedigger.GravediggerEntity;
import com.morecritters.fabric.module.gravedigger.GravediggerModule;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** The gravedigger: burrows and digs up a mob while a player watches; drops its appendage; the Amalgam doll and jar. */
public class GravediggerTests {
	/** Everything a dig can throw out of the ground (GravediggerOnEntityTickUpdateProcedure), besides the Amalgam. */
	private static final Set<EntityType<?>> DUG_UP = Set.of(EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.SPIDER,
		EntityTypes.CAVE_SPIDER, EntityTypes.ZOMBIE_VILLAGER, EntityTypes.ENDERMAN);

	@GameTest(maxTicks = 200)
	public void aWatchedGravediggerBurrowsAndDigsUpAMob(GameTestHelper helper) {
		TestScenes.floor(helper);
		GravediggerEntity digger = helper.spawn(GravediggerModule.GRAVEDIGGER, new BlockPos(1, 1, 3));
		Player watcher = playerInLevel(helper, GameType.SURVIVAL, new BlockPos(6, 1, 3));
		boolean[] burrowed = {false};
		helper.startSequence()
			.thenWaitUntil(() -> {
				if (digger.isDigging()) burrowed[0] = true;
				helper.assertTrue(burrowed[0], "the gravedigger burrows within 5 s of seeing a survival player");
				helper.assertFalse(dugUpNear(helper, digger).isEmpty(), "a mob dug up beside the gravedigger");
			})
			.thenExecute(watcher::discard)
			.thenSucceed();
	}

	@GameTest(maxTicks = 200)
	public void aBurrowingGravediggerHoldsStill(GameTestHelper helper) {
		TestScenes.floor(helper);
		GravediggerEntity digger = helper.spawn(GravediggerModule.GRAVEDIGGER, new BlockPos(1, 1, 3));
		Player watcher = playerInLevel(helper, GameType.SURVIVAL, new BlockPos(6, 1, 3));
		Vec3[] start = {null};
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(digger.isDigging(), "burrowing"))
			.thenExecute(() -> start[0] = digger.position())
			.thenExecuteFor(30, () -> helper.assertTrue(horizontal(digger.position(), start[0]) < 0.01,
				"the gravedigger stays put while burrowing (it does not flee the player)"))
			.thenExecute(watcher::discard)
			.thenSucceed();
	}

	@GameTest(maxTicks = 200)
	public void noDigForACreativePlayer(GameTestHelper helper) {
		TestScenes.floor(helper);
		GravediggerEntity digger = helper.spawn(GravediggerModule.GRAVEDIGGER, new BlockPos(1, 1, 3));
		Player watcher = playerInLevel(helper, GameType.CREATIVE, new BlockPos(6, 1, 3));
		helper.startSequence()
			.thenExecuteFor(150, () -> helper.assertFalse(digger.isDigging(), "burrowing for a creative player"))
			.thenExecute(watcher::discard)
			.thenSucceed();
	}

	@GameTest(maxTicks = 200)
	public void noDigWhenThePlayerIsOutOfSight(GameTestHelper helper) {
		TestScenes.floor(helper);
		for (int y = 1; y < 6; y++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(4, y, z, Blocks.STONE);
			}
		}
		GravediggerEntity digger = helper.spawn(GravediggerModule.GRAVEDIGGER, new BlockPos(1, 1, 3));
		Player watcher = playerInLevel(helper, GameType.SURVIVAL, new BlockPos(6, 1, 3));
		helper.startSequence()
			.thenExecuteFor(150, () -> helper.assertFalse(digger.isDigging(), "burrowing with a wall between it and the player"))
			.thenExecute(watcher::discard)
			.thenSucceed();
	}

	@GameTest(maxTicks = 200)
	public void noDigWithNobodyAround(GameTestHelper helper) {
		TestScenes.floor(helper);
		GravediggerEntity digger = helper.spawn(GravediggerModule.GRAVEDIGGER, new BlockPos(3, 1, 3));
		helper.startSequence()
			.thenExecuteFor(150, () -> helper.assertFalse(digger.isDigging(), "burrowing with no player around"))
			.thenSucceed();
	}

	@GameTest
	public void takesNoFallDamage(GameTestHelper helper) {
		TestScenes.floor(helper);
		GravediggerEntity digger = helper.spawn(GravediggerModule.GRAVEDIGGER, new BlockPos(3, 1, 3));
		digger.hurtServer(helper.getLevel(), helper.getLevel().damageSources().fall(), 8.0F);
		helper.assertValueEqual(digger.getHealth(), 10.0F, "health after fall damage");
		digger.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 3.0F);
		helper.assertValueEqual(digger.getHealth(), 7.0F, "health after other damage");
		helper.succeed();
	}

	@GameTest
	public void dropsItsAppendageOnDeath(GameTestHelper helper) {
		TestScenes.floor(helper);
		GravediggerEntity digger = helper.spawn(GravediggerModule.GRAVEDIGGER, new BlockPos(3, 1, 3));
		helper.kill(digger);
		helper.succeedWhen(() -> helper.assertItemEntityPresent(item(GravediggerIds.Items.GRAVEDIGGER_APPENDAGE), new BlockPos(3, 1, 3), 2.0));
	}

	@GameTest
	public void theDollSummonsAnAmalgamOnTheClickedFaceAndIsUsedUp(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(3, 1, 3, Blocks.STONE);
		Player survival = helper.makeMockPlayer(GameType.SURVIVAL);
		survival.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(GravediggerIds.Items.AMALGAM_SPAWN_DOLL), 2));
		TestScenes.useItemOn(helper, survival, new BlockPos(3, 1, 3), Direction.UP);
		helper.assertValueEqual(survival.getMainHandItem().getCount(), 1, "dolls left in survival");

		// Used up in creative too, as in the original (AmalgamSpawnDollRightClickProcedure shrinks unconditionally).
		Player creative = helper.makeMockPlayer(GameType.CREATIVE);
		GameType.CREATIVE.updatePlayerAbilities(creative.getAbilities());
		creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(GravediggerIds.Items.AMALGAM_SPAWN_DOLL), 2));
		TestScenes.useItemOn(helper, creative, new BlockPos(3, 1, 3), Direction.EAST);
		helper.assertValueEqual(creative.getMainHandItem().getCount(), 1, "dolls left in creative");

		helper.assertEntityPresent(GravediggerModule.AMALGAM, new BlockPos(3, 2, 3));
		helper.assertEntityPresent(GravediggerModule.AMALGAM, new BlockPos(4, 1, 3));
		helper.succeed();
	}

	@GameTest
	public void theAmalgamNeverDespawns(GameTestHelper helper) {
		TestScenes.floor(helper);
		AmalgamEntity amalgam = helper.spawn(GravediggerModule.AMALGAM, new BlockPos(3, 1, 3));
		helper.assertFalse(amalgam.removeWhenFarAway(10000.0), "the amalgam despawns far from players");
		helper.assertValueEqual(amalgam.getMaxHealth(), 30.0F, "amalgam max health");
		helper.succeed();
	}

	@GameTest
	public void theJarDropsItselfWhenBroken(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(3, 1, 3, GravediggerModule.JAR);
		helper.getLevel().destroyBlock(helper.absolutePos(new BlockPos(3, 1, 3)), true);
		helper.succeedWhen(() -> helper.assertItemEntityPresent(GravediggerModule.JAR_ITEM, new BlockPos(3, 1, 3), 2.0));
	}

	// --- helpers ------------------------------------------------------------------------

	/** A player standing in the world (found by entity searches), unlike a bare mock player. Discard it when done. */
	private static Player playerInLevel(GameTestHelper helper, GameType mode, BlockPos pos) {
		Player player = helper.makeMockPlayer(mode);
		Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		helper.getLevel().addFreshEntity(player);
		return player;
	}

	private static List<Mob> dugUpNear(GameTestHelper helper, GravediggerEntity digger) {
		return helper.getLevel().getEntitiesOfClass(Mob.class, digger.getBoundingBox().inflate(3.0),
			mob -> DUG_UP.contains(mob.getType()) || mob.getType() == GravediggerModule.AMALGAM);
	}

	private static double horizontal(Vec3 a, Vec3 b) {
		double dx = a.x - b.x, dz = a.z - b.z;
		return dx * dx + dz * dz;
	}

	private static Item item(Identifier id) {
		return BuiltInRegistries.ITEM.getValue(id);
	}
}
