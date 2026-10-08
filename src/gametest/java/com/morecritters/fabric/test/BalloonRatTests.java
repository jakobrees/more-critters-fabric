package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.BalloonRatIds;
import com.morecritters.fabric.ids.SnowflakeSpiderIds;
import com.morecritters.fabric.ids.StincarpIds;
import com.morecritters.fabric.module.balloon_rat.BalloonRatEffects;
import com.morecritters.fabric.module.balloon_rat.BalloonRatEntities;
import com.morecritters.fabric.module.balloon_rat.BalloonRatEntity;
import com.morecritters.fabric.module.balloon_rat.BalloonRatItems;
import com.morecritters.fabric.module.balloon_rat.PinkMonsterEntity;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * The balloon rat: inflates near players and pops into poison, tamed into a medic or soldier,
 * fed cupcakes, drops a toxin bladder; and its three poisons and the pink monster.
 */
public class BalloonRatTests {
	private static final BlockPos MID = new BlockPos(3, 1, 3);

	@GameTest
	public void spiderEyesTameItIntoAMedic(GameTestHelper helper) {
		TestScenes.floor(helper);
		BalloonRatEntity rat = helper.spawnWithNoFreeWill(BalloonRatEntities.BALLOON_RAT, MID);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SPIDER_EYE, 64));
		int fed = 0;
		while (!rat.isTame() && fed < 64) {
			rat.mobInteract(player, InteractionHand.MAIN_HAND);
			fed++;
		}
		helper.assertTrue(rat.isTame(), "tamed after " + fed + " spider eyes");
		helper.assertTrue(rat.isOwnedBy(player), "owned by the feeder");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 64 - fed, "spider eyes left");
		helper.succeedWhen(() -> helper.assertValueEqual(rat.textureName(), "balloon_rat_medic", "texture of a freshly tamed rat"));
	}

	@GameTest
	public void theOwnerSitsAndStandsIt(GameTestHelper helper) {
		TestScenes.floor(helper);
		BalloonRatEntity rat = helper.spawnWithNoFreeWill(BalloonRatEntities.BALLOON_RAT, MID);
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		rat.tame(owner);
		rat.mobInteract(owner, InteractionHand.MAIN_HAND);
		helper.assertTrue(isSitting(rat), "sits after the owner's right-click");
		rat.mobInteract(owner, InteractionHand.MAIN_HAND);
		helper.assertFalse(isSitting(rat), "stands after a second right-click");

		Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
		rat.mobInteract(stranger, InteractionHand.MAIN_HAND);
		helper.assertFalse(isSitting(rat), "sat down for a stranger");
		helper.succeed();
	}

	@GameTest
	public void sneakClickSwapsMedicAndSoldier(GameTestHelper helper) {
		TestScenes.floor(helper);
		BalloonRatEntity rat = helper.spawnWithNoFreeWill(BalloonRatEntities.BALLOON_RAT, MID);
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		rat.tame(owner);
		owner.setShiftKeyDown(true);
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertValueEqual(rat.textureName(), "balloon_rat_medic", "texture once tamed"))
			.thenExecute(() -> {
				rat.mobInteract(owner, InteractionHand.MAIN_HAND);
				helper.assertValueEqual(rat.textureName(), "balloon_rat_soldier", "texture after a sneak-click on a medic");
				helper.assertFalse(isSitting(rat), "sat down on a sneak-click");
				rat.mobInteract(owner, InteractionHand.MAIN_HAND);
				helper.assertValueEqual(rat.textureName(), "balloon_rat_medic", "texture after a sneak-click on a soldier");
			})
			.thenSucceed();
	}

	@GameTest
	public void aCupcakeHealsThreeHealth(GameTestHelper helper) {
		TestScenes.floor(helper);
		BalloonRatEntity rat = helper.spawnWithNoFreeWill(BalloonRatEntities.BALLOON_RAT, MID);
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		rat.tame(owner);
		rat.setHealth(10.0F);
		owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BalloonRatItems.CUPCAKE, 2));
		rat.mobInteract(owner, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(rat.getHealth(), 13.0F, "health after a cupcake");
		helper.assertValueEqual(owner.getMainHandItem().getCount(), 1, "cupcakes left");
		helper.succeed();
	}

	@GameTest
	public void aPoisonedCupcakeArmsASoldierWithThatPoison(GameTestHelper helper) {
		TestScenes.floor(helper);
		BalloonRatEntity rat = helper.spawnWithNoFreeWill(BalloonRatEntities.BALLOON_RAT, MID);
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		rat.tame(owner);
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertValueEqual(rat.textureName(), "balloon_rat_medic", "texture once tamed"))
			.thenExecute(() -> {
				owner.setShiftKeyDown(true);
				rat.mobInteract(owner, InteractionHand.MAIN_HAND);
				owner.setShiftKeyDown(false);
				helper.assertValueEqual(rat.textureName(), "balloon_rat_soldier", "texture before feeding");

				owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BalloonRatItems.CUPCAKE_STAGNATION, 2));
				rat.mobInteract(owner, InteractionHand.MAIN_HAND);
				MobEffectInstance stagnation = rat.getEffect(BalloonRatEffects.STAGNATION);
				helper.assertTrue(stagnation != null && stagnation.getDuration() == 3600, "a soldier fed a stagnation cupcake carries stagnation for 3600 ticks");
				helper.assertValueEqual(owner.getMainHandItem().getCount(), 1, "poisoned cupcakes left");

				owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BalloonRatItems.CUPCAKE_MUSCLE_ACHE));
				rat.mobInteract(owner, InteractionHand.MAIN_HAND);
				helper.assertTrue(rat.hasEffect(BalloonRatEffects.MUSCLE_ACHE), "the new cupcake's poison is carried");
				helper.assertFalse(rat.hasEffect(BalloonRatEffects.STAGNATION), "the old poison is still carried");
			})
			.thenSucceed();
	}

	@GameTest
	public void aMedicDoesNotTakePoisonedCupcakes(GameTestHelper helper) {
		TestScenes.floor(helper);
		BalloonRatEntity rat = helper.spawnWithNoFreeWill(BalloonRatEntities.BALLOON_RAT, MID);
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		rat.tame(owner);
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertValueEqual(rat.textureName(), "balloon_rat_medic", "texture once tamed"))
			.thenExecute(() -> {
				owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BalloonRatItems.CUPCAKE_STAGNATION, 2));
				rat.mobInteract(owner, InteractionHand.MAIN_HAND);
				helper.assertFalse(rat.hasEffect(BalloonRatEffects.STAGNATION), "a medic took on stagnation");
				helper.assertValueEqual(owner.getMainHandItem().getCount(), 2, "poisoned cupcakes left");
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 80)
	public void aMedicRegeneratesItselfAndItsOwnersPets(GameTestHelper helper) {
		TestScenes.floor(helper);
		BalloonRatEntity medic = helper.spawnWithNoFreeWill(BalloonRatEntities.BALLOON_RAT, MID);
		BalloonRatEntity pet = helper.spawnWithNoFreeWill(BalloonRatEntities.BALLOON_RAT, new BlockPos(5, 1, 3));
		Pig stranger = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 1, 5));
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		medic.tame(owner);
		pet.tame(owner);
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertValueEqual(medic.textureName(), "balloon_rat_medic", "texture once tamed"))
			.thenExecute(() -> setInt(medic, "healTimer", 0))
			.thenExecuteAfter(25, () -> {
				for (BalloonRatEntity healed : List.of(medic, pet)) {
					MobEffectInstance regeneration = healed.getEffect(MobEffects.REGENERATION);
					helper.assertTrue(regeneration != null && regeneration.getAmplifier() == 2, "Regeneration III from the medic, got " + regeneration);
				}
				helper.assertFalse(stranger.hasEffect(MobEffects.REGENERATION), "the medic healed a stranger's pig");
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 80)
	public void aSoldierPoisonsItsTargetWithWhatItCarries(GameTestHelper helper) {
		TestScenes.floor(helper);
		BalloonRatEntity soldier = helper.spawnWithNoFreeWill(BalloonRatEntities.BALLOON_RAT, MID);
		BalloonRatEntity pet = helper.spawnWithNoFreeWill(BalloonRatEntities.BALLOON_RAT, new BlockPos(5, 1, 3));
		Pig target = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 1, 5));
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		soldier.tame(owner);
		pet.tame(owner);
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertValueEqual(soldier.textureName(), "balloon_rat_medic", "texture once tamed"))
			.thenExecute(() -> {
				owner.setShiftKeyDown(true);
				soldier.mobInteract(owner, InteractionHand.MAIN_HAND);
				owner.setShiftKeyDown(false);
				owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BalloonRatItems.CUPCAKE_STAGNATION));
				soldier.mobInteract(owner, InteractionHand.MAIN_HAND);
				// Feeding by the owner also toggles sitting; stand it up again with an empty hand.
				owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				if (isSitting(soldier)) soldier.mobInteract(owner, InteractionHand.MAIN_HAND);
				helper.assertFalse(isSitting(soldier), "the soldier is standing");
				soldier.setTarget(target);
			})
			// The pulse only fires on the ground, and the clicks above leave it airborne for a tick.
			.thenWaitUntil(() -> helper.assertTrue(soldier.onGround(), "the soldier is on the ground"))
			.thenExecute(() -> setInt(soldier, "attackTimer", 0))
			.thenExecuteAfter(25, () -> {
				MobEffectInstance stagnation = target.getEffect(BalloonRatEffects.STAGNATION);
				helper.assertTrue(stagnation != null && stagnation.getAmplifier() == 2 && stagnation.getDuration() <= 100,
					"the target gets the carried stagnation at III for 100 ticks, got " + stagnation);
				helper.assertFalse(pet.hasEffect(BalloonRatEffects.STAGNATION), "the soldier poisoned its owner's other pet");
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 60)
	public void aWildRatInflatesNearAPlayerAndPopsIntoPoison(GameTestHelper helper) {
		TestScenes.floor(helper);
		BalloonRatEntity rat = helper.spawnWithNoFreeWill(BalloonRatEntities.BALLOON_RAT, MID);
		ServerPlayer player = playerInLevel(helper, GameType.SURVIVAL, new Vec3(1.5, 1.0, 1.5));
		Pig bystander = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(6, 1, 6));
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertValueEqual(rat.textureName(), "balloon_rat_inflated", "texture of a wild rat near a survival player"))
			.thenExecute(() -> {
				bystander.snapTo(rat.position());
				rat.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 1.0F);
				removePlayer(helper, player);
				MobEffectInstance poison = bystander.getEffect(MobEffects.POISON);
				helper.assertTrue(poison != null && poison.getAmplifier() == 2 && poison.getDuration() == 100,
					"a mob next to a popped rat gets Poison III for 100 ticks, got " + poison);
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 40)
	public void aCreativePlayerDoesNotInflateIt(GameTestHelper helper) {
		TestScenes.floor(helper);
		BalloonRatEntity rat = helper.spawnWithNoFreeWill(BalloonRatEntities.BALLOON_RAT, MID);
		ServerPlayer player = playerInLevel(helper, GameType.CREATIVE, new Vec3(1.5, 1.0, 1.5));
		helper.runAfterDelay(25, () -> {
			removePlayer(helper, player);
			helper.assertValueEqual(rat.textureName(), "balloon_rat", "texture of a wild rat near a creative player");
			helper.succeed();
		});
	}

	@GameTest
	public void itShrugsOffPoison(GameTestHelper helper) {
		TestScenes.floor(helper);
		BalloonRatEntity rat = helper.spawnWithNoFreeWill(BalloonRatEntities.BALLOON_RAT, MID);
		rat.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
		helper.succeedWhen(() -> helper.assertFalse(rat.hasEffect(MobEffects.POISON), "the rat is still poisoned"));
	}

	@GameTest
	public void aDeadRatDropsOneToxinBladder(GameTestHelper helper) {
		TestScenes.floor(helper);
		Set<Item> bladders = Set.of(item(BalloonRatIds.Items.TOXIN_BLADDER_STAGNATION), item(BalloonRatIds.Items.TOXIN_BLADDER_MUSCLE_ACHE),
			item(BalloonRatIds.Items.TOXIN_BLADDER_HALLUCINAZIUM), item(SnowflakeSpiderIds.Items.TOXIN_BLADDER_BRITTLENESS),
			item(StincarpIds.Items.TOXIN_BLADDER_ASPHYXIATION));
		helper.assertFalse(bladders.contains(Items.AIR), "every toxin bladder is registered");
		BalloonRatEntity rat = helper.spawnWithNoFreeWill(BalloonRatEntities.BALLOON_RAT, MID);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		rat.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 1000.0F);
		helper.succeedWhen(() -> {
			List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds());
			helper.assertValueEqual(drops.size(), 1, "item stacks dropped");
			helper.assertValueEqual(drops.get(0).getItem().getCount(), 1, "items in the drop");
			helper.assertTrue(bladders.contains(drops.get(0).getItem().getItem()), "the drop is a toxin bladder, was " + drops.get(0).getItem());
		});
	}

	@GameTest
	public void stagnationLetsHealthOnlyGoDown(GameTestHelper helper) {
		TestScenes.floor(helper);
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, MID);
		pig.addEffect(new MobEffectInstance(BalloonRatEffects.STAGNATION, 200));
		ServerLevel level = helper.getLevel();
		helper.startSequence()
			.thenExecuteAfter(2, () -> pig.hurtServer(level, level.damageSources().generic(), 4.0F))
			.thenExecuteAfter(2, () -> {
				helper.assertValueEqual(pig.getHealth(), 6.0F, "health after a hit");
				pig.heal(3.0F);
			})
			.thenExecuteAfter(2, () -> helper.assertValueEqual(pig.getHealth(), 6.0F, "health after healing under stagnation"))
			.thenSucceed();
	}

	@GameTest
	public void muscleAcheHurtsAnAttacker(GameTestHelper helper) {
		TestScenes.floor(helper);
		Cow cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(2, 1, 3));
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(4, 1, 3));
		cow.addEffect(new MobEffectInstance(BalloonRatEffects.MUSCLE_ACHE, 200));
		pig.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(cow), 1.0F);
		helper.assertValueInBetween(2.0F, cow.getMaxHealth() - cow.getHealth(), 3.0F, "damage the aching attacker took");
		helper.succeed();
	}

	@GameTest
	public void balloonRatsDoNotFeelMuscleAche(GameTestHelper helper) {
		TestScenes.floor(helper);
		BalloonRatEntity rat = helper.spawnWithNoFreeWill(BalloonRatEntities.BALLOON_RAT, new BlockPos(2, 1, 3));
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(4, 1, 3));
		rat.addEffect(new MobEffectInstance(BalloonRatEffects.MUSCLE_ACHE, 200));
		pig.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(rat), 1.0F);
		helper.assertValueEqual(rat.getHealth(), rat.getMaxHealth(), "rat health after attacking with muscle ache");
		helper.succeed();
	}

	@GameTest(maxTicks = 120)
	public void hallucinaziumJumpscaresAMobForTenDamage(GameTestHelper helper) {
		TestScenes.floor(helper);
		IronGolem golem = helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, MID);
		golem.addEffect(new MobEffectInstance(BalloonRatEffects.HALLUCINAZIUM, 600));
		helper.runAfterDelay(2, () -> helper.assertTrue(golem.hasEffect(MobEffects.NAUSEA), "hallucinazium brings nausea"));
		helper.runAfterDelay(70, () -> helper.assertValueEqual(golem.getHealth(), golem.getMaxHealth(), "health before the first jumpscare"));
		helper.runAfterDelay(90, () -> {
			helper.assertValueEqual(golem.getHealth(), golem.getMaxHealth() - 10.0F, "health after the first jumpscare");
			helper.succeed();
		});
	}

	@GameTest
	public void aToxicCupcakeGivesTenSecondsOfItsPoison(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack cupcake = new ItemStack(BalloonRatItems.CUPCAKE_HALLUCINAZIUM, 2);
		player.setItemInHand(InteractionHand.MAIN_HAND, cupcake);
		cupcake.finishUsingItem(helper.getLevel(), player);
		MobEffectInstance effect = player.getEffect(BalloonRatEffects.HALLUCINAZIUM);
		helper.assertTrue(effect != null && effect.getDuration() == 200, "hallucinazium for 200 ticks after eating, got " + effect);
		helper.succeed();
	}

	@GameTest(maxTicks = 60)
	public void thePinkMonsterHauntsAHallucinatingPlayerForTwoSeconds(GameTestHelper helper) {
		TestScenes.floor(helper);
		ServerPlayer player = playerInLevel(helper, GameType.SURVIVAL, new Vec3(1.5, 1.0, 1.5));
		player.addEffect(new MobEffectInstance(BalloonRatEffects.HALLUCINAZIUM, 600));
		PinkMonsterEntity monster = helper.spawn(BalloonRatEntities.PINK_MONSTER, new BlockPos(4, 3, 4), EntitySpawnReason.MOB_SUMMONED);
		helper.runAfterDelay(20, () -> helper.assertFalse(monster.isRemoved(), "the monster vanished while the player still hallucinates"));
		helper.runAfterDelay(45, () -> {
			removePlayer(helper, player);
			helper.assertTrue(monster.isRemoved(), "the monster is gone after its two seconds");
			helper.succeed();
		});
	}

	@GameTest
	public void thePinkMonsterVanishesWithoutAHallucinatingPlayer(GameTestHelper helper) {
		TestScenes.floor(helper);
		PinkMonsterEntity monster = helper.spawn(BalloonRatEntities.PINK_MONSTER, new BlockPos(4, 3, 4), EntitySpawnReason.MOB_SUMMONED);
		helper.succeedWhen(() -> helper.assertTrue(monster.isRemoved(), "the monster is gone"));
	}

	/** Fast-forwards one of the rat's private tick counters. */
	private static void setInt(Object target, String field, int value) {
		try {
			java.lang.reflect.Field f = target.getClass().getDeclaredField(field);
			f.setAccessible(true);
			f.setInt(target, value);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static boolean isSitting(BalloonRatEntity rat) {
		try {
			Method sitting = BalloonRatEntity.class.getDeclaredMethod("sitting");
			sitting.setAccessible(true);
			return (boolean) sitting.invoke(rat);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
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

	private static Item item(Identifier id) {
		return BuiltInRegistries.ITEM.getValue(id);
	}
}
