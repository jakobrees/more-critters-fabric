package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.IropodIds;
import com.morecritters.fabric.module.iropod.IroballEntity;
import com.morecritters.fabric.module.iropod.IropodEntity;
import com.morecritters.fabric.module.iropod.IropodModule;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/** The iropod (bucketing, rolling up, shedding), the iroball (kicking, hitting, picking up) and the helmet. */
public class IropodTests {
	@GameTest
	public void aWaterBucketScoopsUpAnIropod(GameTestHelper helper) {
		TestScenes.floor(helper);
		IropodEntity iropod = helper.spawn(IropodModule.IROPOD, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));

		iropod.mobInteract(player, InteractionHand.MAIN_HAND);

		helper.assertValueEqual(player.getMainHandItem().getItem(), item(IropodIds.Items.IROPOD_BUCKET_BUCKET), "item in hand");
		helper.assertTrue(iropod.isRemoved(), "the scooped iropod is gone");
		helper.succeed();
	}

	@GameTest
	public void aBlackIropodGoesIntoItsOwnBucket(GameTestHelper helper) {
		TestScenes.floor(helper);
		IropodEntity iropod = helper.spawn(IropodModule.BLACK_IROPOD, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));

		iropod.mobInteract(player, InteractionHand.MAIN_HAND);

		helper.assertValueEqual(player.getMainHandItem().getItem(), item(IropodIds.Items.BLACK_IROPOD_BUCKET_BUCKET), "item in hand");
		helper.assertTrue(iropod.isRemoved(), "the scooped black iropod is gone");
		helper.succeed();
	}

	@GameTest
	public void anIropodWithoutABucketIsNotScooped(GameTestHelper helper) {
		TestScenes.floor(helper);
		IropodEntity iropod = helper.spawn(IropodModule.IROPOD, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));

		iropod.mobInteract(player, InteractionHand.MAIN_HAND);

		helper.assertValueEqual(player.getMainHandItem().getItem(), Items.BUCKET, "item in hand");
		helper.assertFalse(iropod.isRemoved(), "the iropod stays");
		helper.succeed();
	}

	@GameTest
	public void anIropodOnLandRollsUp(GameTestHelper helper) {
		TestScenes.floor(helper);
		IropodEntity iropod = helper.spawn(IropodModule.IROPOD, new BlockPos(3, 1, 3));
		helper.succeedWhen(() -> helper.assertTrue(save(iropod).getBooleanOr("Locked", false), "an iropod on land is rolled up"));
	}

	@GameTest(maxTicks = 200)
	public void anIropodHitInWaterHidesForFiveSecondsThenUnrolls(GameTestHelper helper) {
		pool(helper);
		IropodEntity iropod = helper.spawn(IropodModule.IROPOD, new BlockPos(3, 1, 3));
		helper.runAfterDelay(5, () -> {
			helper.assertFalse(locked(iropod), "an iropod in water is unrolled");
			iropod.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1.0F);
			helper.assertTrue(locked(iropod), "a hit iropod rolls up");
		});
		helper.runAfterDelay(95, () -> helper.assertTrue(locked(iropod), "still hiding 90 ticks after the hit"));
		helper.runAfterDelay(115, () -> {
			helper.assertFalse(locked(iropod), "unrolled 110 ticks after the hit");
			helper.succeed();
		});
	}

	@GameTest
	public void iropodsSpawnNaturallyOnlyAtOrBelowYFortyFive(GameTestHelper helper) {
		helper.assertTrue(IropodEntity.canSpawnAt(IropodModule.IROPOD, helper.getLevel(), EntitySpawnReason.NATURAL,
			new BlockPos(0, 45, 0), helper.getLevel().getRandom()), "allowed at y 45");
		helper.assertFalse(IropodEntity.canSpawnAt(IropodModule.IROPOD, helper.getLevel(), EntitySpawnReason.NATURAL,
			new BlockPos(0, 46, 0), helper.getLevel().getRandom()), "refused at y 46");
		helper.succeed();
	}

	@GameTest
	public void anIropodIsNotHurtByDrowning(GameTestHelper helper) {
		pool(helper);
		IropodEntity iropod = helper.spawn(IropodModule.IROPOD, new BlockPos(3, 1, 3));
		float health = iropod.getHealth();
		iropod.hurtServer(helper.getLevel(), helper.getLevel().damageSources().drown(), 5.0F);
		helper.assertValueEqual(iropod.getHealth(), health, "health after drowning damage");
		helper.succeed();
	}

	@GameTest
	public void anIropodShedsAMoldedShellWhenItsTimerRunsOut(GameTestHelper helper) {
		TestScenes.floor(helper);
		IropodEntity iropod = helper.spawn(IropodModule.IROPOD, new BlockPos(3, 1, 3));
		CompoundTag tag = save(iropod);
		tag.putInt("ShedTimer", 5);
		load(iropod, tag);
		helper.succeedWhen(() -> helper.assertItemEntityPresent(item(IropodIds.Items.MOLDED_SHELL), new BlockPos(3, 1, 3), 2.0));
	}

	@GameTest
	public void anEmptyHandPicksUpAnIroballAndASpikedOneAsItsOwnItem(GameTestHelper helper) {
		TestScenes.floor(helper);
		IroballEntity plain = helper.spawn(IropodModule.IROBALL, new BlockPos(2, 1, 2));
		IroballEntity spiked = helper.spawn(IropodModule.IROBALL, new BlockPos(5, 1, 5));
		spiked.setSturdy(true);

		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		plain.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(player.getMainHandItem().getItem(), item(IropodIds.Items.IROBALL_ITEM), "picked-up plain ball");
		helper.assertTrue(plain.isRemoved(), "the plain ball is gone");

		Player other = helper.makeMockPlayer(GameType.SURVIVAL);
		spiked.mobInteract(other, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(other.getMainHandItem().getItem(), item(IropodIds.Items.SPIKED_IROBALL), "picked-up spiked ball");
		helper.assertTrue(spiked.isRemoved(), "the spiked ball is gone");
		helper.succeed();
	}

	@GameTest
	public void aFullHandDoesNotPickUpAnIroball(GameTestHelper helper) {
		TestScenes.floor(helper);
		IroballEntity ball = helper.spawn(IropodModule.IROBALL, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		ball.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(player.getMainHandItem().getItem(), Items.STICK, "item in hand");
		helper.assertFalse(ball.isRemoved(), "the ball stays");
		helper.succeed();
	}

	@GameTest
	public void iroballItemsPlaceAPlainOrSpikedBallOnTheClickedFace(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(IropodIds.Items.IROBALL_ITEM)));
		TestScenes.useItemOn(helper, player, new BlockPos(1, 0, 1), Direction.UP);
		helper.assertTrue(player.getMainHandItem().isEmpty(), "the iroball item is used up");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(IropodIds.Items.SPIKED_IROBALL)));
		TestScenes.useItemOn(helper, player, new BlockPos(6, 0, 6), Direction.UP);
		helper.assertTrue(player.getMainHandItem().isEmpty(), "the spiked iroball item is used up");

		IroballEntity plain = helper.findClosestEntity(IropodModule.IROBALL, 1, 1, 1, 1.0);
		IroballEntity spiked = helper.findClosestEntity(IropodModule.IROBALL, 6, 1, 6, 1.0);
		helper.assertFalse(plain.sturdy(), "the plain ball is not spiked");
		helper.assertTrue(spiked.sturdy(), "the spiked ball is spiked");
		helper.succeed();
	}

	@GameTest
	public void hittingAnIroballKnocksItAwayWithoutHurtingIt(GameTestHelper helper) {
		TestScenes.floor(helper);
		IroballEntity ball = helper.spawn(IropodModule.IROBALL, new BlockPos(4, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setPos(helper.absoluteVec(new Vec3(2.5, 1.0, 3.5)));

		ball.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 6.0F);

		helper.assertValueEqual(ball.getHealth(), ball.getMaxHealth(), "ball health after the hit");
		Vec3 motion = ball.getDeltaMovement();
		helper.assertTrue(motion.x > 2.5, "the ball flies away from the attacker (east); motion " + motion);
		helper.assertTrue(Math.abs(motion.length() - 3.0) < 0.01, "knocked away by half the damage; speed " + motion.length());
		helper.succeed();
	}

	@GameTest
	public void walkingIntoAnIroballKicksItTheWayTheKickerLooks(GameTestHelper helper) {
		TestScenes.floor(helper);
		Zombie kicker = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(2, 1, 3));
		face(kicker, -90.0F, 0.0F); // looking east
		IroballEntity ball = helper.spawn(IropodModule.IROBALL, new Vec3(2.9, 1.0, 3.5));
		double startX = ball.getX();
		helper.succeedWhen(() -> helper.assertTrue(ball.getX() > startX + 1.5, "the ball rolls east; moved " + (ball.getX() - startX)));
	}

	@GameTest
	public void anIroballFallingOnAMobHitsItForTen(GameTestHelper helper) {
		flyingBallHits(helper, false, 10.0F);
	}

	@GameTest
	public void aSpikedIroballFallingOnAMobHitsItForTwenty(GameTestHelper helper) {
		flyingBallHits(helper, true, 20.0F);
	}

	@GameTest
	public void theIropodHelmetGivesResistanceWhileSneaking(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack helmet = new ItemStack(item(IropodIds.Items.IROPOD_HELMET_HELMET));
		player.setItemSlot(EquipmentSlot.HEAD, helmet);

		helmet.inventoryTick(helper.getLevel(), player, EquipmentSlot.HEAD);
		helper.assertFalse(player.hasEffect(MobEffects.RESISTANCE), "no Resistance while standing");

		player.setShiftKeyDown(true);
		helmet.inventoryTick(helper.getLevel(), player, EquipmentSlot.HEAD);
		helper.assertLivingEntityHasMobEffect(player, MobEffects.RESISTANCE, 0);
		helper.succeed();
	}

	// --- helpers --------------------------------------------------------------------------

	/** A ball dropped onto an iron golem's head has been airborne for a few ticks when it touches, so it hits. */
	private static void flyingBallHits(GameTestHelper helper, boolean spiked, float damage) {
		TestScenes.floor(helper);
		IronGolem golem = helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new BlockPos(3, 1, 3));
		IroballEntity ball = helper.spawn(IropodModule.IROBALL, new Vec3(3.5, 1.0 + golem.getBbHeight() + 1.0, 3.5));
		ball.setSturdy(spiked);
		float health = golem.getHealth();
		helper.runAfterDelay(10, () -> {
			helper.assertValueEqual(golem.getHealth(), health - damage, "golem health after the ball fell on it");
			helper.succeed();
		});
	}

	private static void face(Entity entity, float yRot, float xRot) {
		entity.setYRot(yRot);
		entity.setYHeadRot(yRot);
		entity.setYBodyRot(yRot);
		entity.setXRot(xRot);
	}

	private static void pool(GameTestHelper helper) {
		TestScenes.floor(helper);
		for (int x = 0; x < 8; x++) {
			for (int y = 1; y <= 3; y++) {
				for (int z = 0; z < 8; z++) {
					helper.setBlock(x, y, z, Blocks.WATER);
				}
			}
		}
	}

	private static boolean locked(IropodEntity iropod) {
		return save(iropod).getBooleanOr("Locked", false);
	}

	private static CompoundTag save(Entity entity) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
		entity.saveWithoutId(output);
		return output.buildResult();
	}

	private static void load(Entity entity, CompoundTag tag) {
		entity.load(TagValueInput.create(ProblemReporter.DISCARDING, entity.registryAccess(), tag));
	}

	private static Item item(Identifier id) {
		return BuiltInRegistries.ITEM.getValue(id);
	}
}
