package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.CustodianIds;
import com.morecritters.fabric.ids.NervoidIds;
import com.morecritters.fabric.module.custodian.AncientCustodianEntity;
import com.morecritters.fabric.module.custodian.CustodianEntity;
import com.morecritters.fabric.module.custodian.CustodianModule;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The custodian: charges at a Warden or monsters and fires, then shuts down; Wardens, arrows and falls cannot
 * hurt it; the ancient custodian wakes on three echo shards; the core block builds one from its frame; and the
 * under_control effect. Tests with a working custodian get wide padding, because its blast reaches 25 blocks.
 */
public class CustodianTests {
	/** Keeps a live custodian's 32-block target search and 25-block blast away from other tests. */
	private static final int CUSTODIAN_PADDING = 40;

	@GameTest(maxTicks = 200, padding = CUSTODIAN_PADDING)
	public void chargesFiveSecondsThenBlastsMonstersAndShutsDown(GameTestHelper helper) {
		TestScenes.floor(helper);
		CustodianEntity custodian = helper.spawn(CustodianModule.CUSTODIAN, new BlockPos(1, 1, 1));
		Husk husk = helper.spawn(EntityTypes.HUSK, new BlockPos(6, 1, 6));
		helper.runAtTickTime(80, () -> {
			helper.assertTrue(husk.isAlive(), "the husk survives the charge's first four seconds");
			helper.assertFalse(custodian.closed(), "still charging, not shut down");
		});
		helper.succeedWhen(() -> {
			helper.assertFalse(husk.isAlive(), "the husk is killed by the blast");
			helper.assertTrue(custodian.closed(), "the custodian shuts down after firing");
		});
	}

	@GameTest(maxTicks = 200, padding = CUSTODIAN_PADDING)
	public void aWardenComesFirstAndIsKilledLeavingSculkEssence(GameTestHelper helper) {
		TestScenes.floor(helper);
		CustodianEntity custodian = helper.spawn(CustodianModule.CUSTODIAN, new BlockPos(1, 1, 1));
		// The husk is nearer, but Wardens are its first targets, and a Warden shot blasts nothing else.
		Husk husk = helper.spawn(EntityTypes.HUSK, new BlockPos(3, 1, 1));
		husk.setNoAi(true);
		Warden warden = helper.spawn(EntityTypes.WARDEN, new BlockPos(6, 1, 6));
		// Held in place: a roaming Warden walks out of the test area and is cleared away with a neighbouring test.
		warden.setNoAi(true);
		helper.succeedWhen(() -> {
			helper.assertFalse(warden.isAlive(), "the Warden is killed");
			helper.assertItemEntityPresent(item(CustodianIds.Items.SCULK_ESSENCE));
			helper.assertTrue(custodian.closed(), "the custodian shuts down after firing");
			helper.assertTrue(husk.isAlive(), "the husk is spared by a Warden shot");
		});
	}

	@GameTest
	public void aWardensBlowsDoNotHurtIt(GameTestHelper helper) {
		TestScenes.floor(helper);
		CustodianEntity custodian = helper.spawn(CustodianModule.CUSTODIAN, new BlockPos(2, 1, 2));
		custodian.setNoAi(true);
		Warden warden = helper.spawn(EntityTypes.WARDEN, new BlockPos(5, 1, 5));
		warden.setNoAi(true);
		ServerLevel level = helper.getLevel();
		boolean hurt = custodian.hurtServer(level, level.damageSources().mobAttack(warden), 30.0F);
		helper.assertFalse(hurt, "a Warden's blow lands");
		helper.assertValueEqual(custodian.getHealth(), 150.0F, "custodian health after a Warden's blow");
		helper.succeed();
	}

	@GameTest
	public void arrowsFallsAndCactiDoNotHurtItButAPlayerDoes(GameTestHelper helper) {
		TestScenes.floor(helper);
		CustodianEntity custodian = helper.spawn(CustodianModule.CUSTODIAN, new BlockPos(2, 1, 2));
		custodian.setNoAi(true);
		ServerLevel level = helper.getLevel();
		Arrow arrow = helper.spawn(EntityTypes.ARROW, new BlockPos(5, 1, 5));
		custodian.hurtServer(level, level.damageSources().arrow(arrow, null), 10.0F);
		helper.assertValueEqual(custodian.getHealth(), 150.0F, "health after an arrow");
		custodian.hurtServer(level, level.damageSources().fall(), 10.0F);
		helper.assertValueEqual(custodian.getHealth(), 150.0F, "health after a fall");
		custodian.hurtServer(level, level.damageSources().cactus(), 10.0F);
		helper.assertValueEqual(custodian.getHealth(), 150.0F, "health after a cactus");

		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		custodian.hurtServer(level, level.damageSources().playerAttack(player), 10.0F);
		helper.assertValueEqual(custodian.getHealth(), 140.0F, "health after a player's blow");
		helper.succeed();
	}

	@GameTest
	public void itsOwnSwipesDoNoDamage(GameTestHelper helper) {
		TestScenes.floor(helper);
		CustodianEntity custodian = helper.spawn(CustodianModule.CUSTODIAN, new BlockPos(2, 1, 2));
		custodian.setNoAi(true);
		Husk husk = helper.spawn(EntityTypes.HUSK, new BlockPos(3, 1, 2));
		husk.setNoAi(true);
		custodian.doHurtTarget(helper.getLevel(), husk);
		helper.assertValueEqual(husk.getHealth(), husk.getMaxHealth(), "husk health after a custodian's swipe");
		helper.succeed();
	}

	@GameTest(maxTicks = 200, padding = CUSTODIAN_PADDING)
	public void wakesAgainWhenTheShutdownEnds(GameTestHelper helper) {
		TestScenes.floor(helper);
		CustodianEntity custodian = helper.spawn(CustodianModule.CUSTODIAN, new BlockPos(1, 1, 1));
		Husk husk = helper.spawn(EntityTypes.HUSK, new BlockPos(6, 1, 6));
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(custodian.closed(), "shut down after firing"))
			.thenExecute(() -> {
				helper.assertFalse(husk.isAlive(), "the husk was blasted");
				// Fast-forward the ten-minute shutdown to its last second.
				setSavedInt(helper, custodian, "ShutdownTimer", 20);
			})
			.thenExecuteAfter(10, () -> helper.assertTrue(custodian.closed(), "still shut down with 10 ticks left"))
			.thenWaitUntil(() -> helper.assertFalse(custodian.closed(), "awake again once the shutdown ends"))
			.thenSucceed();
	}

	@GameTest(maxTicks = 100, padding = CUSTODIAN_PADDING)
	public void threeEchoShardsAwakenAnAncientCustodian(GameTestHelper helper) {
		TestScenes.floor(helper);
		AncientCustodianEntity ancient = helper.spawn(CustodianModule.ANCIENT_CUSTODIAN, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ECHO_SHARD, 5));

		rightClick(player, ancient);
		rightClick(player, ancient);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 3, "shards left after two");
		rightClick(player, ancient);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "shards left after three");

		helper.runAtTickTime(20, () -> {
			helper.assertTrue(ancient.isAlive(), "it opens up for a moment before it is replaced");
			helper.assertEntityNotPresent(CustodianModule.CUSTODIAN);
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(ancient.isRemoved(), "the ancient custodian is replaced");
			CustodianEntity custodian = helper.findOneEntity(CustodianModule.CUSTODIAN);
			helper.assertValueEqual(custodian.textureName(), "ancient_custodian", "the awakened custodian's texture");
		});
	}

	@GameTest(maxTicks = 60)
	public void twoEchoShardsAreNotEnough(GameTestHelper helper) {
		TestScenes.floor(helper);
		AncientCustodianEntity ancient = helper.spawn(CustodianModule.ANCIENT_CUSTODIAN, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ECHO_SHARD, 2));
		rightClick(player, ancient);
		rightClick(player, ancient);
		helper.assertTrue(player.getMainHandItem().isEmpty(), "both shards are eaten");
		helper.runAfterDelay(45, () -> {
			helper.assertTrue(ancient.isAlive(), "still dormant");
			helper.assertEntityNotPresent(CustodianModule.CUSTODIAN);
			helper.succeed();
		});
	}

	@GameTest
	public void creativeShardsAreNotUsedUp(GameTestHelper helper) {
		TestScenes.floor(helper);
		AncientCustodianEntity ancient = helper.spawn(CustodianModule.ANCIENT_CUSTODIAN, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		// The mock player reports the game mode but does not take on its abilities by itself.
		GameType.CREATIVE.updatePlayerAbilities(player.getAbilities());
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ECHO_SHARD, 5));
		rightClick(player, ancient);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 5, "creative shards after feeding");
		helper.succeed();
	}

	@GameTest
	public void anAncientCustodianShrugsOffBlowsAndFalls(GameTestHelper helper) {
		TestScenes.floor(helper);
		AncientCustodianEntity ancient = helper.spawn(CustodianModule.ANCIENT_CUSTODIAN, new BlockPos(3, 1, 3));
		ServerLevel level = helper.getLevel();
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Husk husk = helper.spawn(EntityTypes.HUSK, new BlockPos(6, 1, 6));
		husk.setNoAi(true);
		ancient.hurtServer(level, level.damageSources().playerAttack(player), 10.0F);
		ancient.hurtServer(level, level.damageSources().mobAttack(husk), 10.0F);
		ancient.hurtServer(level, level.damageSources().fall(), 10.0F);
		helper.assertValueEqual(ancient.getHealth(), ancient.getMaxHealth(), "ancient custodian health");
		helper.succeed();
	}

	@GameTest(maxTicks = 40, padding = CUSTODIAN_PADDING)
	public void aCompleteFrameAroundTheCoreBecomesACustodian(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos core = new BlockPos(3, 2, 3);
		buildFrame(helper, core, null);
		helper.setBlock(core, block(CustodianIds.Blocks.CUSTODIAN_CORE));
		helper.succeedWhen(() -> {
			helper.assertValueEqual(helper.getEntities(CustodianModule.CUSTODIAN).size(), 1, "custodians built");
			for (BlockPos pos : BlockPos.betweenClosed(core.offset(-1, -1, -1), core.offset(1, 1, 1))) {
				helper.assertBlockPresent(Blocks.AIR, pos);
			}
			helper.assertItemEntityNotPresent(Items.BONE_BLOCK);
			helper.assertItemEntityNotPresent(Items.DEEPSLATE_BRICKS);
		});
	}

	@GameTest(maxTicks = 40)
	public void anIncompleteFrameStaysAsItIs(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos core = new BlockPos(3, 2, 3);
		// One edge block is deepslate bricks instead of a bone block.
		buildFrame(helper, core, core.offset(1, 1, 1));
		helper.setBlock(core, block(CustodianIds.Blocks.CUSTODIAN_CORE));
		helper.runAfterDelay(20, () -> {
			helper.assertEntityNotPresent(CustodianModule.CUSTODIAN);
			helper.assertBlockPresent(block(CustodianIds.Blocks.CUSTODIAN_CORE), core);
			helper.assertBlockPresent(Blocks.BONE_BLOCK, core.offset(-1, -1, -1));
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 80)
	public void aNervoidLeavesItsHostWhenUnderControlEnds(GameTestHelper helper) {
		TestScenes.floor(helper);
		Husk husk = helper.spawn(EntityTypes.HUSK, new BlockPos(3, 1, 3));
		husk.addEffect(new MobEffectInstance(CustodianModule.UNDER_CONTROL, 20));
		EntityType<?> nervoid = BuiltInRegistries.ENTITY_TYPE.getValue(NervoidIds.Entities.NERVOID);
		helper.runAtTickTime(10, () -> helper.assertEntityNotPresent(nervoid));
		helper.succeedWhen(() -> {
			helper.assertFalse(husk.hasEffect(CustodianModule.UNDER_CONTROL), "the effect has ended");
			helper.assertEntityPresent(nervoid);
		});
	}

	/**
	 * Deepslate bricks above, below and on the four faces of the core, bone blocks on the eight vertical edges
	 * (the corners of each layer); {@code wrong} gets deepslate bricks instead of its proper block.
	 */
	private static void buildFrame(GameTestHelper helper, BlockPos core, @Nullable BlockPos wrong) {
		for (BlockPos pos : BlockPos.betweenClosed(core.offset(-1, -1, -1), core.offset(1, 1, 1))) {
			int dx = pos.getX() - core.getX(), dz = pos.getZ() - core.getZ();
			if (dx == 0 && dz == 0 && pos.getY() == core.getY()) {
				continue;
			}
			boolean edge = dx != 0 && dz != 0;
			helper.setBlock(pos.immutable(), edge && !pos.equals(wrong) ? Blocks.BONE_BLOCK : Blocks.DEEPSLATE_BRICKS);
		}
	}

	/** A right-click on an entity as the server handles it: Fabric's use-entity event first, then the entity. */
	private static InteractionResult rightClick(Player player, Entity target) {
		InteractionResult result = UseEntityCallback.EVENT.invoker().interact(player, player.level(), InteractionHand.MAIN_HAND, target, null);
		if (result != InteractionResult.PASS) {
			return result;
		}
		return player.interactOn(target, InteractionHand.MAIN_HAND, target.position());
	}

	/** Rewrites one saved field of an entity, for timers that are not otherwise reachable. */
	private static void setSavedInt(GameTestHelper helper, Entity entity, String key, int value) {
		var registries = helper.getLevel().registryAccess();
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		entity.saveWithoutId(output);
		CompoundTag tag = output.buildResult();
		tag.putInt(key, value);
		entity.load(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
	}

	private static Item item(Identifier id) {
		return BuiltInRegistries.ITEM.getValue(id);
	}

	private static Block block(Identifier id) {
		return BuiltInRegistries.BLOCK.getValue(id);
	}
}
