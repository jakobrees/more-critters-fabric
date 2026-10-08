package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.ShriekbatIds;
import com.morecritters.fabric.module.shriekbat.EchoEntity;
import com.morecritters.fabric.module.shriekbat.ShriekbatEntity;
import com.morecritters.fabric.module.shriekbat.ShriekbatModule;
import com.morecritters.fabric.module.shriekbat.ShriekbombProjectile;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** The shriekbat: test shrieks that catch moving players, the alarmed bat's shriek, the soup and the shriek bomb. */
public class ShriekbatTests {
	private static final BlockPos BAT = new BlockPos(1, 3, 1), SHRIEK = new BlockPos(5, 1, 5), PLAYER = new BlockPos(3, 1, 5);

	@GameTest(maxTicks = 80)
	public void aTestShriekFadesThroughThreeStagesAndVanishesAfterTwoSeconds(GameTestHelper helper) {
		TestScenes.floor(helper);
		EchoEntity shriek = helper.spawn(ShriekbatModule.TESTER_SHRIEK, SHRIEK);
		helper.startSequence()
			.thenExecuteAfter(15, () -> helper.assertValueEqual(shriek.textureName(), "tester_shriek1", "texture with ~25 ticks left"))
			.thenExecuteAfter(10, () -> helper.assertValueEqual(shriek.textureName(), "tester_shriek2", "texture with ~15 ticks left"))
			.thenExecuteAfter(10, () -> helper.assertValueEqual(shriek.textureName(), "tester_shriek3", "texture with ~5 ticks left"))
			.thenExecuteAfter(10, () -> helper.assertTrue(shriek.isRemoved(), "the test shriek is gone after 40 ticks"))
			.thenSucceed();
	}

	@GameTest(maxTicks = 40)
	public void aTestShriekSinksToTheFloor(GameTestHelper helper) {
		TestScenes.floor(helper);
		EchoEntity shriek = helper.spawn(ShriekbatModule.TESTER_SHRIEK, new BlockPos(3, 5, 3));
		helper.succeedWhen(() -> helper.assertValueEqual(helper.relativePos(shriek.blockPosition()).getY(), 1, "height of the test shriek"));
	}

	@GameTest(maxTicks = 80)
	public void anEchoHangsInPlaceForTwoSecondsThenVanishes(GameTestHelper helper) {
		TestScenes.floor(helper);
		EchoEntity echo = helper.spawn(ShriekbatModule.ECHO, new BlockPos(3, 1, 3));
		helper.startSequence()
			.thenExecuteAfter(30, () -> helper.assertFalse(echo.isRemoved(), "the echo is gone before 40 ticks"))
			.thenExecuteAfter(15, () -> helper.assertTrue(echo.isRemoved(), "the echo is gone after 40 ticks"))
			.thenSucceed();
	}

	@GameTest(maxTicks = 60)
	public void aSurvivalPlayerMovingNearATestShriekAlarmsTheBat(GameTestHelper helper) {
		TestScenes.floor(helper);
		ShriekbatEntity bat = helper.spawn(ShriekbatModule.SHRIEKBAT, BAT);
		helper.spawn(ShriekbatModule.TESTER_SHRIEK, SHRIEK);
		Player player = movingPlayerInWorld(helper, GameType.SURVIVAL, 45);
		helper.succeedWhen(() -> {
			helper.assertTrue(bat.isAlarmed(), "the bat is alarmed");
			player.discard();
		});
	}

	@GameTest(maxTicks = 60)
	public void aCreativePlayerMovingNearATestShriekGoesUnheard(GameTestHelper helper) {
		TestScenes.floor(helper);
		ShriekbatEntity bat = helper.spawn(ShriekbatModule.SHRIEKBAT, BAT);
		helper.spawn(ShriekbatModule.TESTER_SHRIEK, SHRIEK);
		Player player = movingPlayerInWorld(helper, GameType.CREATIVE, 45);
		helper.startSequence()
			.thenExecuteAfter(45, () -> {
				helper.assertFalse(bat.isAlarmed(), "the bat is alarmed by a creative player");
				player.discard();
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 60)
	public void aShriekResistantPlayerMovingNearATestShriekGoesUnheard(GameTestHelper helper) {
		TestScenes.floor(helper);
		ShriekbatEntity bat = helper.spawn(ShriekbatModule.SHRIEKBAT, BAT);
		helper.spawn(ShriekbatModule.TESTER_SHRIEK, SHRIEK);
		Player player = movingPlayerInWorld(helper, GameType.SURVIVAL, 45);
		player.addEffect(new MobEffectInstance(ShriekbatModule.SHRIEK_RESISTANCE, 600));
		helper.startSequence()
			.thenExecuteAfter(45, () -> {
				helper.assertFalse(bat.isAlarmed(), "the bat is alarmed by a shriek-resistant player");
				player.discard();
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 160)
	public void anAlarmedBatsShriekTurnsMonstersOnThePlayer(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.spawn(ShriekbatModule.SHRIEKBAT, BAT);
		helper.spawn(ShriekbatModule.TESTER_SHRIEK, SHRIEK);
		Creeper creeper = helper.spawnWithNoFreeWill(EntityTypes.CREEPER, new BlockPos(6, 1, 1));
		Player player = movingPlayerInWorld(helper, GameType.SURVIVAL, 45);
		// The first shriek comes when the bat's 100-tick shriek timer runs out.
		helper.succeedWhen(() -> {
			helper.assertTrue(creeper.getTarget() == player, "the creeper targets the player after the shriek");
			player.discard();
		});
	}

	@GameTest(maxTicks = 200)
	public void aHangingBatDropsThreeTestShrieksBelowItself(GameTestHelper helper) {
		TestScenes.floor(helper);
		for (int x = 2; x <= 4; x++) {
			for (int z = 2; z <= 4; z++) {
				helper.setBlock(x, 5, z, Blocks.STONE);
			}
		}
		helper.spawn(ShriekbatModule.SHRIEKBAT, new BlockPos(3, 3, 3));
		// The bat tests the air when its 100-tick timer runs out; the shrieks follow 42, 47 and 52 ticks later.
		helper.succeedWhen(() -> helper.assertValueEqual(helper.getEntities(ShriekbatModule.TESTER_SHRIEK).size(), 3, "test shrieks"));
	}

	@GameTest
	public void shriekbatSoupGivesHalfAnHourOfShriekResistanceAndLeavesABowl(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack soup = new ItemStack(BuiltInRegistries.ITEM.getValue(ShriekbatIds.Items.SHRIEKBAT_SOUP));
		ItemStack left = soup.finishUsingItem(helper.getLevel(), player);
		helper.assertTrue(left.is(Items.BOWL), "a bowl is left, got " + left);
		MobEffectInstance resistance = player.getEffect(ShriekbatModule.SHRIEK_RESISTANCE);
		helper.assertTrue(resistance != null, "the player has Shriek Resistance");
		helper.assertValueEqual(resistance.getDuration(), 36_000, "Shriek Resistance ticks");
		helper.succeed();
	}

	@GameTest
	public void throwingAShriekBombInSurvivalUsesItUp(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = throwBomb(helper, GameType.SURVIVAL);
		helper.assertTrue(player.getMainHandItem().isEmpty(), "the bomb is used up");
		helper.assertEntityPresent(ShriekbatModule.SHRIEKBOMB_PROJECTILE);
		// Off before it lands, so its lure does not reach the mobs of neighbouring tests.
		helper.getEntities(ShriekbatModule.SHRIEKBOMB_PROJECTILE).forEach(bomb -> bomb.discard());
		helper.succeed();
	}

	@GameTest
	public void throwingAShriekBombInCreativeKeepsItAndStartsAOneSecondCooldown(GameTestHelper helper) {
		TestScenes.floor(helper);
		// The original put the cooldown on the bomb's item after using it up; with a stack of one that is only
		// felt in creative, where the bomb stays in the hand.
		Player player = throwBomb(helper, GameType.CREATIVE);
		helper.assertTrue(player.getMainHandItem().is(ShriekbatModule.SHRIEK_BOMB), "the bomb stays in a creative hand");
		helper.assertTrue(player.getCooldowns().isOnCooldown(player.getMainHandItem()), "the shriek bomb is on cooldown");
		helper.assertEntityPresent(ShriekbatModule.SHRIEKBOMB_PROJECTILE);
		helper.getEntities(ShriekbatModule.SHRIEKBOMB_PROJECTILE).forEach(bomb -> bomb.discard());
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void aShriekBombLandingBurstsIntoALargeEchoAndLuresMobs(GameTestHelper helper) {
		TestScenes.floor(helper);
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(6, 1, 6));
		ShriekbatEntity bat = helper.spawnWithNoFreeWill(ShriekbatModule.SHRIEKBAT, new BlockPos(1, 1, 6));
		dropBomb(helper, new BlockPos(3, 4, 3));
		helper.succeedWhen(() -> {
			helper.assertEntityPresent(ShriekbatModule.LARGE_ECHO, new BlockPos(3, 1, 3), 1.5);
			BlockPos heading = pig.getNavigation().getTargetPos();
			helper.assertTrue(heading != null && heading.closerThan(helper.absolutePos(new BlockPos(3, 1, 3)), 2.0),
				"the pig heads for where the bomb landed, heading " + (heading == null ? null : helper.relativePos(heading)));
			helper.assertTrue(bat.getNavigation().getTargetPos() == null, "the shriekbat is not lured");
		});
	}

	@GameTest(maxTicks = 40)
	public void aShriekBombHittingAnEchoFizzlesAndLeavesEverythingElse(GameTestHelper helper) {
		TestScenes.floor(helper);
		EchoEntity echo = helper.spawn(ShriekbatModule.ECHO, new BlockPos(3, 1, 3));
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 1, 4));
		ShriekbombProjectile bomb = dropBomb(helper, new BlockPos(3, 4, 3));
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(bomb.isRemoved(), "the bomb is gone"))
			.thenExecuteAfter(2, () -> {
				helper.assertEntityNotPresent(ShriekbatModule.LARGE_ECHO);
				helper.assertFalse(echo.isRemoved(), "the echo was erased");
				helper.assertFalse(pig.isRemoved(), "the pig beside the echo was erased");
			})
			.thenSucceed();
	}

	/** A player of {@code mode} throws a shriek bomb (one in hand) northwards across the test area. */
	private static Player throwBomb(GameTestHelper helper, GameType mode) {
		Player player = helper.makeMockPlayer(mode);
		mode.updatePlayerAbilities(player.getAbilities());
		player.snapTo(helper.absoluteVec(new Vec3(3.5, 1.0, 1.5)), 0.0F, 0.0F);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ShriekbatModule.SHRIEK_BOMB));
		player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		return player;
	}

	/** A shriek bomb falling straight down from {@code pos}, thrown by a survival player. */
	private static ShriekbombProjectile dropBomb(GameTestHelper helper, BlockPos pos) {
		ShriekbombProjectile bomb = helper.spawn(ShriekbatModule.SHRIEKBOMB_PROJECTILE, pos);
		bomb.setOwner(helper.makeMockPlayer(GameType.SURVIVAL));
		bomb.setDeltaMovement(0.0, -1.0, 0.0);
		return bomb;
	}

	/**
	 * A player standing in the level (test shrieks and the bat's shriek look for players in the world),
	 * kept moving slowly for {@code ticks} ticks. Invisible, so the monsters of neighbouring tests ignore it;
	 * it is discarded when the test passes or just before it times out.
	 */
	private static Player movingPlayerInWorld(GameTestHelper helper, GameType mode, int ticks) {
		Player player = helper.makeMockPlayer(mode);
		mode.updatePlayerAbilities(player.getAbilities());
		player.snapTo(helper.absoluteVec(Vec3.atBottomCenterOf(PLAYER)), 0.0F, 0.0F);
		player.setInvisible(true);
		helper.getLevel().addFreshEntity(player);
		for (int tick = 1; tick <= ticks; tick++) {
			helper.runAfterDelay(tick, () -> player.setDeltaMovement(0.01, player.getDeltaMovement().y, 0.0));
		}
		helper.runBeforeTestEnd(player::discard);
		return player;
	}
}
