package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.NightshroomIds;
import com.morecritters.fabric.module.mightshroom.MightshroomEntity;
import com.morecritters.fabric.module.mightshroom.MightshroomModule;
import com.morecritters.fabric.module.nightshroom.AncientSkeletonEntity;
import com.morecritters.fabric.module.nightshroom.FrightshroomEntity;
import com.morecritters.fabric.module.nightshroom.MoriRootsEntity;
import com.morecritters.fabric.module.nightshroom.NightshroomEntity;
import com.morecritters.fabric.module.nightshroom.NightshroomModule;
import com.morecritters.fabric.module.nightshroom.RotZombieEntity;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * The nightshroom module: taming and sitting the nightshroom, its Natsirt coat, fungal flesh, the ancient
 * skeleton (placing, picking up, invulnerability), mori roots, spawn dolls, the frightshroom's bond with its
 * rot zombies, the rot zombie's arrow immunity and the Spawn Nightshroom effect.
 */
public class NightshroomTests {
	// --- Nightshroom ---------------------------------------------------------------------------------------

	@GameTest
	@SuppressWarnings("removal") // the only helper that puts a real player in the level
	public void itIsTamedByTheNearestPlayerWhenItAppears(GameTestHelper helper) {
		TestScenes.floor(helper);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		try {
			Vec3 at = helper.absoluteVec(new Vec3(6.5, 1.0, 6.5));
			player.teleportTo(at.x, at.y, at.z);
			NightshroomEntity nightshroom = helper.spawn(NightshroomModule.NIGHTSHROOM, new BlockPos(2, 1, 2), EntitySpawnReason.MOB_SUMMONED);
			helper.assertTrue(nightshroom.isTame(), "tamed when it appears");
			helper.assertTrue(nightshroom.isOwnedBy(player), "owned by the nearby player");
		} finally {
			helper.getLevel().getServer().getPlayerList().remove(player);
		}
		helper.succeed();
	}

	@GameTest
	public void anUntamedOneIsTamedByARightClickAndDoesNotSitYet(GameTestHelper helper) {
		TestScenes.floor(helper);
		NightshroomEntity nightshroom = helper.spawn(NightshroomModule.NIGHTSHROOM, new BlockPos(3, 1, 3));
		helper.assertFalse(nightshroom.isTame(), "a nightshroom spawned with no player near is tame");
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		rightClick(player, nightshroom);
		helper.assertTrue(nightshroom.isTame(), "tamed by the click");
		helper.assertTrue(nightshroom.isOwnedBy(player), "owned by the player who clicked");
		helper.assertFalse(nightshroom.isOrderedToSit(), "sitting after the taming click");
		helper.succeed();
	}

	@GameTest
	public void itsOwnerMakesItSitAndStand(GameTestHelper helper) {
		TestScenes.floor(helper);
		NightshroomEntity nightshroom = helper.spawn(NightshroomModule.NIGHTSHROOM, new BlockPos(3, 1, 3));
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		nightshroom.tame(owner);
		rightClick(owner, nightshroom);
		helper.assertTrue(nightshroom.isOrderedToSit(), "sits on the owner's click");
		helper.assertTrue(nightshroom.isInSittingPose(), "in the sitting pose");
		rightClick(owner, nightshroom);
		helper.assertFalse(nightshroom.isOrderedToSit(), "stands on the owner's next click");

		Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
		rightClick(stranger, nightshroom);
		helper.assertFalse(nightshroom.isOrderedToSit(), "sits on a stranger's click");
		helper.assertTrue(nightshroom.isOwnedBy(owner), "still owned by its owner");
		helper.succeed();
	}

	@GameTest
	public void namedNatsirtItWearsItsOwnCoat(GameTestHelper helper) {
		TestScenes.floor(helper);
		NightshroomEntity nightshroom = helper.spawn(NightshroomModule.NIGHTSHROOM, new BlockPos(3, 1, 3));
		helper.assertValueEqual(nightshroom.textureName(), "nightshroom", "texture without a name");
		nightshroom.setCustomName(Component.literal("Natsirt"));
		helper.assertValueEqual(nightshroom.textureName(), "nightshroom_texture_natsirt", "texture named Natsirt");
		nightshroom.setCustomName(Component.literal("natsirt"));
		helper.assertValueEqual(nightshroom.textureName(), "nightshroom_texture_natsirt", "texture named natsirt");
		helper.succeed();
	}

	@GameTest
	public void itTakesNoFallDamage(GameTestHelper helper) {
		TestScenes.floor(helper);
		NightshroomEntity nightshroom = helper.spawn(NightshroomModule.NIGHTSHROOM, new BlockPos(3, 1, 3));
		nightshroom.setNoAi(true);
		ServerLevel level = helper.getLevel();
		nightshroom.hurtServer(level, level.damageSources().fall(), 50.0F);
		helper.assertValueEqual(nightshroom.getHealth(), 150.0F, "health after a fall");
		helper.succeed();
	}

	// --- Fungal flesh --------------------------------------------------------------------------------------

	@GameTest(maxTicks = 20)
	public void fungalFleshTurnsAZombieIntoAFungalZombie(GameTestHelper helper) {
		TestScenes.floor(helper);
		Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(3, 1, 3));
		zombie.setNoAi(true);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(NightshroomIds.Items.FUNGAL_FLESH), 2));
		rightClick(player, zombie);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "flesh left");
		helper.assertTrue(zombie.isRemoved(), "the zombie is gone");
		helper.assertValueEqual(helper.getEntities(NightshroomModule.FUNGAL_ZOMBIE).size(), 1, "fungal zombies");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void fungalFleshTurnsACowIntoAMooshroom(GameTestHelper helper) {
		TestScenes.floor(helper);
		Cow cow = helper.spawn(EntityTypes.COW, new BlockPos(3, 1, 3));
		cow.setNoAi(true);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(NightshroomIds.Items.FUNGAL_FLESH), 2));
		rightClick(player, cow);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "flesh left");
		helper.assertTrue(cow.isRemoved(), "the cow is gone");
		helper.assertValueEqual(helper.getEntities(EntityTypes.MOOSHROOM).size(), 1, "mooshrooms");
		helper.succeed();
	}

	// --- Ancient skeleton ----------------------------------------------------------------------------------

	@GameTest
	public void aPlacedSkeletonIsPickedUpBySneakingWithAnEmptyHand(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Item bones = item(NightshroomIds.Items.ANCIENT_SKELETON_ITEM);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(bones));
		TestScenes.useItemOn(helper, player, new BlockPos(3, 0, 3), Direction.UP);
		helper.assertTrue(player.getMainHandItem().isEmpty(), "the bones are used up");
		AncientSkeletonEntity skeleton = helper.findOneEntity(NightshroomModule.ANCIENT_SKELETON);
		helper.assertEntityPresent(NightshroomModule.ANCIENT_SKELETON, new BlockPos(3, 1, 3));

		// Not without sneaking.
		rightClick(player, skeleton);
		helper.assertTrue(skeleton.isAlive(), "picked up without sneaking");
		player.setShiftKeyDown(true);
		rightClick(player, skeleton);
		helper.assertTrue(skeleton.isRemoved(), "the skeleton is picked up");
		helper.assertTrue(player.getMainHandItem().is(bones), "the bones are back in hand");
		helper.succeed();
	}

	@GameTest
	public void aSkeletonFoundInTheWorldCannotBePickedUp(GameTestHelper helper) {
		TestScenes.floor(helper);
		AncientSkeletonEntity skeleton = helper.spawn(NightshroomModule.ANCIENT_SKELETON, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setShiftKeyDown(true);
		rightClick(player, skeleton);
		helper.assertTrue(skeleton.isAlive(), "a skeleton not placed by a player is picked up");
		helper.assertTrue(player.getMainHandItem().isEmpty(), "hand after the click");
		helper.succeed();
	}

	@GameTest
	public void anAncientSkeletonTakesNoDamage(GameTestHelper helper) {
		TestScenes.floor(helper);
		AncientSkeletonEntity skeleton = helper.spawn(NightshroomModule.ANCIENT_SKELETON, new BlockPos(3, 1, 3));
		ServerLevel level = helper.getLevel();
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		skeleton.hurtServer(level, level.damageSources().playerAttack(player), 100.0F);
		skeleton.hurtServer(level, level.damageSources().lava(), 100.0F);
		skeleton.hurtServer(level, level.damageSources().magic(), 100.0F);
		helper.assertTrue(skeleton.isAlive(), "the skeleton survives");
		helper.assertValueEqual(skeleton.getHealth(), skeleton.getMaxHealth(), "skeleton health");
		helper.succeed();
	}

	// --- Mori roots, dolls, frightshroom, rot zombies -----------------------------------------------------

	@GameTest(maxTicks = 80)
	public void moriRootsHoldACreatureForTwoAndAHalfSecondsThenSink(GameTestHelper helper) {
		TestScenes.floor(helper);
		MoriRootsEntity roots = helper.spawn(NightshroomModule.MORI_ROOTS, new BlockPos(3, 1, 3), EntitySpawnReason.MOB_SUMMONED);
		Husk husk = helper.spawn(EntityTypes.HUSK, new BlockPos(3, 1, 3));
		// The husk keeps trying to walk off at a brisk pace; the roots pull it back every tick.
		for (int tick = 5; tick <= 25; tick++) {
			helper.runAtTickTime(tick, () -> husk.setDeltaMovement(0.3, husk.getDeltaMovement().y, 0.0));
		}
		helper.runAtTickTime(30, () -> helper.assertTrue(husk.position().distanceTo(roots.position()) < 0.75,
			"the husk is held by the roots (" + husk.position().distanceTo(roots.position()) + " away)"));
		helper.runAtTickTime(40, () -> helper.assertTrue(roots.isAlive(), "the roots last 2.5 s"));
		helper.succeedWhen(() -> helper.assertTrue(roots.isRemoved(), "the roots sink away"));
	}

	@GameTest
	public void moriRootsCannotBeHurt(GameTestHelper helper) {
		TestScenes.floor(helper);
		MoriRootsEntity roots = helper.spawn(NightshroomModule.MORI_ROOTS, new BlockPos(3, 1, 3), EntitySpawnReason.MOB_SUMMONED);
		ServerLevel level = helper.getLevel();
		roots.hurtServer(level, level.damageSources().playerAttack(helper.makeMockPlayer(GameType.SURVIVAL)), 100.0F);
		helper.assertValueEqual(roots.getHealth(), roots.getMaxHealth(), "roots health");
		helper.succeed();
	}

	@GameTest
	public void aSpawnDollSetsItsCreatureOnTheClickedFaceAndIsUsedUpEvenInCreative(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		GameType.CREATIVE.updatePlayerAbilities(player.getAbilities());
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(NightshroomIds.Items.ROT_ZOMBIE_SPAWN_DOLL), 2));
		TestScenes.useItemOn(helper, player, new BlockPos(3, 0, 3), Direction.UP);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "dolls left");
		helper.assertEntityPresent(NightshroomModule.ROT_ZOMBIE, new BlockPos(3, 1, 3));
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void rotZombiesCrumbleWhenTheFrightshroomDies(GameTestHelper helper) {
		TestScenes.floor(helper);
		FrightshroomEntity frightshroom = helper.spawn(NightshroomModule.FRIGHTSHROOM, new BlockPos(1, 1, 1));
		frightshroom.setNoAi(true);
		RotZombieEntity near = helper.spawn(NightshroomModule.ROT_ZOMBIE, new BlockPos(6, 1, 6));
		near.setNoAi(true);
		RotZombieEntity other = helper.spawn(NightshroomModule.ROT_ZOMBIE, new BlockPos(6, 1, 1));
		other.setNoAi(true);
		ServerLevel level = helper.getLevel();
		frightshroom.hurtServer(level, level.damageSources().playerAttack(helper.makeMockPlayer(GameType.SURVIVAL)), 1000.0F);
		helper.succeedWhen(() -> {
			helper.assertTrue(frightshroom.isDeadOrDying(), "the frightshroom is dead");
			helper.assertTrue(near.isRemoved() && other.isRemoved(), "its rot zombies crumble");
		});
	}

	@GameTest
	public void arrowsPassThroughARotZombie(GameTestHelper helper) {
		TestScenes.floor(helper);
		RotZombieEntity zombie = helper.spawn(NightshroomModule.ROT_ZOMBIE, new BlockPos(3, 1, 3));
		zombie.setNoAi(true);
		ServerLevel level = helper.getLevel();
		Arrow arrow = helper.spawn(EntityTypes.ARROW, new BlockPos(6, 1, 6));
		zombie.hurtServer(level, level.damageSources().arrow(arrow, null), 2.0F);
		helper.assertValueEqual(zombie.getHealth(), 5.0F, "rot zombie health after an arrow");
		zombie.hurtServer(level, level.damageSources().playerAttack(helper.makeMockPlayer(GameType.SURVIVAL)), 2.0F);
		helper.assertValueEqual(zombie.getHealth(), 3.0F, "rot zombie health after a player's blow");
		helper.succeed();
	}

	// --- Spawn Nightshroom ---------------------------------------------------------------------------------

	@GameTest(maxTicks = 80)
	public void spawnNightshroomTurnsAMightshroomIntoANightshroomWhenItEnds(GameTestHelper helper) {
		TestScenes.floor(helper);
		MightshroomEntity mightshroom = helper.spawn(MightshroomModule.MIGHTSHROOM, new BlockPos(3, 1, 3));
		mightshroom.setNoAi(true);
		mightshroom.addEffect(new MobEffectInstance(NightshroomModule.SPAWN_NIGHTSHROOM, 20));
		helper.runAtTickTime(10, () -> helper.assertTrue(mightshroom.isAlive(), "still a mightshroom while the effect lasts"));
		helper.succeedWhen(() -> {
			helper.assertTrue(mightshroom.isRemoved(), "the mightshroom is gone");
			helper.assertValueEqual(helper.getEntities(NightshroomModule.NIGHTSHROOM).size(), 1, "nightshrooms");
		});
	}

	// --- Helpers -------------------------------------------------------------------------------------------

	/** The server's handling of a right-click on an entity: Fabric's use-entity event first, then the entity. */
	private static InteractionResult rightClick(Player player, Entity target) {
		InteractionResult result = UseEntityCallback.EVENT.invoker().interact(player, player.level(), InteractionHand.MAIN_HAND, target, null);
		if (result != InteractionResult.PASS) {
			return result;
		}
		return player.interactOn(target, InteractionHand.MAIN_HAND, target.position());
	}

	private static Item item(Identifier id) {
		return BuiltInRegistries.ITEM.getValue(id);
	}
}
