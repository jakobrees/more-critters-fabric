package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.TreepletIds;
import com.morecritters.fabric.module.treeplet.ResinPuddleEntity;
import com.morecritters.fabric.module.treeplet.SplinterProjectile;
import com.morecritters.fabric.module.treeplet.TreepletEntity;
import com.morecritters.fabric.module.treeplet.TreepletModule;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The treeplet: falling apart into treeplings, its resin puddles, the eerie dart, the snow cone and stripping eerie birch. */
public class TreepletTests {
	@GameTest
	public void aKilledTreepletFallsApartIntoThreeTreeplings(GameTestHelper helper) {
		TestScenes.floor(helper);
		TreepletEntity treeplet = helper.spawn(TreepletModule.TREEPLET, new BlockPos(3, 1, 3));
		helper.kill(treeplet);
		helper.assertTrue(treeplet.isRemoved(), "the treeplet is gone at once");
		helper.assertValueEqual(helper.getEntities(TreepletModule.TREEPLING_TOP).size(), 1, "top treeplings");
		helper.assertValueEqual(helper.getEntities(TreepletModule.TREEPLING_MIDDLE).size(), 1, "middle treeplings");
		helper.assertValueEqual(helper.getEntities(TreepletModule.TREEPLING_BOTTOM).size(), 1, "bottom treeplings");
		double base = treeplet.getY();
		helper.assertValueEqual(helper.getEntities(TreepletModule.TREEPLING_TOP).getFirst().getBlockY() - (int) Math.floor(base), 2, "top treepling's height");
		helper.assertValueEqual(helper.getEntities(TreepletModule.TREEPLING_MIDDLE).getFirst().getBlockY() - (int) Math.floor(base), 1, "middle treepling's height");
		helper.succeed();
	}

	@GameTest
	public void aTreepletIsNotHurtByCactus(GameTestHelper helper) {
		TestScenes.floor(helper);
		TreepletEntity treeplet = helper.spawnWithNoFreeWill(TreepletModule.TREEPLET, new BlockPos(3, 1, 3));
		helper.assertFalse(treeplet.hurtServer(helper.getLevel(), helper.getLevel().damageSources().cactus(), 4.0F), "cactus hurt it");
		helper.assertValueEqual(treeplet.getHealth(), 20.0F, "health");
		helper.succeed();
	}

	@GameTest(maxTicks = 60)
	public void aFleeingTreepletSpitsAResinPuddle(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.spawn(TreepletModule.TREEPLET, new BlockPos(1, 1, 1));
		Player player = playerInWorld(helper, GameType.SURVIVAL, new BlockPos(5, 1, 5));
		// The first spit comes 25 ticks after it appears, if a survival player is in sight.
		helper.succeedWhen(() -> {
			helper.assertEntityPresent(TreepletModule.RESIN_PUDDLE);
			player.discard();
		});
	}

	@GameTest(maxTicks = 60)
	public void aTreepletDoesNotSpitAtACreativePlayer(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.spawn(TreepletModule.TREEPLET, new BlockPos(1, 1, 1));
		Player player = playerInWorld(helper, GameType.CREATIVE, new BlockPos(5, 1, 5));
		helper.startSequence()
			.thenExecuteAfter(45, () -> {
				helper.assertEntityNotPresent(TreepletModule.RESIN_PUDDLE);
				player.discard();
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 80)
	public void aResinPuddleDriesUpInNineStagesOverFortyFiveTicks(GameTestHelper helper) {
		TestScenes.floor(helper);
		ResinPuddleEntity puddle = helper.spawn(TreepletModule.RESIN_PUDDLE, new BlockPos(3, 1, 3));
		helper.startSequence()
			.thenExecuteAfter(2, () -> helper.assertValueEqual(puddle.textureName(), "resin_puddle1", "texture at first"))
			.thenExecuteAfter(20, () -> helper.assertValueEqual(puddle.textureName(), "resin_puddle5", "texture after 22 ticks"))
			.thenExecuteAfter(16, () -> helper.assertValueEqual(puddle.textureName(), "resin_puddle8", "texture after 38 ticks"))
			.thenExecuteAfter(10, () -> helper.assertTrue(puddle.isRemoved(), "the puddle is gone after 45 ticks"))
			.thenSucceed();
	}

	@GameTest(maxTicks = 40)
	public void aResinPuddleSlowsMostCreaturesButSpeedsUpTheUndead(GameTestHelper helper) {
		TestScenes.floor(helper);
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 1, 3));
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(3, 1, 4));
		helper.spawn(TreepletModule.RESIN_PUDDLE, new BlockPos(3, 1, 3));
		helper.succeedWhen(() -> {
			MobEffectInstance slowness = pig.getEffect(MobEffects.SLOWNESS);
			helper.assertTrue(slowness != null && slowness.getAmplifier() == 2, "the pig has Slowness III");
			MobEffectInstance speed = zombie.getEffect(MobEffects.SPEED);
			helper.assertTrue(speed != null && speed.getAmplifier() == 1, "the zombie has Speed II");
			helper.assertFalse(zombie.hasEffect(MobEffects.SLOWNESS), "the zombie is slowed");
		});
	}

	@GameTest
	public void throwingAnEerieDartUsesOneUp(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(new Vec3(3.5, 1.0, 1.5)), 0.0F, 0.0F);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TreepletModule.EERIE_DART, 3));
		player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "darts left");
		helper.assertEntityPresent(TreepletModule.SPLINTER);
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void eachEerieDartHitSlowsOneLevelMore(GameTestHelper helper) {
		TestScenes.floor(helper);
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 1, 3));
		SplinterProjectile first = dropDart(helper, new BlockPos(3, 4, 3));
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(first.isRemoved(), "the first dart hit"))
			.thenExecute(() -> {
				MobEffectInstance slowness = pig.getEffect(MobEffects.SLOWNESS);
				helper.assertTrue(slowness != null && slowness.getAmplifier() == 0 && slowness.getDuration() > 50, "Slowness I for 3 s after one dart");
				// Hurt cooldown: wait it out before the second dart.
			})
			.thenIdle(12)
			.thenExecute(() -> {
				// The first hit knocked the pig aside; put it back under the second dart.
				pig.snapTo(helper.absoluteVec(new Vec3(3.5, 1.0, 3.5)), 0.0F, 0.0F);
				pig.setDeltaMovement(Vec3.ZERO);
				dropDart(helper, new BlockPos(3, 4, 3));
			})
			.thenWaitUntil(() -> {
				MobEffectInstance slowness = pig.getEffect(MobEffects.SLOWNESS);
				helper.assertTrue(slowness != null && slowness.getAmplifier() == 1, "Slowness II after a second dart");
			})
			.thenSucceed();
	}

	@GameTest
	public void aBirchSnowConeIsEatenInFourBites(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack cone = new ItemStack(item(TreepletIds.Items.BIRCH_SNOW_CONE));
		cone = cone.finishUsingItem(helper.getLevel(), player);
		helper.assertTrue(cone.is(item(TreepletIds.Items.BIRCH_SNOW_CONE_1)), "after one bite: " + cone);
		cone = cone.finishUsingItem(helper.getLevel(), player);
		helper.assertTrue(cone.is(item(TreepletIds.Items.BIRCH_SNOW_CONE_2)), "after two bites: " + cone);
		cone = cone.finishUsingItem(helper.getLevel(), player);
		helper.assertTrue(cone.is(item(TreepletIds.Items.BIRCH_SNOW_CONE_3)), "after three bites: " + cone);
		cone = cone.finishUsingItem(helper.getLevel(), player);
		helper.assertTrue(cone.isEmpty(), "after four bites: " + cone);
		helper.succeed();
	}

	@GameTest
	public void anAxeStripsEerieBirchLogIntoStrippedBirchKeepingItsAxis(GameTestHelper helper) {
		strips(helper, TreepletIds.Blocks.EERIE_BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG);
	}

	@GameTest
	public void anAxeStripsEerieBirchWoodIntoStrippedBirchWood(GameTestHelper helper) {
		strips(helper, TreepletIds.Blocks.EERIE_BIRCH_WOOD, Blocks.STRIPPED_BIRCH_WOOD);
	}

	private static void strips(GameTestHelper helper, net.minecraft.resources.Identifier eerie, Block stripped) {
		BlockPos pos = new BlockPos(3, 1, 3);
		Block block = BuiltInRegistries.BLOCK.getValue(eerie);
		helper.setBlock(pos, block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
		BlockPos absolute = helper.absolutePos(pos);
		UseBlockCallback.EVENT.invoker().interact(player, helper.getLevel(), InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
		helper.assertBlockPresent(stripped, pos);
		helper.assertValueEqual(helper.getBlockState(pos).getValue(RotatedPillarBlock.AXIS), Direction.Axis.X, "axis");
		helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 1, "axe damage");
		helper.succeed();
	}

	/** An eerie dart falling straight down from {@code pos}. */
	private static SplinterProjectile dropDart(GameTestHelper helper, BlockPos pos) {
		SplinterProjectile dart = helper.spawn(TreepletModule.SPLINTER, pos);
		dart.setDeltaMovement(0.0, -1.0, 0.0);
		return dart;
	}

	private static Item item(net.minecraft.resources.Identifier id) {
		return BuiltInRegistries.ITEM.getValue(id);
	}

	/**
	 * A player standing still in the level (the treeplet looks for players in the world). Invisible, so the
	 * monsters of neighbouring tests ignore it; discarded when the test passes or just before it times out.
	 */
	private static Player playerInWorld(GameTestHelper helper, GameType mode, BlockPos pos) {
		Player player = helper.makeMockPlayer(mode);
		mode.updatePlayerAbilities(player.getAbilities());
		player.snapTo(helper.absoluteVec(Vec3.atBottomCenterOf(pos)), 0.0F, 0.0F);
		player.setInvisible(true);
		helper.getLevel().addFreshEntity(player);
		helper.runBeforeTestEnd(player::discard);
		return player;
	}
}
