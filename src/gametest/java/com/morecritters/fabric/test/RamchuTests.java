package com.morecritters.fabric.test;

import com.mojang.authlib.GameProfile;
import com.morecritters.fabric.ids.CorpseGearIds;
import com.morecritters.fabric.ids.RamchuIds;
import com.morecritters.fabric.module.ramchu.RamchuEntity;
import com.morecritters.fabric.module.ramchu.RamchuEntity.ShellState;
import com.morecritters.fabric.module.ramchu.RamchuFryEntity;
import com.morecritters.fabric.module.ramchu.RamchuModule;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.predicates.NbtPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/** The ramchu: milking, bucketing, fry, oiled up, and ramming. */
public class RamchuTests {
	@GameTest
	public void aGlassBottleMilksOilFromAShellLessRamchu(GameTestHelper helper) {
		fillWater(helper);
		RamchuEntity ramchu = ramchu(helper, new BlockPos(3, 1, 3), ShellState.NO_SHELL);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));
		ramchu.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(ramchu.shellState(), ShellState.NO_OIL, "shell state after milking");
		helper.assertValueEqual(ramchu.textureName(), "ramchu_noslime", "texture after milking");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "glass bottles left");
		helper.assertTrue(player.getInventory().contains(stack -> stack.is(item(RamchuIds.Items.RAMCHU_OIL_BOTTLE))), "the player gets a ramchu oil bottle");
		helper.succeed();
	}

	@GameTest
	public void aShelledRamchuCannotBeMilked(GameTestHelper helper) {
		fillWater(helper);
		RamchuEntity ramchu = helper.spawn(RamchuModule.RAMCHU, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));
		ramchu.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(ramchu.shellState(), ShellState.SHELLED, "shell state");
		helper.assertValueEqual(ramchu.textureName(), "ramchu", "texture");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "glass bottles left");
		helper.succeed();
	}

	@GameTest
	public void milkedOilGrowsBack(GameTestHelper helper) {
		fillWater(helper);
		RamchuEntity ramchu = ramchu(helper, new BlockPos(3, 1, 3), ShellState.NO_OIL);
		mergeData(ramchu, tag -> tag.putInt("OilRegrowTicks", 5));
		helper.succeedWhen(() -> helper.assertValueEqual(ramchu.shellState(), ShellState.NO_SHELL, "shell state once the oil is back"));
	}

	@GameTest
	public void aWaterBucketScoopsEachShellStateIntoItsBucket(GameTestHelper helper) {
		fillWater(helper);
		scoop(helper, ShellState.SHELLED, RamchuIds.Items.RAMCHU_BUCKET_BUCKET);
		scoop(helper, ShellState.NO_SHELL, RamchuIds.Items.RAMCHU_BUCKET_NO_SHELL_BUCKET);
		scoop(helper, ShellState.NO_OIL, RamchuIds.Items.RAMCHU_BUCKET_NO_OIL_BUCKET);
		helper.succeed();
	}

	@GameTest
	public void theNoOilBucketReleasesANoOilRamchu(GameTestHelper helper) {
		fillWater(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack bucket = new ItemStack(item(RamchuIds.Items.RAMCHU_BUCKET_NO_OIL_BUCKET));
		((MobBucketItem) bucket.getItem()).checkExtraContent(player, helper.getLevel(), bucket, helper.absolutePos(new BlockPos(3, 1, 3)));
		helper.assertEntityPresent(RamchuModule.RAMCHU);
		RamchuEntity released = helper.getEntities(RamchuModule.RAMCHU).getFirst();
		helper.assertValueEqual(released.shellState(), ShellState.NO_OIL, "shell state of the released ramchu");
		helper.assertTrue(released.fromBucket(), "a released ramchu is marked as from a bucket (never despawns)");
		helper.succeed();
	}

	@GameTest
	public void aWaterBucketScoopsAFryIntoTheFryBucket(GameTestHelper helper) {
		fillWater(helper);
		RamchuFryEntity fry = helper.spawn(RamchuModule.RAMCHU_FRY, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
		fry.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertTrue(fry.isRemoved(), "the fry is scooped up");
		helper.assertValueEqual(player.getMainHandItem().getItem(), item(RamchuIds.Items.RAMCHU_FRY_BUCKET_BUCKET), "item in hand");
		helper.succeed();
	}

	@GameTest
	public void aBabyRamchuBecomesAFry(GameTestHelper helper) {
		fillWater(helper);
		RamchuEntity baby = helper.spawn(RamchuModule.RAMCHU, new BlockPos(3, 1, 3));
		baby.setAge(-24000);
		helper.succeedWhen(() -> {
			helper.assertTrue(baby.isRemoved(), "the baby ramchu is gone");
			helper.assertEntityPresent(RamchuModule.RAMCHU_FRY);
		});
	}

	@GameTest
	public void aFryGrowsIntoARamchu(GameTestHelper helper) {
		fillWater(helper);
		RamchuFryEntity fry = helper.spawn(RamchuModule.RAMCHU_FRY, new BlockPos(3, 1, 3));
		mergeData(fry, tag -> tag.putInt("TicksUntilGrown", 5));
		helper.succeedWhen(() -> {
			helper.assertTrue(fry.isRemoved(), "the fry is gone");
			helper.assertEntityPresent(RamchuModule.RAMCHU);
		});
	}

	@GameTest
	public void ramchuOilGivesOiledUpAndLeavesTheBottle(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack oil = new ItemStack(item(RamchuIds.Items.RAMCHU_OIL_BOTTLE));
		player.setItemInHand(InteractionHand.MAIN_HAND, oil);
		ItemStack left = oil.finishUsingItem(helper.getLevel(), player);
		MobEffectInstance oiled = player.getEffect(RamchuModule.OILED_UP);
		helper.assertTrue(oiled != null, "drinking ramchu oil oils you up");
		helper.assertValueEqual(oiled.getDuration(), 6000, "oiled up ticks");
		helper.assertValueEqual(left.getItem(), Items.GLASS_BOTTLE, "what is left after drinking");
		helper.succeed();
	}

	@GameTest
	public void oiledUpMakesAMobsHitSlipOffAndCostsTenSeconds(GameTestHelper helper) {
		TestScenes.floor(helper);
		Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(2, 1, 2));
		Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(5, 1, 5));
		pig.addEffect(new MobEffectInstance(RamchuModule.OILED_UP, 1000));
		pig.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(zombie), 4.0F);
		helper.assertValueEqual(pig.getHealth(), pig.getMaxHealth(), "health after a slipped hit");
		helper.assertValueEqual(pig.getEffect(RamchuModule.OILED_UP).getDuration(), 800, "oiled up ticks after a slip");

		pig.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 4.0F);
		helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "damage with no attacker still hurts");
		helper.succeed();
	}

	@GameTest
	public void aRammingRamchuBustsItsShellOnAWall(GameTestHelper helper) {
		fillWater(helper);
		helper.setBlock(new BlockPos(4, 1, 3), Blocks.STONE);
		helper.setBlock(new BlockPos(4, 2, 3), Blocks.STONE);
		RamchuEntity ramchu = helper.spawn(RamchuModule.RAMCHU, new BlockPos(3, 1, 3));
		ramchu.setTarget(helper.spawn(EntityTypes.COD, new BlockPos(1, 2, 1)));
		helper.succeedWhen(() -> {
			helper.assertValueEqual(ramchu.shellState(), ShellState.NO_SHELL, "shell state after hitting the wall");
			helper.assertTrue(ramchu.hasEffect(BuiltInRegistries.MOB_EFFECT.get(CorpseGearIds.Effects.STUNNED).orElseThrow()), "the ramchu is stunned");
		});
	}

	@GameTest
	public void aRamchuWithoutATargetLeavesItsShellOnAWall(GameTestHelper helper) {
		fillWater(helper);
		helper.setBlock(new BlockPos(4, 1, 3), Blocks.STONE);
		helper.setBlock(new BlockPos(4, 2, 3), Blocks.STONE);
		RamchuEntity ramchu = helper.spawn(RamchuModule.RAMCHU, new BlockPos(3, 1, 3));
		helper.runAfterDelay(10, () -> {
			helper.assertValueEqual(ramchu.shellState(), ShellState.SHELLED, "shell state of a calm ramchu by a wall");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 160)
	public void aShelledRamchuChargesASwimmingSurvivalPlayer(GameTestHelper helper) {
		fillWater(helper);
		RamchuEntity ramchu = helper.spawn(RamchuModule.RAMCHU, new BlockPos(1, 1, 1));
		ServerPlayer player = joinPlayer(helper, GameType.SURVIVAL, new Vec3(5.5, 1.0, 5.5));
		// A test player has no client connection driving its living tick, so tick it here (it notices the water).
		helper.onEachTick(player::doTick);
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(ramchu.getTarget() == player, "the ramchu targets the swimming player"))
			.thenExecute(() -> helper.getLevel().getServer().getPlayerList().remove(player))
			.thenSucceed();
	}

	@GameTest
	public void drowningDoesNotHurtARamchu(GameTestHelper helper) {
		fillWater(helper);
		RamchuEntity ramchu = helper.spawn(RamchuModule.RAMCHU, new BlockPos(3, 1, 3));
		ramchu.hurtServer(helper.getLevel(), helper.getLevel().damageSources().drown(), 5.0F);
		helper.assertValueEqual(ramchu.getHealth(), 20.0F, "health after drowning damage");
		helper.succeed();
	}

	private static void scoop(GameTestHelper helper, ShellState state, Identifier bucketId) {
		RamchuEntity ramchu = ramchu(helper, new BlockPos(3, 1, 3), state);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
		ramchu.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertTrue(ramchu.isRemoved(), "the " + state + " ramchu is scooped up");
		helper.assertValueEqual(player.getMainHandItem().getItem(), item(bucketId), "bucket for a " + state + " ramchu");
	}

	/** A ramchu in the given shell state, set the way a bucket sets it. */
	private static RamchuEntity ramchu(GameTestHelper helper, BlockPos pos, ShellState state) {
		RamchuEntity ramchu = helper.spawn(RamchuModule.RAMCHU, pos);
		CompoundTag tag = new CompoundTag();
		tag.putInt("Datastate", state.ordinal());
		ramchu.loadFromBucketTag(tag);
		return ramchu;
	}

	/** Like {@code /data merge entity}: edits the entity's saved data and loads it back. */
	private static void mergeData(Entity entity, Consumer<CompoundTag> edit) {
		CompoundTag tag = NbtPredicate.getEntityTagToCompare(entity);
		edit.accept(tag);
		UUID uuid = entity.getUUID();
		entity.load(TagValueInput.create(ProblemReporter.DISCARDING, entity.registryAccess(), tag));
		entity.setUUID(uuid);
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

	/** A real player joined to the server (target scans only see players in the level), standing at {@code pos}. */
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
}
