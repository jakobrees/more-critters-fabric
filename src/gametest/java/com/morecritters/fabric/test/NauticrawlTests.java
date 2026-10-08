package com.morecritters.fabric.test;

import com.mojang.authlib.GameProfile;
import com.morecritters.fabric.ids.NauticrawlIds;
import com.morecritters.fabric.module.nauticrawl.BubbleEntity;
import com.morecritters.fabric.module.nauticrawl.NauticrawlEntity;
import com.morecritters.fabric.module.nauticrawl.NauticrawlModule;
import com.morecritters.fabric.module.nauticrawl.NauticrawlRamenBlock;
import com.morecritters.fabric.module.nauticrawl.ZombieNauticrawlEntity;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** The nauticrawl, its zombie, the kelpire's blood bubbles, and the ramen, axe, helmet and doll. */
public class NauticrawlTests {
	@GameTest
	public void ramenIsEatenInThreeGoesThenTheShellIsCrunched(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos ramen = new BlockPos(3, 1, 3);
		helper.setBlock(ramen, NauticrawlModule.RAMEN);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.getFoodData().setFoodLevel(0);

		helper.useBlock(ramen, player);
		helper.assertBlockProperty(ramen, NauticrawlRamenBlock.STAGE, 2);
		helper.assertValueEqual(player.getFoodData().getFoodLevel(), 10, "food after the noodles");

		helper.useBlock(ramen, player);
		helper.assertBlockProperty(ramen, NauticrawlRamenBlock.STAGE, 3);
		helper.assertValueEqual(player.getFoodData().getFoodLevel(), 20, "food after the broth");

		helper.useBlock(ramen, player);
		helper.assertBlockPresent(Blocks.AIR, ramen);
		MobEffectInstance breathing = player.getEffect(MobEffects.WATER_BREATHING);
		helper.assertTrue(breathing != null, "crunching the shell gives water breathing");
		helper.assertValueEqual(breathing.getDuration(), 300, "water breathing ticks");
		helper.succeed();
	}

	@GameTest
	public void hittingABloodBubblePopsItAndHealsTheHitter(GameTestHelper helper) {
		fillWater(helper);
		BubbleEntity bubble = helper.spawn(NauticrawlModule.BUBBLE, new BlockPos(3, 2, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setHealth(10.0F);
		bubble.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 1.0F);
		helper.assertTrue(bubble.isRemoved(), "the bubble pops");
		helper.assertValueEqual(player.getHealth(), 12.0F, "hitter health");
		helper.succeed();
	}

	@GameTest
	public void aBloodBubbleOutOfWaterPops(GameTestHelper helper) {
		TestScenes.floor(helper);
		BubbleEntity bubble = helper.spawn(NauticrawlModule.BUBBLE, new BlockPos(3, 1, 3));
		helper.succeedWhen(() -> helper.assertTrue(bubble.isRemoved(), "a bubble on dry land pops"));
	}

	@GameTest
	public void aBloodBubbleInWaterStays(GameTestHelper helper) {
		fillWater(helper);
		BubbleEntity bubble = helper.spawn(NauticrawlModule.BUBBLE, new BlockPos(3, 2, 3));
		helper.runAfterDelay(20, () -> {
			helper.assertFalse(bubble.isRemoved(), "a bubble in water stays");
			helper.succeed();
		});
	}

	@GameTest
	public void theNauticalAxeHitsWetTargetsForFifteenMore(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(new BlockPos(1, 1, 1), Blocks.WATER);
		helper.setBlock(new BlockPos(1, 2, 1), Blocks.WATER);
		NauticrawlEntity wet = helper.spawn(NauticrawlModule.NAUTICRAWL, new BlockPos(1, 1, 1));
		NauticrawlEntity dry = helper.spawn(NauticrawlModule.NAUTICRAWL, new BlockPos(5, 1, 5));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.runAfterDelay(2, () -> {
			float wetBefore = wet.getHealth(), dryBefore = dry.getHealth();
			new ItemStack(item(NauticrawlIds.Items.NAUTICAL_AXE)).hurtEnemy(wet, player);
			new ItemStack(item(NauticrawlIds.Items.NAUTICAL_AXE)).hurtEnemy(dry, player);
			helper.assertValueEqual(wetBefore - wet.getHealth(), 15.0F, "bonus damage to a target in water");
			helper.assertValueEqual(dryBefore - dry.getHealth(), 0.0F, "bonus damage to a target on dry land");
			helper.succeed();
		});
	}

	@GameTest
	public void theSpawnDollSummonsAZombieNauticrawlAgainstTheClickedFace(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(NauticrawlIds.Items.ZOMBIE_NAUTICRAWL_SPAWN_DOLL), 2));
		TestScenes.useItemOn(helper, player, new BlockPos(3, 1, 3), Direction.EAST);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "dolls left");
		helper.assertEntityPresent(NauticrawlModule.ZOMBIE_NAUTICRAWL, new BlockPos(4, 1, 3));
		helper.succeed();
	}

	@GameTest
	public void theShellCracksAtHalfHealthAndArgonautHasItsOwnShell(GameTestHelper helper) {
		fillWater(helper);
		NauticrawlEntity nauticrawl = helper.spawn(NauticrawlModule.NAUTICRAWL, new BlockPos(3, 1, 3));
		helper.assertValueEqual(nauticrawl.textureName(), "nauticrawl", "texture at full health");
		nauticrawl.setHealth(25.0F);
		helper.assertValueEqual(nauticrawl.textureName(), "nauticrawl_cracked", "texture at half health");
		nauticrawl.setCustomName(Component.literal("Argonaut"));
		helper.assertValueEqual(nauticrawl.textureName(), "nauticrawl_argonaut_cracked", "cracked argonaut texture");
		nauticrawl.setHealth(50.0F);
		helper.assertValueEqual(nauticrawl.textureName(), "nauticrawl_argonaut", "argonaut texture");

		ZombieNauticrawlEntity zombie = helper.spawn(NauticrawlModule.ZOMBIE_NAUTICRAWL, new BlockPos(5, 1, 5));
		helper.assertValueEqual(zombie.textureName(), "nauticrawl_zombie", "zombie texture");
		zombie.setHealth(20.0F);
		helper.assertValueEqual(zombie.textureName(), "nauticrawl_zombie_cracked", "cracked zombie texture");
		helper.succeed();
	}

	@GameTest
	public void oneNauticrawlInTwentyRisesAsAZombie(GameTestHelper helper) {
		fillWater(helper);
		int zombies = 0;
		int rolls = 300;
		for (int i = 0; i < rolls; i++) {
			NauticrawlEntity nauticrawl = helper.spawn(NauticrawlModule.NAUTICRAWL, new BlockPos(3, 1, 3), EntitySpawnReason.NATURAL);
			if (nauticrawl.isRemoved()) zombies++;
			helper.getEntities(NauticrawlModule.NAUTICRAWL).forEach(Entity::discard);
			helper.getEntities(NauticrawlModule.ZOMBIE_NAUTICRAWL).forEach(Entity::discard);
		}
		// Expected 15 of 300; the bounds hold for practically every run.
		helper.assertValueInBetween(1, zombies, 45, "nauticrawls replaced by zombies out of " + rolls);
		helper.succeed();
	}

	@GameTest
	public void oneZombieNauticrawlInTenGrowsCoral(GameTestHelper helper) {
		fillWater(helper);
		int coral = 0;
		int rolls = 200;
		for (int i = 0; i < rolls; i++) {
			ZombieNauticrawlEntity zombie = helper.spawn(NauticrawlModule.ZOMBIE_NAUTICRAWL, new BlockPos(3, 1, 3), EntitySpawnReason.NATURAL);
			if (zombie.textureName().equals("nauticrawl_zombie_coral")) coral++;
			zombie.discard();
		}
		// Expected 20 of 200.
		helper.assertValueInBetween(1, coral, 60, "coral zombies out of " + rolls);
		helper.succeed();
	}

	@GameTest
	public void drowningDoesNotHurtANauticrawl(GameTestHelper helper) {
		fillWater(helper);
		NauticrawlEntity nauticrawl = helper.spawn(NauticrawlModule.NAUTICRAWL, new BlockPos(3, 1, 3));
		nauticrawl.hurtServer(helper.getLevel(), helper.getLevel().damageSources().drown(), 10.0F);
		helper.assertValueEqual(nauticrawl.getHealth(), 50.0F, "health after drowning damage");
		helper.succeed();
	}

	@GameTest(maxTicks = 300)
	public void aNauticrawlOnLandDriesOut(GameTestHelper helper) {
		TestScenes.floor(helper);
		NauticrawlEntity nauticrawl = helper.spawn(NauticrawlModule.NAUTICRAWL, new BlockPos(3, 1, 3));
		helper.runAfterDelay(150, () -> helper.assertValueEqual(nauticrawl.getHealth(), 50.0F, "health before its air runs out"));
		helper.succeedWhen(() -> helper.assertTrue(nauticrawl.getHealth() < 50.0F, "a nauticrawl on land takes dry-out damage"));
	}

	@GameTest
	public void killedItDropsShellPiecesAndTentacles(GameTestHelper helper) {
		TestScenes.floor(helper);
		NauticrawlEntity nauticrawl = helper.spawn(NauticrawlModule.NAUTICRAWL, new BlockPos(3, 1, 3));
		nauticrawl.kill(helper.getLevel());
		helper.succeedWhen(() -> {
			helper.assertItemEntityPresent(item(NauticrawlIds.Items.SHELL_PIECES));
			helper.assertItemEntityPresent(item(NauticrawlIds.Items.NAUTICRAWL_TENTACLE));
		});
	}

	@GameTest
	public void sneakingInTheNauticalHelmetGivesResistanceAndSlowness(GameTestHelper helper) {
		TestScenes.floor(helper);
		ServerPlayer player = joinPlayer(helper, GameType.SURVIVAL, new Vec3(3.5, 1.0, 3.5));
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(item(NauticrawlIds.Items.NAUTICAL_HELMET_HELMET)));
		player.setShiftKeyDown(true);
		helper.startSequence()
			.thenWaitUntil(() -> {
				MobEffectInstance resistance = player.getEffect(MobEffects.RESISTANCE);
				helper.assertTrue(resistance != null, "a sneaking wearer gets resistance");
				helper.assertValueEqual(resistance.getAmplifier(), 2, "resistance level (III)");
				helper.assertTrue(player.hasEffect(MobEffects.SLOWNESS), "a sneaking wearer gets slowness");
			})
			.thenExecute(() -> {
				player.setShiftKeyDown(false);
				player.removeAllEffects();
			})
			.thenIdle(3)
			.thenExecute(() -> {
				helper.assertFalse(player.hasEffect(MobEffects.RESISTANCE), "no resistance while standing");
				leave(helper, player);
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 100)
	public void aZombieNauticrawlHuntsSurvivalPlayers(GameTestHelper helper) {
		fillWater(helper);
		ZombieNauticrawlEntity zombie = helper.spawn(NauticrawlModule.ZOMBIE_NAUTICRAWL, new BlockPos(1, 1, 1));
		ServerPlayer player = joinPlayer(helper, GameType.SURVIVAL, new Vec3(6.5, 1.0, 6.5));
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(zombie.getTarget() == player, "the zombie nauticrawl targets the player"))
			.thenExecute(() -> leave(helper, player))
			.thenSucceed();
	}

	@GameTest
	public void aNauticrawlStruckByAMobFightsBack(GameTestHelper helper) {
		fillWater(helper);
		NauticrawlEntity nauticrawl = helper.spawn(NauticrawlModule.NAUTICRAWL, new BlockPos(1, 1, 1));
		NauticrawlEntity attacker = helper.spawn(NauticrawlModule.NAUTICRAWL, new BlockPos(5, 1, 5));
		// Struck after its first tick: the retaliation goal ignores a hit stamped at tick 0.
		helper.runAfterDelay(2, () -> nauticrawl.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(attacker), 1.0F));
		helper.succeedWhen(() -> helper.assertTrue(nauticrawl.getTarget() == attacker, "the struck nauticrawl targets its attacker"));
	}

	/** Water through the whole test area above a stone floor. */
	private static void fillWater(GameTestHelper helper) {
		TestScenes.floor(helper);
		for (int x = 0; x < 8; x++) {
			for (int y = 1; y < 7; y++) {
				for (int z = 0; z < 8; z++) {
					helper.setBlock(x, y, z, Blocks.WATER);
				}
			}
		}
	}

	private static Item item(Identifier id) {
		return BuiltInRegistries.ITEM.getValue(id);
	}

	/** A real player joined to the server (tick handlers and target goals only see listed players), standing at {@code pos}. */
	private static ServerPlayer joinPlayer(GameTestHelper helper, GameType mode, Vec3 pos) {
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-player"), false);
		ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(mode);
		Vec3 absolute = helper.absoluteVec(pos);
		player.teleportTo(absolute.x, absolute.y, absolute.z);
		return player;
	}

	private static void leave(GameTestHelper helper, ServerPlayer player) {
		helper.getLevel().getServer().getPlayerList().remove(player);
	}
}
