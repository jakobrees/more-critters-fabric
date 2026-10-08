package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.CritterlingsEIds;
import com.morecritters.fabric.module.critterling_system.CritterlingRarity;
import com.morecritters.fabric.module.critterlings_e.CobbleEntity;
import com.morecritters.fabric.module.critterlings_e.CritterlingsEModule;
import com.morecritters.fabric.module.critterlings_e.FresnoidEntity;
import com.morecritters.fabric.module.critterlings_e.LightflyEntity;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.phys.Vec3;

/** Fresnoid (yawns), cobble (sits, epic spins to music), lightfly (short-lived diving spark), and the sacks. */
public class CritterlingsETests {
	private static final BlockPos MIDDLE = new BlockPos(3, 1, 3);

	@GameTest(maxTicks = 200)
	public void theFresnoidYawnsWithItsMouthOpenForAboutASecondAndAHalf(GameTestHelper helper) {
		TestScenes.floor(helper);
		FresnoidEntity fresnoid = helper.spawnWithNoFreeWill(CritterlingsEModule.FRESNOID, MIDDLE);
		fresnoid.setRarity(CritterlingRarity.EPIC);
		long[] mouth = {-1, -1};
		helper.onEachTick(() -> {
			boolean open = fresnoid.textureName().equals("fresnoid_epic_yawn");
			if (open && mouth[0] < 0) mouth[0] = helper.getTick();
			if (!open && mouth[0] >= 0 && mouth[1] < 0) mouth[1] = helper.getTick();
			if (!open) helper.assertValueEqual(fresnoid.textureName(), "fresnoid_epic", "texture with the mouth shut");
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(mouth[1] >= 0, "the fresnoid has yawned");
			long open = mouth[1] - mouth[0];
			helper.assertTrue(open >= 27 && open <= 29, "mouth open for 28 ticks, was " + open);
		});
	}

	@GameTest(maxTicks = 120)
	public void theCobbleNeverMovesFromItsSpot(GameTestHelper helper) {
		TestScenes.floor(helper);
		CobbleEntity cobble = helper.spawn(CritterlingsEModule.COBBLE, MIDDLE);
		Vec3 start = cobble.position();
		helper.onEachTick(() -> helper.assertTrue(cobble.position().subtract(start).horizontalDistance() < 1.0E-3, "the cobble stays put"));
		helper.runAtTickTime(100, helper::succeed);
	}

	@GameTest(maxTicks = 80)
	public void cobblesTurnToARandomFacingOnceAndKeepIt(GameTestHelper helper) {
		TestScenes.floor(helper);
		List<CobbleEntity> cobbles = new java.util.ArrayList<>();
		for (int x = 0; x < 8; x += 2) {
			for (int z = 0; z < 8; z += 3) {
				cobbles.add(helper.spawn(CritterlingsEModule.COBBLE, new BlockPos(x, 1, z)));
			}
		}
		float[] facings = new float[cobbles.size()];
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(() -> {
				Set<Float> distinct = new HashSet<>();
				for (int i = 0; i < facings.length; i++) {
					facings[i] = cobbles.get(i).getYRot();
					distinct.add(facings[i]);
				}
				helper.assertTrue(distinct.size() > 1, "cobbles face different ways");
			})
			.thenIdle(40)
			.thenExecute(() -> {
				for (int i = 0; i < facings.length; i++) {
					helper.assertValueEqual(cobbles.get(i).getYRot(), facings[i], "facing of cobble " + i + " later on");
				}
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 60)
	public void onlyAnEpicCobbleSpinsWhileAJukeboxPlays(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(new BlockPos(3, 1, 5), Blocks.JUKEBOX.defaultBlockState().setValue(JukeboxBlock.HAS_RECORD, true));
		CobbleEntity normal = helper.spawn(CritterlingsEModule.COBBLE, new BlockPos(2, 1, 3));
		CobbleEntity rare = helper.spawn(CritterlingsEModule.COBBLE, new BlockPos(3, 1, 3));
		rare.setRarity(CritterlingRarity.RARE);
		CobbleEntity epic = helper.spawn(CritterlingsEModule.COBBLE, new BlockPos(4, 1, 3));
		epic.setRarity(CritterlingRarity.EPIC);
		float[] before = new float[3];
		helper.startSequence()
			.thenIdle(3)
			.thenExecute(() -> {
				before[0] = normal.getYRot();
				before[1] = rare.getYRot();
				before[2] = epic.getYRot();
			})
			.thenIdle(20)
			.thenExecute(() -> {
				helper.assertValueEqual(normal.getYRot(), before[0], "facing of a normal cobble");
				helper.assertValueEqual(rare.getYRot(), before[1], "facing of a rare cobble");
				helper.assertTrue(Math.abs(epic.getYRot() - before[2] - 20.0F) < 1.0E-3F, "an epic cobble turns 1 degree a tick, turned " + (epic.getYRot() - before[2]));
				for (CobbleEntity cobble : List.of(normal, rare, epic)) {
					helper.assertFalse(cobble.isDancing(), "a cobble never shows a dance");
				}
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 120)
	public void aLightflyFizzlesOutAfterFiveSeconds(GameTestHelper helper) {
		TestScenes.floor(helper);
		LightflyEntity lightfly = helper.spawnWithNoFreeWill(CritterlingsEModule.LIGHTFLY, new BlockPos(3, 3, 3));
		helper.runAtTickTime(95, () -> helper.assertFalse(lightfly.isRemoved(), "the lightfly is still there after 95 ticks"));
		helper.runAtTickTime(102, () -> {
			helper.assertTrue(lightfly.isRemoved(), "the lightfly is gone after 100 ticks");
			helper.succeed();
		});
	}

	@GameTest
	public void aReleasedLightflyShootsUpward(GameTestHelper helper) {
		TestScenes.floor(helper);
		LightflyEntity lightfly = helper.spawn(CritterlingsEModule.LIGHTFLY, new BlockPos(3, 2, 3), EntitySpawnReason.MOB_SUMMONED);
		helper.assertValueEqual(lightfly.getDeltaMovement(), new Vec3(0.0, 1.0, 0.0), "motion of a new lightfly");
		helper.assertTrue(lightfly.isNoGravity(), "a lightfly ignores gravity");
		helper.succeed();
	}

	@GameTest
	public void weaponsAndHazardsDoNotHurtALightflyButAMobsBiteDoes(GameTestHelper helper) {
		TestScenes.floor(helper);
		LightflyEntity lightfly = helper.spawnWithNoFreeWill(CritterlingsEModule.LIGHTFLY, new BlockPos(3, 3, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		DamageSources sources = helper.getLevel().damageSources();
		for (DamageSource source : List.of(sources.playerAttack(player), sources.inFire(), sources.fall(), sources.drown(),
				sources.cactus(), sources.lightningBolt(), sources.explosion(null, null), sources.wither(), sources.dragonBreath())) {
			helper.hurt(lightfly, source, 4.0F);
			helper.assertValueEqual(lightfly.getHealth(), 10.0F, "health after " + source.getMsgId());
		}
		helper.assertTrue(lightfly.fireImmune(), "a lightfly does not burn");
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		helper.hurt(lightfly, sources.mobAttack(zombie), 4.0F);
		helper.assertValueEqual(lightfly.getHealth(), 6.0F, "health after a zombie's hit");
		helper.succeed();
	}

	@GameTest(maxTicks = 100)
	public void aLightflyWithATargetDivesAtItAndSetsItAlight(GameTestHelper helper) {
		TestScenes.floor(helper);
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(1, 1, 1));
		LightflyEntity lightfly = helper.spawn(CritterlingsEModule.LIGHTFLY, new BlockPos(6, 4, 6));
		lightfly.setTarget(pig);
		helper.succeedWhen(() -> {
			helper.assertTrue(lightfly.isRemoved(), "the lightfly burns out on hitting");
			helper.assertTrue(pig.isOnFire(), "the target is set alight");
		});
	}

	@GameTest
	public void fresnoidAndCobbleSacksLetOutTheirRarity(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(CritterlingsEIds.Items.CRITTERLING_SACK_COBBLE_RARE)));
		TestScenes.useItemOn(helper, player, new BlockPos(2, 0, 3), Direction.UP);
		helper.assertValueEqual(helper.findOneEntity(CritterlingsEModule.COBBLE).rarity(), CritterlingRarity.RARE, "rarity of the released cobble");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(CritterlingsEIds.Items.CRITTERLING_SACK_FRESNOID_EPIC)));
		TestScenes.useItemOn(helper, player, new BlockPos(5, 0, 3), Direction.UP);
		helper.assertValueEqual(helper.findOneEntity(CritterlingsEModule.FRESNOID).rarity(), CritterlingRarity.EPIC, "rarity of the released fresnoid");
		helper.succeed();
	}

	@GameTest
	public void fresnoidSacksAreUncommonAndCobbleSacksEpic(GameTestHelper helper) {
		for (Identifier id : List.of(CritterlingsEIds.Items.CRITTERLING_SACK_FRESNOID, CritterlingsEIds.Items.CRITTERLING_SACK_FRESNOID_RARE, CritterlingsEIds.Items.CRITTERLING_SACK_FRESNOID_EPIC)) {
			helper.assertValueEqual(new ItemStack(item(id)).get(DataComponents.RARITY), Rarity.UNCOMMON, "rarity of " + id);
		}
		for (Identifier id : List.of(CritterlingsEIds.Items.CRITTERLING_SACK_COBBLE, CritterlingsEIds.Items.CRITTERLING_SACK_COBBLE_RARE, CritterlingsEIds.Items.CRITTERLING_SACK_COBBLE_EPIC)) {
			helper.assertValueEqual(new ItemStack(item(id)).get(DataComponents.RARITY), Rarity.EPIC, "rarity of " + id);
		}
		helper.succeed();
	}

	@GameTest
	public void eachRarityHasItsOwnLook(GameTestHelper helper) {
		TestScenes.floor(helper);
		CobbleEntity cobble = helper.spawnWithNoFreeWill(CritterlingsEModule.COBBLE, new BlockPos(2, 1, 3));
		FresnoidEntity fresnoid = helper.spawnWithNoFreeWill(CritterlingsEModule.FRESNOID, new BlockPos(5, 1, 3));
		String[] cobbles = {"cobble", "cobble_rare", "cobble_epic"};
		String[] fresnoids = {"fresnoid", "fresnoid_rare", "fresnoid_epic"};
		for (CritterlingRarity rarity : CritterlingRarity.values()) {
			cobble.setRarity(rarity);
			fresnoid.setRarity(rarity);
			helper.assertValueEqual(cobble.textureName(), cobbles[rarity.ordinal()], "cobble texture");
			helper.assertValueEqual(fresnoid.textureName(), fresnoids[rarity.ordinal()], "fresnoid texture");
		}
		helper.succeed();
	}

	private static Item item(Identifier id) {
		return BuiltInRegistries.ITEM.getValue(id);
	}
}
