package com.morecritters.fabric.test;

import com.morecritters.fabric.module.warptrap.WarptrapEntity;
import com.morecritters.fabric.module.warptrap.WarptrapModule;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** The warptrap: digs into its nylium and hides, bites victims up onto its back, preys on endermen; its digger effect. */
public class WarptrapTests {
	@GameTest(maxTicks = 120)
	public void onWarpedNyliumItDigsInAndHides(GameTestHelper helper) {
		floor(helper, Blocks.WARPED_NYLIUM);
		WarptrapEntity warptrap = helper.spawn(WarptrapModule.WARPTRAP, new BlockPos(3, 1, 3));
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(warptrap.hasEffect(WarptrapModule.DIGGER), "the digger effect while digging in"))
			.thenExecute(() -> {
				helper.assertFalse(warptrap.isBuried(), "hidden as soon as it starts digging");
				MobEffectInstance slowness = warptrap.getEffect(MobEffects.SLOWNESS);
				helper.assertTrue(slowness != null && slowness.getAmplifier() == 30, "rooted (Slowness XXXI) while digging");
			})
			.thenWaitUntil(() -> {
				helper.assertTrue(warptrap.isBuried(), "hidden after digging in");
				helper.assertValueEqual(warptrap.textureName(), "warptrap_dig", "texture while hidden");
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 120)
	public void itDoesNotDigIntoOtherGround(GameTestHelper helper) {
		floor(helper, Blocks.CRIMSON_NYLIUM);
		WarptrapEntity warptrap = helper.spawn(WarptrapModule.WARPTRAP, new BlockPos(3, 1, 3));
		helper.startSequence()
			.thenExecuteFor(100, () -> {
				helper.assertFalse(warptrap.isBuried(), "a warped warptrap hidden on crimson nylium");
				helper.assertFalse(warptrap.hasEffect(WarptrapModule.DIGGER), "a warped warptrap digging into crimson nylium");
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 120)
	public void leavingItsNyliumBringsItOut(GameTestHelper helper) {
		floor(helper, Blocks.WARPED_NYLIUM);
		helper.setBlock(7, 0, 7, Blocks.STONE);
		WarptrapEntity warptrap = helper.spawn(WarptrapModule.WARPTRAP, new BlockPos(3, 1, 3));
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(warptrap.isBuried(), "hidden"))
			.thenExecute(() -> helper.moveTo(warptrap, new BlockPos(7, 1, 7)))
			.thenWaitUntil(() -> helper.assertFalse(warptrap.isBuried(), "hidden off its nylium"))
			.thenSucceed();
	}

	@GameTest(maxTicks = 120)
	public void itsBitePullsTheVictimOntoItsBackAndBringsItOut(GameTestHelper helper) {
		floor(helper, Blocks.WARPED_NYLIUM);
		WarptrapEntity warptrap = helper.spawn(WarptrapModule.WARPTRAP, new BlockPos(2, 1, 3));
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(5, 1, 3));
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(warptrap.isBuried(), "hidden"))
			.thenExecute(() -> {
				warptrap.doHurtTarget(helper.getLevel(), pig);
				helper.assertFalse(warptrap.isBuried(), "hidden after biting");
				helper.assertTrue(pig.position().distanceToSqr(warptrap.position().add(0.0, 1.0, 0.0)) < 1.0E-4,
					"the pig is pulled one block above the warptrap");
				helper.assertValueEqual(pig.getHealth(), pig.getMaxHealth() - 3.0F, "pig health after a bite");
			})
			.thenSucceed();
	}

	@GameTest
	public void endermenTakeTwentyExtraFromItsBite(GameTestHelper helper) {
		floor(helper, Blocks.STONE);
		WarptrapEntity warptrap = helper.spawn(WarptrapModule.WARPTRAP, new BlockPos(2, 1, 3));
		Enderman enderman = helper.spawnWithNoFreeWill(EntityTypes.ENDERMAN, new BlockPos(5, 1, 3));
		warptrap.doHurtTarget(helper.getLevel(), enderman);
		// 20 generic damage (invulnerability then swallows the 3 of the bite itself, as in the original).
		helper.assertValueEqual(enderman.getHealth(), enderman.getMaxHealth() - 20.0F, "enderman health after a bite");
		helper.assertTrue(enderman.position().distanceToSqr(warptrap.position().add(0.0, 1.0, 0.0)) < 1.0E-4,
			"the enderman is pulled back onto the warptrap");
		helper.succeed();
	}

	@GameTest
	public void anEndermanItKillsDropsAnEnderPearl(GameTestHelper helper) {
		floor(helper, Blocks.STONE);
		WarptrapEntity warptrap = helper.spawn(WarptrapModule.WARPTRAP, new BlockPos(2, 1, 3));
		Enderman enderman = helper.spawnWithNoFreeWill(EntityTypes.ENDERMAN, new BlockPos(5, 1, 3));
		enderman.setHealth(1.0F);
		ServerLevel level = helper.getLevel();
		enderman.hurtServer(level, level.damageSources().mobAttack(warptrap), 5.0F);
		helper.succeedWhen(() -> helper.assertItemEntityPresent(Items.ENDER_PEARL));
	}

	@GameTest(maxTicks = 100)
	public void itHuntsEndermen(GameTestHelper helper) {
		floor(helper, Blocks.STONE);
		WarptrapEntity warptrap = helper.spawn(WarptrapModule.WARPTRAP, new BlockPos(2, 1, 3));
		Enderman enderman = helper.spawnWithNoFreeWill(EntityTypes.ENDERMAN, new BlockPos(6, 1, 3));
		helper.succeedWhen(() -> helper.assertTrue(warptrap.getTarget() == enderman, "the warptrap targets the enderman"));
	}

	@GameTest
	public void fireAndExplosionsDoNotHurtIt(GameTestHelper helper) {
		floor(helper, Blocks.STONE);
		ServerLevel level = helper.getLevel();
		WarptrapEntity warptrap = helper.spawn(WarptrapModule.WARPTRAP, new BlockPos(3, 1, 3));
		warptrap.hurtServer(level, level.damageSources().inFire(), 5.0F);
		warptrap.hurtServer(level, level.damageSources().explosion(null, null), 5.0F);
		helper.assertValueEqual(warptrap.getHealth(), 30.0F, "health after fire and an explosion");
		helper.assertTrue(warptrap.fireImmune(), "fire immune");
		warptrap.hurtServer(level, level.damageSources().generic(), 5.0F);
		helper.assertValueEqual(warptrap.getHealth(), 25.0F, "health after generic damage");
		helper.succeed();
	}

	@GameTest
	public void theDiggerEffectRootsItsBearer(GameTestHelper helper) {
		floor(helper, Blocks.STONE);
		Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(3, 1, 3));
		pig.addEffect(new MobEffectInstance(WarptrapModule.DIGGER, 40));
		MobEffectInstance slowness = pig.getEffect(MobEffects.SLOWNESS);
		helper.assertTrue(slowness != null, "slowness from the digger effect");
		helper.assertValueEqual(slowness.getAmplifier(), 30, "slowness amplifier");
		helper.assertValueEqual(slowness.getDuration(), 40, "slowness duration");
		helper.succeed();
	}

	private static void floor(GameTestHelper helper, Block block) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(x, 0, z, block);
			}
		}
	}
}
