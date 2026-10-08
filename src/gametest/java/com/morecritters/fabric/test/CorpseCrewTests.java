package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.CorpseCrewIds;
import com.morecritters.fabric.ids.CorpseGearIds;
import com.morecritters.fabric.ids.ShipFittingsIds;
import com.morecritters.fabric.module.corpse_crew.CorpseBarnacleBlock;
import com.morecritters.fabric.module.corpse_crew.CorpseCaptainEntity;
import com.morecritters.fabric.module.corpse_crew.CorpseCrewModule;
import com.morecritters.fabric.module.corpse_crew.CorpseMateEntity;
import com.morecritters.fabric.module.corpse_crew.CorpseQuartermasterEntity;
import com.morecritters.fabric.module.corpse_crew.CorpseTankEntity;
import com.morecritters.fabric.module.corpse_crew.TamedCorpseParrotEntity;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/** The corpse crew: the muster, the dolls, the tamed parrot, whom the crew attacks and how its officers fight. */
public class CorpseCrewTests {
	@GameTest
	public void aMusterLeavesAFullCrewAndVanishes(GameTestHelper helper) {
		TestScenes.floor(helper);
		Entity muster = helper.spawn(CorpseCrewModule.CORPSE_CREW, new BlockPos(3, 1, 3));
		helper.succeedWhen(() -> {
			helper.assertTrue(muster.isRemoved(), "the muster removes itself");
			int mates = helper.getEntities(CorpseCrewModule.CORPSE_MATE).size();
			helper.assertTrue(mates >= 1 && mates <= 2, "one or two mates, got " + mates);
			helper.assertValueEqual(helper.getEntities(CorpseCrewModule.CORPSE_QUARTERMASTER).size(), 1, "quartermasters");
			helper.assertValueEqual(helper.getEntities(CorpseCrewModule.CORPSE_TANK).size(), 1, "tanks");
			helper.assertValueEqual(helper.getEntities(CorpseCrewModule.CORPSE_CAPTAIN).size(), 1, "captains");
			helper.assertValueEqual(helper.getEntities(CorpseCrewModule.CORPSE_PARROT).size(), 1, "parrots");
			helper.assertTrue(helper.getEntities(CorpseCrewModule.CORPSE_LOOKOUT).size() <= 1, "at most one lookout");
		});
	}

	@GameTest
	public void aSpawnDollIsUsedUpEvenInCreativeAndSpawnsOnTheClickedFace(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.getValue(CorpseCrewIds.Items.CORPSE_MATE_SPAWN_DOLL)));
		TestScenes.useItemOn(helper, player, new BlockPos(3, 0, 3), Direction.UP);
		helper.assertTrue(player.getMainHandItem().isEmpty(), "the doll is used up in creative");
		helper.succeedWhen(() -> helper.assertEntityPresent(CorpseCrewModule.CORPSE_MATE, new BlockPos(3, 1, 3)));
	}

	@GameTest
	public void theCorpseParrotItemGivesAParrotOwnedByTheUser(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.getValue(CorpseCrewIds.Items.CORPSE_PARROT_ITEM)));
		TestScenes.useItemOn(helper, player, new BlockPos(3, 0, 3), Direction.UP);
		helper.assertTrue(player.getMainHandItem().isEmpty(), "the parrot item is used up");
		helper.succeedWhen(() -> {
			helper.assertEntityPresent(CorpseCrewModule.TAMED_CORPSE_PARROT, new BlockPos(3, 1, 3));
			TamedCorpseParrotEntity parrot = helper.getEntities(CorpseCrewModule.TAMED_CORPSE_PARROT).getFirst();
			helper.assertTrue(parrot.isTame(), "the parrot is tame");
			helper.assertValueEqual(parrot.getOwnerReference().getUUID(), player.getUUID(), "the parrot's owner");
		});
	}

	@GameTest
	public void theOwnerTogglesSittingAndAStrangerCannot(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
		TamedCorpseParrotEntity parrot = helper.spawn(CorpseCrewModule.TAMED_CORPSE_PARROT, new BlockPos(3, 1, 3));
		parrot.tame(owner);

		parrot.mobInteract(owner, InteractionHand.MAIN_HAND);
		helper.assertTrue(parrot.isOrderedToSit(), "the owner's first right-click makes it sit");
		parrot.mobInteract(stranger, InteractionHand.MAIN_HAND);
		helper.assertTrue(parrot.isOrderedToSit(), "a stranger's right-click changes nothing");
		helper.assertValueEqual(parrot.getOwnerReference().getUUID(), owner.getUUID(), "owner after a stranger's click");
		parrot.mobInteract(owner, InteractionHand.MAIN_HAND);
		helper.assertFalse(parrot.isOrderedToSit(), "the owner's second right-click makes it stand");
		helper.succeed();
	}

	@GameTest
	public void anUntamedParrotIsClaimedByTheFirstPlayerToRightClickIt(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player first = helper.makeMockPlayer(GameType.SURVIVAL);
		Player second = helper.makeMockPlayer(GameType.SURVIVAL);
		TamedCorpseParrotEntity parrot = helper.spawn(CorpseCrewModule.TAMED_CORPSE_PARROT, new BlockPos(3, 1, 3));
		helper.assertFalse(parrot.isTame(), "a summoned parrot starts untamed");
		parrot.mobInteract(first, InteractionHand.MAIN_HAND);
		parrot.mobInteract(second, InteractionHand.MAIN_HAND);
		helper.assertTrue(parrot.isTame(), "tamed by a right-click");
		helper.assertValueEqual(parrot.getOwnerReference().getUUID(), first.getUUID(), "the first player owns it");
		helper.assertFalse(parrot.isOrderedToSit(), "claiming it does not make it sit");
		helper.succeed();
	}

	@GameTest
	public void theCrewGoesForMonstersButNotCreepers(GameTestHelper helper) {
		TestScenes.floor(helper);
		CorpseMateEntity mate = helper.spawnWithNoFreeWill(CorpseCrewModule.CORPSE_MATE, new BlockPos(1, 1, 3));
		helper.spawnWithNoFreeWill(EntityTypes.CREEPER, new BlockPos(4, 1, 3));
		Zombie[] zombie = new Zombie[1];
		helper.startSequence()
			.thenExecuteAfter(10, () -> {
				helper.assertTrue(mate.getTarget() == null, "the mate's target with only a creeper about");
				zombie[0] = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(5, 1, 3));
			})
			.thenWaitUntil(() -> helper.assertTrue(mate.getTarget() == zombie[0], "the mate's target"))
			.thenSucceed();
	}

	@GameTest
	public void anyPieceOfPirateGearPassesForCrew(GameTestHelper helper) {
		TestScenes.floor(helper);
		CorpseMateEntity mate = helper.spawnWithNoFreeWill(CorpseCrewModule.CORPSE_MATE, new BlockPos(1, 1, 3));
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(5, 1, 3));
		zombie.setItemSlot(EquipmentSlot.FEET, new ItemStack(BuiltInRegistries.ITEM.getValue(CorpseGearIds.Items.PIRATE_BOOTS)));
		helper.startSequence()
			.thenExecuteAfter(10, () -> {
				helper.assertTrue(mate.getTarget() == null, "the mate's target while the zombie wears pirate boots");
				zombie.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
			})
			.thenWaitUntil(() -> helper.assertTrue(mate.getTarget() == zombie, "the mate's target once the boots are off"))
			.thenSucceed();
	}

	@GameTest
	public void theCrewShrugsOffExplosionsAndDrowning(GameTestHelper helper) {
		TestScenes.floor(helper);
		CorpseTankEntity tank = helper.spawnWithNoFreeWill(CorpseCrewModule.CORPSE_TANK, new BlockPos(3, 1, 3));
		Vec3 at = helper.absoluteVec(new Vec3(3.5, 1.5, 3.5));
		helper.getLevel().explode(null, at.x, at.y, at.z, 2.0F, Level.ExplosionInteraction.NONE);
		helper.assertValueEqual(tank.getHealth(), tank.getMaxHealth(), "tank health after an explosion beside it");
		helper.assertFalse(tank.hurtServer(helper.getLevel(), helper.getLevel().damageSources().drown(), 5.0F), "drowning hurts the tank");
		helper.assertValueEqual(tank.getHealth(), tank.getMaxHealth(), "tank health after drowning");
		helper.succeed();
	}

	@GameTest
	public void theCaptainDropsATreasureKey(GameTestHelper helper) {
		TestScenes.floor(helper);
		CorpseCaptainEntity captain = helper.spawnWithNoFreeWill(CorpseCrewModule.CORPSE_CAPTAIN, new BlockPos(3, 1, 3));
		captain.kill(helper.getLevel());
		helper.succeedWhen(() -> helper.assertItemEntityPresent(BuiltInRegistries.ITEM.getValue(ShipFittingsIds.Items.TREASURE_KEY)));
	}

	@GameTest(maxTicks = 100)
	public void inAFightTheQuartermasterThrowsRumThatHealsTheCrew(GameTestHelper helper) {
		TestScenes.floor(helper);
		CorpseQuartermasterEntity quartermaster = helper.spawnWithNoFreeWill(CorpseCrewModule.CORPSE_QUARTERMASTER, new BlockPos(1, 1, 3));
		CorpseMateEntity mate = helper.spawnWithNoFreeWill(CorpseCrewModule.CORPSE_MATE, new BlockPos(2, 1, 3));
		helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(5, 1, 3));
		quartermaster.setHealth(5.0F);
		mate.setHealth(5.0F);
		setTimer(helper, quartermaster, "heal", 3);
		helper.succeedWhen(() -> {
			helper.assertTrue(quartermaster.getHealth() >= 10.0F, "quartermaster healed by 5-10, has " + quartermaster.getHealth());
			helper.assertTrue(mate.getHealth() >= 10.0F, "mate healed by 5-10, has " + mate.getHealth());
		});
	}

	@GameTest(maxTicks = 100)
	public void withNobodyToFightTheQuartermasterKeepsItsRum(GameTestHelper helper) {
		TestScenes.floor(helper);
		CorpseQuartermasterEntity quartermaster = helper.spawnWithNoFreeWill(CorpseCrewModule.CORPSE_QUARTERMASTER, new BlockPos(3, 1, 3));
		quartermaster.setHealth(5.0F);
		setTimer(helper, quartermaster, "heal", 3);
		helper.runAfterDelay(30, () -> {
			helper.assertValueEqual(quartermaster.getHealth(), 5.0F, "quartermaster health without a fight");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 100)
	public void theTankFiresAPearlAtWhatItSeesAfterTakingAim(GameTestHelper helper) {
		TestScenes.floor(helper);
		CorpseTankEntity tank = helper.spawnWithNoFreeWill(CorpseCrewModule.CORPSE_TANK, new BlockPos(1, 1, 3));
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(6, 1, 3));
		setTimer(helper, tank, "fire", 3);
		helper.startSequence()
			.thenExecuteAfter(15, () -> helper.assertFalse(save(helper, tank).getBooleanOr("DataHasShot", true), "fired before the 18-tick aim"))
			.thenWaitUntil(() -> {
				helper.assertTrue(save(helper, tank).getBooleanOr("DataHasShot", false), "the tank has fired");
				helper.assertTrue(!zombie.isAlive() || zombie.getHealth() < zombie.getMaxHealth(), "the pearl hurt the zombie");
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 120)
	public void theCaptainsRallyStrengthensTheCrew(GameTestHelper helper) {
		TestScenes.floor(helper);
		CorpseCaptainEntity captain = helper.spawnWithNoFreeWill(CorpseCrewModule.CORPSE_CAPTAIN, new BlockPos(1, 1, 3));
		CorpseMateEntity mate = helper.spawnWithNoFreeWill(CorpseCrewModule.CORPSE_MATE, new BlockPos(1, 1, 5));
		helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(6, 1, 3));
		setTimer(helper, captain, "callcrew", 3);
		helper.startSequence()
			.thenExecuteAfter(20, () -> helper.assertFalse(mate.hasEffect(MobEffects.STRENGTH), "strength before the 40-tick call"))
			.thenWaitUntil(() -> {
				// The rally also hands out Regeneration II, but the crew is undead and shrugs it off, as in the
				// original (MobType.UNDEAD there, the minecraft:undead tag here).
				for (LivingEntity member : List.<LivingEntity>of(captain, mate)) {
					MobEffectInstance strength = member.getEffect(MobEffects.STRENGTH);
					helper.assertTrue(strength != null && strength.getAmplifier() == 1, member.getType() + " has Strength II");
					helper.assertTrue(strength.getDuration() <= 200 && strength.getDuration() > 150, "strength lasts 200 ticks, has " + strength.getDuration());
				}
			})
			.thenSucceed();
	}

	/** Not required: it needs a player standing in the level, which only the deprecated in-level mock player gives. */
	@GameTest(maxTicks = 200, required = false)
	@SuppressWarnings("removal")
	public void aBarnacleBitesWhatStandsInItWhileAPlayerIsThere(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos pos = new BlockPos(3, 1, 3);
		helper.setBlock(pos, CorpseCrewModule.CORPSE_BARNACLE.defaultBlockState()
			.setValue(CorpseBarnacleBlock.FACE, AttachFace.FLOOR).setValue(CorpseBarnacleBlock.FACING, Direction.NORTH));
		LivingEntity pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(3.5, 1.4, 3.5));
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Vec3 inside = helper.absoluteVec(new Vec3(3.5, 1.4, 3.5));
		helper.onEachTick(() -> player.snapTo(inside.x, inside.y, inside.z));
		helper.succeedWhen(() -> {
			helper.assertValueEqual(pig.getMaxHealth() - pig.getHealth(), 4.0F, "damage the barnacle did to the pig");
			helper.assertValueEqual(player.getHealth(), player.getMaxHealth(), "the creative player's health");
			helper.getLevel().getServer().getPlayerList().remove(player);
		});
	}

	// --- Helpers -------------------------------------------------------------------------

	/** The entity's saved data. */
	private static CompoundTag save(GameTestHelper helper, Entity entity) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
		entity.saveWithoutId(output);
		return output.buildResult();
	}

	/** Sets one of the member's saved timers (saved under the original's names), as a reloaded world would. */
	private static void setTimer(GameTestHelper helper, Entity entity, String key, int ticks) {
		CompoundTag tag = save(helper, entity);
		tag.putInt(key, ticks);
		entity.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
	}
}
