package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.AvoiderIds;
import com.morecritters.fabric.module.avoider.AvoiderEntity;
import com.morecritters.fabric.module.avoider.AvoiderFryEntity;
import com.morecritters.fabric.module.avoider.AvoiderModule;
import java.lang.reflect.Field;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** The avoider: flees and panics at players, tears kelp, hatches as a fry; buckets and the booster pump. */
public class AvoiderTests {
	private static final BlockPos FISH = new BlockPos(4, 2, 4);

	@GameTest(maxTicks = 40)
	public void aSurvivalPlayerInSightScaresIt(GameTestHelper helper) {
		waterTank(helper);
		AvoiderEntity avoider = helper.spawnWithNoFreeWill(AvoiderModule.AVOIDER, FISH);
		ServerPlayer player = playerInLevel(helper, GameType.SURVIVAL, new Vec3(1.5, 2.0, 1.5));
		helper.succeedWhen(() -> {
			helper.assertTrue(avoider.isScared(), "the avoider is scared of a survival player in sight");
			helper.assertValueEqual(avoider.textureName(), "avoider_scared", "texture of a scared avoider");
			helper.assertTrue(avoider.hasEffect(MobEffects.DOLPHINS_GRACE), "a scared avoider has dolphin's grace");
			removePlayer(helper, player);
		});
	}

	@GameTest(maxTicks = 40)
	public void aCreativePlayerDoesNotScareIt(GameTestHelper helper) {
		waterTank(helper);
		AvoiderEntity avoider = helper.spawnWithNoFreeWill(AvoiderModule.AVOIDER, FISH);
		ServerPlayer player = playerInLevel(helper, GameType.CREATIVE, new Vec3(1.5, 2.0, 1.5));
		helper.runAfterDelay(20, () -> {
			removePlayer(helper, player);
			helper.assertFalse(avoider.isScared(), "the avoider is scared of a creative player");
			helper.assertValueEqual(avoider.textureName(), "avoider", "texture of a calm avoider");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void aScaredAvoiderTearsThroughKelp(GameTestHelper helper) {
		waterTank(helper);
		for (int y = 1; y <= 3; y++) helper.setBlock(FISH.atY(y), Blocks.KELP_PLANT);
		helper.setBlock(FISH.atY(4), Blocks.KELP);
		AvoiderEntity avoider = helper.spawnWithNoFreeWill(AvoiderModule.AVOIDER, FISH);
		ServerPlayer player = playerInLevel(helper, GameType.SURVIVAL, new Vec3(1.5, 2.0, 1.5));
		helper.succeedWhen(() -> {
			helper.assertTrue(avoider.isScared(), "the avoider is scared");
			helper.assertBlockNotPresent(Blocks.KELP_PLANT, FISH);
			removePlayer(helper, player);
		});
	}

	@GameTest(maxTicks = 60)
	public void itLeapsOutOfTheWaterAndGlidesDown(GameTestHelper helper) {
		waterTank(helper);
		AvoiderEntity avoider = helper.spawnWithNoFreeWill(AvoiderModule.AVOIDER, FISH);
		setInt(avoider, "ticksUntilLeap", 0);
		helper.runAfterDelay(2, () -> helper.assertTrue(avoider.isLeaping(), "the avoider leaps when its timer runs out in water"));
		helper.succeedWhen(() -> {
			helper.assertFalse(avoider.isInWater(), "the avoider left the water");
			helper.assertTrue(avoider.hasEffect(MobEffects.SLOW_FALLING), "out of the water it glides with slow falling");
		});
	}

	@GameTest
	public void aWaterBucketScoopsUpAnAvoider(GameTestHelper helper) {
		TestScenes.floor(helper);
		AvoiderEntity avoider = helper.spawnWithNoFreeWill(AvoiderModule.AVOIDER, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
		avoider.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.getMainHandItem().is(item(AvoiderIds.Items.AVOIDER_BUCKET_BUCKET)), "the water bucket became an avoider bucket");
		helper.assertTrue(avoider.isRemoved(), "the scooped avoider is gone");
		helper.succeed();
	}

	@GameTest
	public void aWaterBucketScoopsUpAFry(GameTestHelper helper) {
		TestScenes.floor(helper);
		AvoiderFryEntity fry = helper.spawnWithNoFreeWill(AvoiderModule.AVOIDER_FRY, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
		fry.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.getMainHandItem().is(item(AvoiderIds.Items.AVOIDER_FRY_BUCKET_BUCKET)), "the water bucket became an avoider fry bucket");
		helper.assertTrue(fry.isRemoved(), "the scooped fry is gone");
		helper.succeed();
	}

	@GameTest
	public void anAvoiderBucketReleasesTheAvoider(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack bucket = new ItemStack(item(AvoiderIds.Items.AVOIDER_BUCKET_BUCKET));
		player.setItemInHand(InteractionHand.MAIN_HAND, bucket);
		// Buckets aim by the player's gaze: stand above the floor and look straight down.
		Vec3 eyeSpot = helper.absoluteVec(new Vec3(3.5, 2.0, 3.5));
		player.snapTo(eyeSpot.x, eyeSpot.y, eyeSpot.z, 0.0F, 90.0F);
		InteractionResult result = bucket.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertBlockPresent(Blocks.WATER, new BlockPos(3, 1, 3));
		ItemStack left = result instanceof InteractionResult.Success success ? success.heldItemTransformedTo() : null;
		helper.assertTrue(left != null && left.is(Items.BUCKET), "an empty bucket is left, got " + left);
		helper.succeedWhen(() -> helper.assertEntityPresent(AvoiderModule.AVOIDER, new BlockPos(3, 1, 3)));
	}

	@GameTest
	public void aBredAvoiderHatchesAsAFry(GameTestHelper helper) {
		waterTank(helper);
		AvoiderEntity newborn = helper.spawn(AvoiderModule.AVOIDER, FISH);
		newborn.setAge(-24000);
		helper.succeedWhen(() -> {
			helper.assertEntityPresent(AvoiderModule.AVOIDER_FRY);
			helper.assertTrue(newborn.isRemoved(), "the bred baby is replaced by a fry");
		});
	}

	@GameTest
	public void aFryGrowsIntoAnAvoider(GameTestHelper helper) {
		waterTank(helper);
		AvoiderFryEntity fry = helper.spawnWithNoFreeWill(AvoiderModule.AVOIDER_FRY, FISH);
		helper.assertFalse(fry.removeWhenFarAway(1000.0), "a fry despawns");
		setInt(fry, "ticksUntilGrown", 2);
		helper.succeedWhen(() -> {
			helper.assertEntityPresent(AvoiderModule.AVOIDER);
			helper.assertTrue(fry.isRemoved(), "the fry is replaced by the adult");
		});
	}

	@GameTest
	public void drowningDoesNotHurtIt(GameTestHelper helper) {
		TestScenes.floor(helper);
		AvoiderEntity avoider = helper.spawnWithNoFreeWill(AvoiderModule.AVOIDER, new BlockPos(3, 1, 3));
		AvoiderFryEntity fry = helper.spawnWithNoFreeWill(AvoiderModule.AVOIDER_FRY, new BlockPos(5, 1, 5));
		avoider.hurtServer(helper.getLevel(), helper.getLevel().damageSources().drown(), 4.0F);
		fry.hurtServer(helper.getLevel(), helper.getLevel().damageSources().drown(), 2.0F);
		helper.assertValueEqual(avoider.getHealth(), avoider.getMaxHealth(), "avoider health after drowning damage");
		helper.assertValueEqual(fry.getHealth(), fry.getMaxHealth(), "fry health after drowning damage");
		helper.succeed();
	}

	@GameTest
	public void killedItDropsAtMostOneTail(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		int kills = 20;
		for (int i = 0; i < kills; i++) {
			AvoiderEntity avoider = helper.spawnWithNoFreeWill(AvoiderModule.AVOIDER, new BlockPos(3, 1, 3));
			avoider.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 100.0F);
		}
		helper.runAfterDelay(5, () -> {
			var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, helper.getBounds());
			int tails = drops.stream().filter(drop -> drop.getItem().is(item(AvoiderIds.Items.AVOIDER_TAIL))).mapToInt(drop -> drop.getItem().getCount()).sum();
			helper.assertValueInBetween(1, tails, kills, "avoider tails from " + kills + " kills (0 or 1 each)");
			helper.assertTrue(drops.stream().allMatch(drop -> drop.getItem().is(item(AvoiderIds.Items.AVOIDER_TAIL))), "only tails drop");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 260)
	public void itDriesOutOnLand(GameTestHelper helper) {
		TestScenes.floor(helper);
		AvoiderEntity avoider = helper.spawnWithNoFreeWill(AvoiderModule.AVOIDER, new BlockPos(3, 1, 3));
		helper.runAfterDelay(190, () -> helper.assertValueEqual(avoider.getHealth(), avoider.getMaxHealth(), "health after 190 ticks on land"));
		helper.runAfterDelay(215, () -> {
			helper.assertTrue(avoider.getHealth() < avoider.getMaxHealth(), "a beached avoider has started drying out");
			helper.succeed();
		});
	}

	@GameTest
	public void theBoosterPumpLaunchesUpOnLand(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Vec3 feet = helper.absoluteVec(new Vec3(3.5, 1.0, 3.5));
		player.snapTo(feet.x, feet.y, feet.z, 0.0F, 0.0F);
		ItemStack pump = new ItemStack(item(AvoiderIds.Items.BOOSTER_PUMP));
		player.setItemInHand(InteractionHand.MAIN_HAND, pump);
		player.setDeltaMovement(0.1, 0.0, 0.2);

		pump.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		Vec3 motion = player.getDeltaMovement();
		helper.assertValueEqual(motion.y, 1.0, "upward speed after pumping on land");
		helper.assertTrue(Math.abs(motion.x - 0.3) < 1.0E-6 && Math.abs(motion.z - 0.6) < 1.0E-6, "horizontal speed tripled, was " + motion);
		helper.assertTrue(player.getCooldowns().isOnCooldown(pump), "the pump is on cooldown");
		helper.assertValueEqual(pump.getDamageValue(), 1, "pump durability used");
		helper.succeed();
	}

	@GameTest
	public void theBoosterPumpShovesBackwardsUnderwater(GameTestHelper helper) {
		waterTank(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Vec3 feet = helper.absoluteVec(new Vec3(3.5, 2.0, 3.5));
		player.snapTo(feet.x, feet.y, feet.z, 0.0F, 0.0F);
		player.baseTick();
		helper.assertTrue(player.isInWater(), "the player is in the water");
		ItemStack pump = new ItemStack(item(AvoiderIds.Items.BOOSTER_PUMP));
		player.setItemInHand(InteractionHand.MAIN_HAND, pump);

		pump.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		Vec3 motion = player.getDeltaMovement();
		Vec3 expected = player.getLookAngle().scale(-2.0);
		helper.assertTrue(motion.distanceTo(expected) < 1.0E-6, "pushed twice the gaze backwards: expected " + expected + ", was " + motion);
		helper.assertTrue(player.hasEffect(MobEffects.DOLPHINS_GRACE), "the pump gives dolphin's grace");
		helper.assertValueEqual(player.getEffect(MobEffects.DOLPHINS_GRACE).getAmplifier(), 2, "dolphin's grace amplifier");
		helper.assertTrue(player.getCooldowns().isOnCooldown(pump), "the pump is on cooldown");
		helper.assertValueEqual(pump.getDamageValue(), 1, "pump durability used");
		helper.succeed();
	}

	/** A glass-walled tank of still water filling the test area above a stone floor. */
	private static void waterTank(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(x, 0, z, Blocks.STONE);
				boolean wall = x == 0 || x == 7 || z == 0 || z == 7;
				for (int y = 1; y < 7; y++) {
					helper.setBlock(x, y, z, wall ? Blocks.GLASS : Blocks.WATER);
				}
			}
		}
	}

	/** A real server player in the level (so mobs can find it), in the given game mode at a test-relative spot. */
	private static ServerPlayer playerInLevel(GameTestHelper helper, GameType mode, Vec3 at) {
		@SuppressWarnings("removal")
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(mode);
		Vec3 absolute = helper.absoluteVec(at);
		player.snapTo(absolute.x, absolute.y, absolute.z, 0.0F, 0.0F);
		return player;
	}

	private static void removePlayer(GameTestHelper helper, ServerPlayer player) {
		helper.getLevel().getServer().getPlayerList().remove(player);
	}

	private static Item item(net.minecraft.resources.Identifier id) {
		return BuiltInRegistries.ITEM.getValue(id);
	}

	/** Fast-forwards one of the module's private tick counters. */
	private static void setInt(Object target, String field, int value) {
		for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
			try {
				Field f = type.getDeclaredField(field);
				f.setAccessible(true);
				f.setInt(target, value);
				return;
			} catch (NoSuchFieldException e) {
				// look in the superclass
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		}
		throw new IllegalArgumentException("no field " + field + " on " + target.getClass());
	}
}
