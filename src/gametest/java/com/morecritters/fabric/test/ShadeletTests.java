package com.morecritters.fabric.test;

import com.morecritters.fabric.module.shadelet.ChatteringTeethEntity;
import com.morecritters.fabric.module.shadelet.ShadeletEntity;
import com.morecritters.fabric.module.shadelet.ShadeletModule;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** The shadelet: jumpscares, sugar rush from sweets, its immunities; Spooked; chattering teeth and the tooth syringe. */
public class ShadeletTests {
	@GameTest(maxTicks = 200)
	public void itSneaksUpOnASurvivalPlayerAndSpooksThem(GameTestHelper helper) {
		TestScenes.floor(helper);
		// Spawned without finalizeSpawn, its scare timer starts at 0, so it picks a victim at once.
		helper.spawn(ShadeletModule.SHADELET, new BlockPos(1, 1, 3));
		Player player = playerInLevel(helper, GameType.SURVIVAL, new BlockPos(6, 1, 3));
		helper.startSequence()
			.thenWaitUntil(() -> {
				helper.assertTrue(player.hasEffect(ShadeletModule.SPOOKED), "the player is spooked");
				helper.assertTrue(player.hasEffect(MobEffects.WEAKNESS), "the spooked player is weakened");
			})
			.thenExecute(player::discard)
			.thenSucceed();
	}

	@GameTest(maxTicks = 200)
	public void itLeavesCreativePlayersAlone(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.spawn(ShadeletModule.SHADELET, new BlockPos(1, 1, 3));
		Player player = playerInLevel(helper, GameType.CREATIVE, new BlockPos(4, 1, 3));
		helper.startSequence()
			.thenExecuteFor(150, () -> helper.assertFalse(player.hasEffect(ShadeletModule.SPOOKED), "a creative player is spooked"))
			.thenExecute(player::discard)
			.thenSucceed();
	}

	@GameTest(maxTicks = 300)
	public void onASugarRushItSpooksMonstersInstead(GameTestHelper helper) {
		TestScenes.floor(helper);
		ShadeletEntity shadelet = helper.spawn(ShadeletModule.SHADELET, new BlockPos(1, 1, 3));
		Husk husk = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(6, 1, 3));
		Player feeder = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(() -> feed(feeder, shadelet, new ItemStack(Items.COOKIE)))
			.thenWaitUntil(() -> {
				helper.assertTrue(husk.hasEffect(ShadeletModule.SPOOKED), "the husk is spooked");
				helper.assertTrue(husk.hasEffect(MobEffects.WEAKNESS), "the spooked husk is weakened");
			})
			.thenSucceed();
	}

	/** ShadeletRightClickProcedure: sweets are eaten only once the last rush is over (sugar rush below 0). */
	@GameTest
	public void itEatsOneSweetPerSugarRush(GameTestHelper helper) {
		TestScenes.floor(helper);
		ShadeletEntity shadelet = helper.spawn(ShadeletModule.SHADELET, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(() -> {
				feed(player, shadelet, new ItemStack(Items.APPLE, 3));
				helper.assertValueEqual(player.getMainHandItem().getCount(), 3, "apples left (not a sweet)");
				feed(player, shadelet, new ItemStack(Items.COOKIE, 3));
				helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "cookies left after the first");
				feed(player, shadelet, player.getMainHandItem());
				helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "cookies left while it is still on a sugar rush");
			})
			.thenSucceed();
	}

	@GameTest
	public void bottledAndBowledSweetsHandBackTheContainer(GameTestHelper helper) {
		TestScenes.floor(helper);
		ShadeletEntity first = helper.spawn(ShadeletModule.SHADELET, new BlockPos(2, 1, 3));
		ShadeletEntity second = helper.spawn(ShadeletModule.SHADELET, new BlockPos(5, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(() -> {
				feed(player, first, new ItemStack(Items.HONEY_BOTTLE));
				helper.assertValueEqual(player.getInventory().countItem(Items.GLASS_BOTTLE), 1, "glass bottles handed back");
				feed(player, second, new ItemStack(ShadeletModule.TOOTH_MELTER));
				helper.assertValueEqual(player.getInventory().countItem(Items.BOWL), 1, "bowls handed back");
				helper.assertValueEqual(player.getInventory().countItem(ShadeletModule.TOOTH_MELTER), 0, "tooth melters left");
			})
			.thenSucceed();
	}

	@GameTest
	public void handsArrowsFallsFireAndExplosionsDoNotHurtIt(GameTestHelper helper) {
		TestScenes.floor(helper);
		ServerLevel level = helper.getLevel();
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		assertUnhurt(helper, level.damageSources().playerAttack(player), "a player's hand");
		assertUnhurt(helper, level.damageSources().fall(), "a fall");
		assertUnhurt(helper, level.damageSources().inFire(), "fire");
		assertUnhurt(helper, level.damageSources().explosion(null, null), "an explosion");
		assertUnhurt(helper, level.damageSources().cactus(), "a cactus");
		ShadeletEntity shadelet = helper.spawn(ShadeletModule.SHADELET, new BlockPos(3, 1, 3));
		Husk husk = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(5, 1, 5));
		shadelet.hurtServer(level, level.damageSources().mobAttack(husk), 3.0F);
		helper.assertValueEqual(shadelet.getHealth(), 7.0F, "health after a husk's hit");
		helper.succeed();
	}

	@GameTest
	public void aShadeletNamedLolipopWearsTheLolipopTexture(GameTestHelper helper) {
		TestScenes.floor(helper);
		ShadeletEntity shadelet = helper.spawn(ShadeletModule.SHADELET, new BlockPos(3, 1, 3));
		helper.assertValueEqual(shadelet.textureName(), "shadelet", "texture unnamed");
		shadelet.setCustomName(Component.literal("Lolipop"));
		helper.assertValueEqual(shadelet.textureName(), "shadelet_lolipop", "texture named Lolipop");
		helper.succeed();
	}

	@GameTest
	public void spookedFreezesAndWeakens(GameTestHelper helper) {
		TestScenes.floor(helper);
		Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(3, 1, 3));
		pig.addEffect(new MobEffectInstance(ShadeletModule.SPOOKED, 100));
		MobEffectInstance slowness = pig.getEffect(MobEffects.SLOWNESS);
		MobEffectInstance weakness = pig.getEffect(MobEffects.WEAKNESS);
		helper.assertTrue(slowness != null && weakness != null, "slowness and weakness from Spooked");
		helper.assertValueEqual(slowness.getAmplifier(), 49, "slowness amplifier (a freeze)");
		helper.assertValueEqual(slowness.getDuration(), 10, "slowness duration");
		helper.assertValueEqual(weakness.getDuration(), 100, "weakness duration");
		helper.succeed();
	}

	@GameTest
	public void chatteringTeethAreSetDownOnTheClickedFace(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ShadeletModule.CHATTERING_TEETH_ITEM, 2));
		TestScenes.useItemOn(helper, player, new BlockPos(3, 0, 3), Direction.UP);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "teeth left in hand");
		helper.assertEntityPresent(ShadeletModule.CHATTERING_TEETH, new BlockPos(3, 1, 3));
		helper.succeed();
	}

	@GameTest
	public void chatteringTeethBiteForFive(GameTestHelper helper) {
		TestScenes.floor(helper);
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 1, 3));
		helper.spawn(ShadeletModule.CHATTERING_TEETH, new Vec3(3.5, 1.0, 4.0));
		helper.succeedWhen(() -> helper.assertValueEqual(pig.getHealth(), pig.getMaxHealth() - 5.0F, "pig health after one bite"));
	}

	@GameTest
	public void chatteringTeethWindDownIntoTheItemAtAWall(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(4, 1, 3, Blocks.STONE);
		ChatteringTeethEntity teeth = helper.spawn(ShadeletModule.CHATTERING_TEETH, new Vec3(3.6, 1.0, 3.5));
		helper.succeedWhen(() -> {
			helper.assertTrue(teeth.isRemoved(), "the teeth are packed up");
			helper.assertItemEntityPresent(ShadeletModule.CHATTERING_TEETH_ITEM, new BlockPos(3, 1, 3), 2.0);
		});
	}

	/** ToothSyringeRightclickedProcedure: 3 in 10 Poison II, otherwise Regeneration III, both 60 ticks; used up. */
	@GameTest
	public void theToothSyringeHealsOrPoisons(GameTestHelper helper) {
		int poisoned = 0, healed = 0;
		for (int i = 0; i < 40; i++) {
			Player player = helper.makeMockPlayer(GameType.SURVIVAL);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ShadeletModule.TOOTH_SYRINGE, 2));
			ShadeletModule.TOOTH_SYRINGE.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
			helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "syringes left");
			MobEffectInstance poison = player.getEffect(MobEffects.POISON);
			MobEffectInstance regeneration = player.getEffect(MobEffects.REGENERATION);
			helper.assertTrue((poison == null) != (regeneration == null), "exactly one of poison and regeneration");
			MobEffectInstance effect = poison != null ? poison : regeneration;
			helper.assertValueEqual(effect.getDuration(), 60, "effect duration");
			helper.assertValueEqual(effect.getAmplifier(), poison != null ? 1 : 2, "effect amplifier");
			if (poison != null) poisoned++; else healed++;
		}
		helper.assertTrue(poisoned > 0 && healed > 0, "both outcomes over 40 uses: " + poisoned + " poisoned, " + healed + " healed");
		helper.succeed();
	}

	// --- helpers ------------------------------------------------------------------------

	private static void assertUnhurt(GameTestHelper helper, DamageSource source, String what) {
		ShadeletEntity shadelet = helper.spawn(ShadeletModule.SHADELET, new BlockPos(3, 1, 3));
		shadelet.hurtServer(helper.getLevel(), source, 4.0F);
		helper.assertValueEqual(shadelet.getHealth(), 10.0F, "health after " + what);
		shadelet.discard();
	}

	private static void feed(Player player, ShadeletEntity shadelet, ItemStack stack) {
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		player.interactOn(shadelet, InteractionHand.MAIN_HAND, shadelet.position());
	}

	/** A player standing in the world (found by entity searches), unlike a bare mock player. Discard it when done. */
	private static Player playerInLevel(GameTestHelper helper, GameType mode, BlockPos pos) {
		Player player = helper.makeMockPlayer(mode);
		Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		helper.getLevel().addFreshEntity(player);
		return player;
	}
}
