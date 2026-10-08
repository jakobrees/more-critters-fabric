package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.BouncelizardIds;
import com.morecritters.fabric.module.bouncelizard.BouncelizardEggBlock;
import com.morecritters.fabric.module.bouncelizard.BouncelizardEggBlockEntity;
import com.morecritters.fabric.module.bouncelizard.BouncelizardEntity;
import com.morecritters.fabric.module.bouncelizard.BouncelizardModule;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The bouncelizard: a trampoline that naps on bounceberries and wakes on milk, lays clutches of eggs that hatch,
 * and its bounceberry bushes and food.
 */
public class BouncelizardTests {
	private static final BlockPos AT = new BlockPos(3, 1, 3);

	@GameTest
	public void aBounceberryPutsItToSleep(GameTestHelper helper) {
		TestScenes.floor(helper);
		BouncelizardEntity lizard = helper.spawnWithNoFreeWill(BouncelizardModule.BOUNCELIZARD, AT);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(BouncelizardIds.Items.BOUNCEBERRY), 2));
		lizard.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertTrue(lizard.sleeping(), "asleep after a bounceberry");
		helper.assertValueEqual(lizard.textureName(), "bouncelizard0_sleep", "texture while asleep");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "berries left");
		helper.succeed();
	}

	@GameTest
	public void aMilkBucketWakesItAndIsEmptied(GameTestHelper helper) {
		TestScenes.floor(helper);
		BouncelizardEntity lizard = helper.spawnWithNoFreeWill(BouncelizardModule.BOUNCELIZARD, AT);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(BouncelizardIds.Items.BOUNCEBERRY), 2));
		lizard.mobInteract(player, InteractionHand.MAIN_HAND);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MILK_BUCKET));
		lizard.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertFalse(lizard.sleeping(), "asleep after milk");
		helper.assertValueEqual(lizard.textureName(), "bouncelizard0", "texture when awake");
		helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "an empty bucket is left in hand");
		helper.succeed();
	}

	@GameTest
	public void someNamesGiveASpecialSkin(GameTestHelper helper) {
		TestScenes.floor(helper);
		BouncelizardEntity lizard = helper.spawnWithNoFreeWill(BouncelizardModule.BOUNCELIZARD, AT);
		String[][] skins = {{"Skippy", "bouncelizard1"}, {"Dusty", "bouncelizard2"}, {"Waffle", "bouncelizard4"},
			{"Yarilis20", "bouncelizard9"}, {"Bob", "bouncelizard0"}};
		for (String[] skin : skins) {
			lizard.setCustomName(Component.literal(skin[0]));
			helper.assertValueEqual(lizard.textureName(), skin[1], "texture when named " + skin[0]);
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 60)
	public void somethingLandingOnItIsFlungUp(GameTestHelper helper) {
		assertBounce(helper, false, 2.0, 4.0);
	}

	@GameTest(maxTicks = 80)
	public void aSleepingOneFlingsItTwiceAsFast(GameTestHelper helper) {
		assertBounce(helper, true, 6.0, 12.0);
	}

	@GameTest
	public void itTakesNoFallDamage(GameTestHelper helper) {
		TestScenes.floor(helper);
		BouncelizardEntity lizard = helper.spawnWithNoFreeWill(BouncelizardModule.BOUNCELIZARD, AT);
		lizard.hurtServer(helper.getLevel(), helper.getLevel().damageSources().fall(), 5.0F);
		helper.assertValueEqual(lizard.getHealth(), lizard.getMaxHealth(), "health after a fall");
		helper.succeed();
	}

	@GameTest
	public void spiderEyesAreItsBreedingFood(GameTestHelper helper) {
		TestScenes.floor(helper);
		BouncelizardEntity lizard = helper.spawnWithNoFreeWill(BouncelizardModule.BOUNCELIZARD, AT);
		helper.assertTrue(lizard.isFood(new ItemStack(Items.SPIDER_EYE)), "spider eye is food");
		helper.assertFalse(lizard.isFood(new ItemStack(item(BouncelizardIds.Items.BOUNCEBERRY))), "bounceberry is food");
		helper.succeed();
	}

	@GameTest
	public void aBredBabyBecomesAClutchOfEggs(GameTestHelper helper) {
		TestScenes.floor(helper);
		BouncelizardEntity baby = helper.spawn(BouncelizardModule.BOUNCELIZARD, AT);
		baby.setAge(-24000);
		helper.succeedWhen(() -> {
			helper.assertTrue(baby.isRemoved(), "the bred baby is replaced");
			helper.assertBlockPresent(BouncelizardModule.EGG, AT);
			int state = helper.getBlockState(AT).getValue(BouncelizardEggBlock.EGGS);
			// State 0 is one egg; 2-4 are that many.
			helper.assertTrue(Set.of(0, 2, 3, 4).contains(state), "clutch state " + state);
		});
	}

	@GameTest(maxTicks = 60)
	public void aClutchHatchesIntoThatManyBabiesThatStayLizards(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(AT, BouncelizardModule.EGG.defaultBlockState().setValue(BouncelizardEggBlock.EGGS, 3));
		setHatchTimer(helper, AT, 3);
		helper.runAfterDelay(30, () -> {
			helper.assertBlockNotPresent(BouncelizardModule.EGG, AT);
			List<BouncelizardEntity> babies = helper.getEntities(BouncelizardModule.BOUNCELIZARD);
			helper.assertValueEqual(babies.size(), 3, "hatched bouncelizards");
			helper.assertTrue(babies.stream().allMatch(BouncelizardEntity::isBaby), "every hatchling is a baby");
			helper.succeed();
		});
	}

	@GameTest
	public void anEggAddedByHandGrowsTheClutchUpToFour(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(AT, BouncelizardModule.EGG.defaultBlockState());
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BouncelizardModule.EGG, 5));
		for (int state : new int[] {2, 3, 4}) {
			helper.useBlock(AT, player);
			helper.assertValueEqual(helper.getBlockState(AT).getValue(BouncelizardEggBlock.EGGS), state, "clutch state");
		}
		helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "eggs left after filling the clutch");
		// Deviation (NOTES.md): a full clutch takes no more; the egg is placed as a new clutch on the clicked face
		// instead of being used up for nothing as in the original.
		helper.useBlock(AT, player);
		helper.assertValueEqual(helper.getBlockState(AT).getValue(BouncelizardEggBlock.EGGS), 4, "a full clutch stays at four");
		helper.assertBlockPresent(BouncelizardModule.EGG, AT.north());
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "eggs left after placing beside a full clutch");
		helper.succeed();
	}

	@GameTest
	public void breakingAClutchTakesOneEggAndOnlySilkTouchKeepsIt(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(AT, BouncelizardModule.EGG.defaultBlockState().setValue(BouncelizardEggBlock.EGGS, 3));
		ServerPlayer player = survivalPlayer(helper);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE_PICKAXE));
		player.gameMode.destroyBlock(helper.absolutePos(AT));
		helper.assertValueEqual(helper.getBlockState(AT).getValue(BouncelizardEggBlock.EGGS), 2, "clutch state after breaking three");
		helper.assertValueEqual(count(helper, BouncelizardModule.EGG.asItem()), 0, "eggs dropped without silk touch");

		ItemStack silky = new ItemStack(Items.STONE_PICKAXE);
		silky.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
		player.setItemInHand(InteractionHand.MAIN_HAND, silky);
		player.gameMode.destroyBlock(helper.absolutePos(AT));
		helper.assertValueEqual(helper.getBlockState(AT).getValue(BouncelizardEggBlock.EGGS), 0, "clutch state after breaking two (one egg)");
		helper.assertValueEqual(count(helper, BouncelizardModule.EGG.asItem()), 1, "eggs dropped with silk touch");

		player.gameMode.destroyBlock(helper.absolutePos(AT));
		helper.assertBlockNotPresent(BouncelizardModule.EGG, AT);
		helper.succeed();
	}

	@GameTest
	public void pickingAFullBushGivesTwoBerriesAndEmptiesIt(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(AT, BouncelizardModule.BUSH);
		helper.useBlock(AT, helper.makeMockPlayer(GameType.SURVIVAL));
		helper.assertBlockPresent(BouncelizardModule.EMPTY_BUSH, AT);
		helper.assertValueEqual(count(helper, item(BouncelizardIds.Items.BOUNCEBERRY)), 2, "berries picked");
		helper.succeed();
	}

	@GameTest
	public void breakingAFullBushByHandGivesTwoBerriesAndAnEmptyBush(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(AT, BouncelizardModule.BUSH);
		survivalPlayer(helper).gameMode.destroyBlock(helper.absolutePos(AT));
		helper.assertBlockNotPresent(BouncelizardModule.BUSH, AT);
		helper.assertValueEqual(count(helper, item(BouncelizardIds.Items.BOUNCEBERRY)), 2, "berries dropped");
		helper.assertValueEqual(count(helper, BouncelizardModule.EMPTY_BUSH.asItem()), 1, "empty bushes dropped");
		helper.succeed();
	}

	@GameTest
	public void boneMealRefillsAnEmptyBush(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(AT, BouncelizardModule.EMPTY_BUSH);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL, 2));
		TestScenes.useItemOn(helper, player, AT, Direction.UP);
		helper.assertBlockPresent(BouncelizardModule.BUSH, AT);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "bone meal left");
		helper.succeed();
	}

	@GameTest
	public void bounceberryFoodGivesJumpBoost(GameTestHelper helper) {
		assertJumpBoost(helper, BouncelizardIds.Items.BOUNCEBERRY, 1);
		assertJumpBoost(helper, BouncelizardIds.Items.BOUNCEBERRY_SANDWICH, 2);
		Player player = assertJumpBoost(helper, BouncelizardIds.Items.BOUNCEBERRY_JAM, 2);
		helper.assertTrue(player.getMainHandItem().is(Items.GLASS_BOTTLE), "jam leaves a glass bottle");
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void leaperWearsOffInWaterUnderABlock(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos covered = new BlockPos(2, 1, 3);
		BlockPos open = new BlockPos(5, 1, 3);
		helper.setBlock(covered, Blocks.WATER);
		helper.setBlock(covered.above(), Blocks.STONE);
		helper.setBlock(open, Blocks.WATER);
		Cod underBlock = helper.spawnWithNoFreeWill(EntityTypes.COD, covered);
		Cod inOpenWater = helper.spawnWithNoFreeWill(EntityTypes.COD, open);
		Pig onLand = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 1, 6));
		helper.setBlock(new BlockPos(3, 2, 6), Blocks.STONE);
		var leaper = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(BouncelizardModule.LEAPER);
		underBlock.addEffect(new MobEffectInstance(leaper, 600));
		inOpenWater.addEffect(new MobEffectInstance(leaper, 600));
		onLand.addEffect(new MobEffectInstance(leaper, 600));
		helper.runAfterDelay(10, () -> {
			helper.assertFalse(underBlock.hasEffect(leaper), "leaper kept in water under a block");
			helper.assertTrue(inOpenWater.hasEffect(leaper), "leaper kept in open water");
			helper.assertTrue(onLand.hasEffect(leaper), "leaper kept on land under a block");
			helper.succeed();
		});
	}

	/** A pig dropped onto an adult bouncelizard: its highest point above the lizard must lie in the range. */
	private static void assertBounce(GameTestHelper helper, boolean asleep, double minRise, double maxRise) {
		TestScenes.floor(helper);
		BouncelizardEntity lizard = helper.spawnWithNoFreeWill(BouncelizardModule.BOUNCELIZARD, AT);
		if (asleep) {
			Player player = helper.makeMockPlayer(GameType.SURVIVAL);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(BouncelizardIds.Items.BOUNCEBERRY)));
			lizard.mobInteract(player, InteractionHand.MAIN_HAND);
		}
		var pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(3.5, 2.0, 3.5));
		double floor = lizard.getY();
		double[] peak = {pig.getY() - floor};
		helper.onEachTick(() -> peak[0] = Math.max(peak[0], pig.getY() - floor));
		helper.runAfterDelay(asleep ? 70 : 50, () -> {
			helper.assertTrue(peak[0] >= minRise && peak[0] <= maxRise,
				"highest point " + String.format("%.2f", peak[0]) + " should be within " + minRise + "-" + maxRise);
			helper.succeed();
		});
	}

	private static Player assertJumpBoost(GameTestHelper helper, Identifier food, int amplifier) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(food)));
		player.setItemInHand(InteractionHand.MAIN_HAND, player.getMainHandItem().finishUsingItem(helper.getLevel(), player));
		MobEffectInstance boost = player.getEffect(MobEffects.JUMP_BOOST);
		helper.assertTrue(boost != null, food.getPath() + " gives Jump Boost");
		helper.assertValueEqual(boost.getAmplifier(), amplifier, food.getPath() + " Jump Boost amplifier");
		helper.assertValueEqual(boost.getDuration(), 200, food.getPath() + " Jump Boost ticks");
		return player;
	}

	private static ServerPlayer survivalPlayer(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		Vec3 at = helper.absoluteVec(new Vec3(3.5, 1, 1.5));
		player.setPos(at.x, at.y, at.z);
		return player;
	}

	/** Fast-forwards the clutch: the countdown is private to the block entity. */
	private static void setHatchTimer(GameTestHelper helper, BlockPos pos, int ticks) {
		try {
			BouncelizardEggBlockEntity eggs = (BouncelizardEggBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
			Field timer = BouncelizardEggBlockEntity.class.getDeclaredField("hatchTimer");
			timer.setAccessible(true);
			timer.setInt(eggs, ticks);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static Item item(Identifier id) {
		return BuiltInRegistries.ITEM.getValue(id);
	}

	private static int count(GameTestHelper helper, Item item) {
		return helper.getEntities(EntityTypes.ITEM).stream()
			.filter(drop -> drop.getItem().is(item))
			.mapToInt(drop -> drop.getItem().getCount())
			.sum();
	}
}
