package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.CustodianIds;
import com.morecritters.fabric.ids.NervoidIds;
import com.morecritters.fabric.module.nervoid.NervoidEntity;
import com.morecritters.fabric.module.nervoid.NervoidModule;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;

/** The nervoid: wet and dry nervoids, spinal fluid, possession and release, Endfected bursts; brains and Brain Scented. */
public class NervoidTests {
	@GameTest
	public void aGlassBottleDrawsSpinalFluidFromAWetNervoid(GameTestHelper helper) {
		TestScenes.floor(helper);
		NervoidEntity nervoid = helper.spawn(NervoidModule.NERVOID, new BlockPos(3, 1, 3), EntitySpawnReason.MOB_SUMMONED);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));
		player.interactOn(nervoid, InteractionHand.MAIN_HAND, nervoid.position());
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "glass bottles left");
		helper.assertValueEqual(player.getInventory().countItem(item(NervoidIds.Items.SPINAL_FLUID_BOTTLE)), 1, "spinal fluid bottles");
		helper.assertValueEqual(nervoid.textureName(), "nervoid", "texture after bottling");

		// Dry now: a second bottle gets nothing.
		player.interactOn(nervoid, InteractionHand.MAIN_HAND, nervoid.position());
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "glass bottles left after trying a dry nervoid");
		helper.assertValueEqual(player.getInventory().countItem(item(NervoidIds.Items.SPINAL_FLUID_BOTTLE)), 1, "spinal fluid bottles after a dry nervoid");
		helper.succeed();
	}

	@GameTest
	public void aNervoidIsDryUnlessSummonedOutOfAHost(GameTestHelper helper) {
		TestScenes.floor(helper);
		NervoidEntity plain = helper.spawn(NervoidModule.NERVOID, new BlockPos(2, 1, 3), EntitySpawnReason.SPAWN_ITEM_USE);
		NervoidEntity summoned = helper.spawn(NervoidModule.NERVOID, new BlockPos(5, 1, 3), EntitySpawnReason.MOB_SUMMONED);
		helper.assertValueEqual(plain.textureName(), "nervoid", "texture of an egg-spawned nervoid");
		helper.assertValueEqual(summoned.textureName(), "nervoid_wet", "texture of a nervoid summoned out of a host");
		helper.succeed();
	}

	@GameTest
	public void aPossessedCreatureLetsOutOneWetNervoidWhenItDies(GameTestHelper helper) {
		TestScenes.floor(helper);
		Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(3, 1, 3));
		pig.addEffect(new MobEffectInstance(underControl(), 600));
		helper.kill(pig);
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertEntitiesPresent(NervoidModule.NERVOID, 1))
			.thenIdle(5)
			.thenExecute(() -> {
				List<NervoidEntity> nervoids = helper.getEntities(NervoidModule.NERVOID);
				helper.assertValueEqual(nervoids.size(), 1, "nervoids let out (not a second one from the effect ending)");
				helper.assertValueEqual(nervoids.get(0).textureName(), "nervoid_wet", "texture of the released nervoid");
			})
			.thenSucceed();
	}

	@GameTest
	public void anEndfectedCreatureBurstsIntoADryNervoid(GameTestHelper helper) {
		TestScenes.floor(helper);
		Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(3, 1, 3));
		pig.addEffect(new MobEffectInstance(NervoidModule.ENDFECTED, Integer.MAX_VALUE));
		helper.kill(pig);
		helper.succeedWhen(() -> {
			helper.assertEntitiesPresent(NervoidModule.NERVOID, 1);
			helper.assertValueEqual(helper.getEntities(NervoidModule.NERVOID).get(0).textureName(), "nervoid", "texture of the burst nervoid");
		});
	}

	/** NervoidAnimResetProcedure: a nervoid's hit rolls 1 in 3 to give Under Control (60 ticks) and vanish. */
	@GameTest
	public void aBiteSometimesPossessesTheVictim(GameTestHelper helper) {
		TestScenes.floor(helper);
		int bites = 40;
		List<NervoidEntity> nervoids = new ArrayList<>();
		List<Pig> victims = new ArrayList<>();
		for (int i = 0; i < bites; i++) {
			BlockPos pos = new BlockPos(i % 8, 1, (i / 8) % 8);
			Pig pig = helper.spawn(EntityTypes.PIG, pos);
			NervoidEntity nervoid = helper.spawn(NervoidModule.NERVOID, pos);
			helper.assertTrue(nervoid.doHurtTarget(helper.getLevel(), pig), "the bite lands");
			nervoids.add(nervoid);
			victims.add(pig);
		}
		int possessed = 0;
		for (int i = 0; i < bites; i++) {
			MobEffectInstance control = victims.get(i).getEffect(underControl());
			boolean gone = nervoids.get(i).isRemoved();
			helper.assertValueEqual(gone, control != null, "nervoid " + i + " vanished exactly when its victim is under control");
			if (control != null) {
				possessed++;
				helper.assertValueEqual(control.getDuration(), 60, "Under Control duration");
			}
		}
		// 1 in 3 over 40 bites: always some, never all (each bound fails with odds below 1e-6).
		helper.assertTrue(possessed > 0 && possessed < bites, "possessions out of " + bites + " bites: " + possessed);
		helper.succeed();
	}

	@GameTest
	public void takesNoFallDamage(GameTestHelper helper) {
		TestScenes.floor(helper);
		NervoidEntity nervoid = helper.spawn(NervoidModule.NERVOID, new BlockPos(3, 1, 3));
		helper.assertFalse(nervoid.causeFallDamage(20.0, 1.0F, helper.getLevel().damageSources().fall()), "a long fall hurts");
		nervoid.hurtServer(helper.getLevel(), helper.getLevel().damageSources().fall(), 8.0F);
		helper.assertValueEqual(nervoid.getHealth(), 30.0F, "health after fall damage");
		helper.succeed();
	}

	@GameTest
	public void iceFreezesABrainAndATorchThawsIt(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos pos = new BlockPos(3, 1, 3);
		helper.setBlock(pos, block(NervoidIds.Blocks.DECOMPOSING_NERVOID_BRAIN));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ICE, 2));
		helper.useBlock(pos, player);
		helper.assertBlockPresent(block(NervoidIds.Blocks.ICED_DECOMPOSING_NERVOID_BRAIN), pos);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "ice left");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TORCH, 2));
		helper.useBlock(pos, player);
		helper.assertBlockPresent(block(NervoidIds.Blocks.DECOMPOSING_NERVOID_BRAIN), pos);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "torches left");

		// In creative the ice is kept.
		Player creative = helper.makeMockPlayer(GameType.CREATIVE);
		GameType.CREATIVE.updatePlayerAbilities(creative.getAbilities());
		creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ICE, 2));
		helper.useBlock(pos, creative);
		helper.assertBlockPresent(block(NervoidIds.Blocks.ICED_DECOMPOSING_NERVOID_BRAIN), pos);
		helper.assertValueEqual(creative.getMainHandItem().getCount(), 2, "ice left in creative");
		helper.succeed();
	}

	/**
	 * Each check (every 5 ticks) an un-iced brain decays one time in 200; iced ones never do. The checks are run
	 * directly here, and with decomposing brains (lure range 15, not a fresh brain's 30), so that a brain sitting
	 * for the hundreds of ticks decay takes does not pull undead out of neighbouring tests.
	 */
	@GameTest
	public void brainsRotUnlessIced(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos plain = new BlockPos(2, 1, 3), iced = new BlockPos(5, 1, 3);
		Block rotten = block(NervoidIds.Blocks.ROTTEN_NERVOID_BRAIN);
		helper.setBlock(plain, block(NervoidIds.Blocks.DECOMPOSING_NERVOID_BRAIN));
		helper.setBlock(iced, block(NervoidIds.Blocks.ICED_DECOMPOSING_NERVOID_BRAIN));
		// Up to 5000 checks: never rotting has odds near 1e-11.
		for (int i = 0; i < 5000 && !helper.getBlockState(plain).is(rotten); i++) {
			helper.tickBlock(plain);
		}
		helper.assertBlockPresent(rotten, plain);
		// 1000 checks would rot an un-iced brain 99 times in 100.
		for (int i = 0; i < 1000; i++) {
			helper.tickBlock(iced);
		}
		helper.assertBlockPresent(block(NervoidIds.Blocks.ICED_DECOMPOSING_NERVOID_BRAIN), iced);
		helper.succeed();
	}

	/**
	 * A rotten brain lures from 5 blocks only, so this one stays inside the test. Not required: the brain is
	 * eaten only by an undead overlapping a one-block box at the brain's north-west corner (as the original's
	 * block coordinates did), and in about a quarter of runs the husk's wandering leaves it beside the brain
	 * outside that box. Worth a look in game.
	 */
	@GameTest(maxTicks = 600, required = false)
	public void theUndeadWalkToABrainAndEatIt(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos brain = new BlockPos(6, 1, 3);
		Block rotten = block(NervoidIds.Blocks.ROTTEN_NERVOID_BRAIN);
		helper.setBlock(brain, rotten);
		helper.spawn(EntityTypes.HUSK, new BlockPos(2, 1, 3));
		helper.succeedWhen(() -> {
			helper.assertTrue(helper.getBlockState(brain).isAir(), "the brain is eaten");
			helper.assertItemEntityPresent(rotten.asItem(), brain, 2.0);
		});
	}

	@GameTest
	public void theUndeadHuntABrainScentedCreature(GameTestHelper helper) {
		TestScenes.floor(helper);
		Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(1, 1, 1));
		Husk husk = helper.spawn(EntityTypes.HUSK, new BlockPos(6, 1, 6));
		pig.addEffect(new MobEffectInstance(NervoidModule.BRAIN_SCENTED, 200));
		helper.succeedWhen(() -> helper.assertTrue(husk.getTarget() == pig, "the husk targets the brain-scented pig"));
	}

	// --- helpers ------------------------------------------------------------------------

	private static Holder<MobEffect> underControl() {
		return BuiltInRegistries.MOB_EFFECT.get(CustodianIds.Effects.UNDER_CONTROL).orElseThrow();
	}

	private static Item item(Identifier id) {
		return BuiltInRegistries.ITEM.getValue(id);
	}

	private static Block block(Identifier id) {
		return BuiltInRegistries.BLOCK.getValue(id);
	}
}
