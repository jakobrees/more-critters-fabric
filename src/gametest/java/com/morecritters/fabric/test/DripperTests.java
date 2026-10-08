package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.MightshroomIds;
import com.morecritters.fabric.module.dripper.DripperEntity;
import com.morecritters.fabric.module.dripper.DripperModule;
import com.morecritters.fabric.module.dripper.DripstoneWallMaskBlock;
import com.morecritters.fabric.module.dripper.SlashEffectEntity;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The dripper: a leaping cave monster with a named skin; the Slashklub's crouching slash and the slash effect it
 * releases; the dripstone wall mask.
 */
public class DripperTests {
	@GameTest
	public void itShrugsOffFallsAndStalagmites(GameTestHelper helper) {
		TestScenes.floor(helper);
		DripperEntity dripper = helper.spawnWithNoFreeWill(DripperModule.DRIPPER, new BlockPos(3, 1, 3));
		var sources = helper.getLevel().damageSources();
		dripper.hurtServer(helper.getLevel(), sources.fall(), 5.0F);
		dripper.hurtServer(helper.getLevel(), sources.stalagmite(), 5.0F);
		helper.assertValueEqual(dripper.getHealth(), dripper.getMaxHealth(), "health after a fall and a stalagmite");
		dripper.hurtServer(helper.getLevel(), sources.generic(), 5.0F);
		helper.assertValueEqual(dripper.getHealth(), dripper.getMaxHealth() - 5.0F, "health after a generic hit");
		helper.succeed();
	}

	@GameTest
	public void namedMurmurtalItWearsTheMurmurtalSkin(GameTestHelper helper) {
		TestScenes.floor(helper);
		DripperEntity dripper = helper.spawnWithNoFreeWill(DripperModule.DRIPPER, new BlockPos(3, 1, 3));
		helper.assertValueEqual(dripper.textureName(), "dripper", "texture unnamed");
		String[][] names = {{"Murmurtal!", "dripper_murmurtal"}, {"murmurtal!", "dripper_murmurtal"}, {"Murmurtal", "dripper"}};
		for (String[] name : names) {
			dripper.setCustomName(Component.literal(name[0]));
			helper.assertValueEqual(dripper.textureName(), name[1], "texture when named " + name[0]);
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void itLeapsAtATargetMoreThanFourBlocksAway(GameTestHelper helper) {
		TestScenes.floor(helper);
		DripperEntity dripper = facingSouth(helper.spawnWithNoFreeWill(DripperModule.DRIPPER, new BlockPos(3, 1, 0)));
		Pig target = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 1, 7));
		dripper.setTarget(target);
		setField(dripper, "rollTimer", 1);
		double startY = dripper.getY();
		double startZ = dripper.getZ();
		helper.succeedWhen(() -> {
			helper.assertTrue(dripper.getY() > startY + 1.0, "the dripper rose more than a block");
			helper.assertTrue(dripper.getZ() > startZ + 1.0, "the dripper flew towards its target");
		});
	}

	@GameTest(maxTicks = 40)
	public void itDoesNotLeapAtACloseTarget(GameTestHelper helper) {
		TestScenes.floor(helper);
		DripperEntity dripper = facingSouth(helper.spawnWithNoFreeWill(DripperModule.DRIPPER, new BlockPos(3, 1, 2)));
		Pig target = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 1, 5));
		dripper.setTarget(target);
		setField(dripper, "rollTimer", 1);
		double startY = dripper.getY();
		double[] peak = {startY};
		helper.onEachTick(() -> peak[0] = Math.max(peak[0], dripper.getY()));
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(peak[0] < startY + 0.1, "the dripper stayed on the ground");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void aCrouchingSlashklubHitReleasesASlashAndCostsThreeMoreDurability(GameTestHelper helper) {
		TestScenes.floor(helper);
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(3, 1, 4));
		ServerPlayer player = playerWithClub(helper);
		player.setShiftKeyDown(true);
		player.attack(zombie);
		ItemStack club = player.getMainHandItem();
		helper.assertValueEqual(club.getDamageValue(), 4, "durability spent on a crouching hit");
		helper.assertTrue(player.getCooldowns().isOnCooldown(club), "the club cools down");
		helper.assertValueEqual(helper.getEntities(DripperModule.SLASH_EFFECT).size(), 1, "slashes released");
		BuiltInRegistries.MOB_EFFECT.get(MightshroomIds.Effects.TREMBLE).ifPresent(tremble ->
			helper.assertTrue(zombie.hasEffect(tremble), "the victim trembles"));
		helper.succeed();
	}

	@GameTest
	public void aStandingSlashklubHitIsAPlainHit(GameTestHelper helper) {
		TestScenes.floor(helper);
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(3, 1, 4));
		ServerPlayer player = playerWithClub(helper);
		player.attack(zombie);
		helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 1, "durability spent on a plain hit");
		helper.assertFalse(player.getCooldowns().isOnCooldown(player.getMainHandItem()), "cooldown after a plain hit");
		helper.assertEntityNotPresent(DripperModule.SLASH_EFFECT);
		helper.succeed();
	}

	@GameTest
	public void aCrouchingHitDuringTheCooldownIsAPlainHit(GameTestHelper helper) {
		TestScenes.floor(helper);
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(3, 1, 4));
		ServerPlayer player = playerWithClub(helper);
		player.setShiftKeyDown(true);
		player.getCooldowns().addCooldown(player.getMainHandItem(), 100);
		player.attack(zombie);
		helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 1, "durability spent while cooling down");
		helper.assertEntityNotPresent(DripperModule.SLASH_EFFECT);
		helper.succeed();
	}

	@GameTest
	public void theSlashklubIsRepairedWithDripperRemains(GameTestHelper helper) {
		ItemStack club = new ItemStack(DripperModule.SLASHKLUB);
		helper.assertTrue(club.isValidRepairItem(new ItemStack(DripperModule.DRIPPER_REMAINS)), "dripper remains repair it");
		helper.assertValueEqual(club.getMaxDamage(), 200, "durability");
		helper.succeed();
	}

	@GameTest(maxTicks = 60)
	public void aSlashHurtsCreaturesNearItAndFadesAfterOneAndAHalfSeconds(GameTestHelper helper) {
		TestScenes.floor(helper);
		SlashEffectEntity slash = helper.spawn(DripperModule.SLASH_EFFECT, new BlockPos(1, 1, 3));
		slash.setYRot(-90.0F);
		slash.setYHeadRot(-90.0F);
		slash.setYBodyRot(-90.0F);
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(3, 1, 3));
		helper.runAfterDelay(5, () -> {
			helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "the zombie beside the slash is hurt");
			helper.assertTrue(slash.isAlive(), "the slash is still gliding");
		});
		helper.runAfterDelay(25, () -> helper.assertTrue(slash.isAlive(), "the slash lasts 30 ticks"));
		helper.runAfterDelay(35, () -> {
			helper.assertTrue(slash.isRemoved(), "the slash has faded");
			helper.succeed();
		});
	}

	@GameTest
	public void aWallMaskHangsOnAWallWithARandomFace(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Set<Integer> faces = new HashSet<>();
		for (int x = 0; x < 8; x++) {
			// The wall is at z 5; the player faces south into it and clicks its north side.
			helper.setBlock(new BlockPos(x, 1, 5), Blocks.STONE);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(DripperModule.DRIPSTONE_WALL_MASK));
			TestScenes.useItemOn(helper, player, new BlockPos(x, 1, 5), Direction.NORTH);
			BlockState mask = helper.getBlockState(new BlockPos(x, 1, 4));
			helper.assertTrue(mask.is(DripperModule.DRIPSTONE_WALL_MASK), "a mask is placed against the wall at x " + x);
			int face = mask.getValue(DripstoneWallMaskBlock.FACE);
			helper.assertTrue(face >= 1 && face <= 10, "mask face 1-10, was " + face);
			faces.add(face);
		}
		helper.assertTrue(faces.size() > 1, "eight masks show more than one face");
		helper.succeed();
	}

	@GameTest
	public void aWallMaskNeedsTheBlockBehindIt(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos wall = new BlockPos(3, 1, 5);
		BlockPos maskPos = new BlockPos(3, 1, 4);
		helper.setBlock(wall, Blocks.STONE);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(DripperModule.DRIPSTONE_WALL_MASK));
		TestScenes.useItemOn(helper, player, wall, Direction.NORTH);
		helper.assertBlockPresent(DripperModule.DRIPSTONE_WALL_MASK, maskPos);
		helper.setBlock(wall, Blocks.AIR);
		helper.assertBlockNotPresent(DripperModule.DRIPSTONE_WALL_MASK, maskPos);
		helper.succeed();
	}

	private static DripperEntity facingSouth(DripperEntity dripper) {
		dripper.setYRot(0.0F);
		dripper.setYHeadRot(0.0F);
		dripper.setYBodyRot(0.0F);
		dripper.setXRot(0.0F);
		return dripper;
	}

	/** A survival player standing at (3, 1, 3), facing south, holding a fresh Slashklub. */
	private static ServerPlayer playerWithClub(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		// A dummy connection: the club's cooldown is sent to the player's client.
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player,
			CommonListenerCookie.createInitial(player.getGameProfile(), false));
		Vec3 stand = helper.absoluteVec(new Vec3(3.5, 1, 3.5));
		player.snapTo(stand.x, stand.y, stand.z, 0.0F, 0.0F);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(DripperModule.SLASHKLUB));
		return player;
	}

	private static void setField(Object target, String name, int value) {
		try {
			Field field = target.getClass().getDeclaredField(name);
			field.setAccessible(true);
			field.setInt(target, value);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}
}
