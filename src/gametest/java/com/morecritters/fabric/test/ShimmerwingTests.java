package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.ShimmerwingIds;
import com.morecritters.fabric.module.shimmerwing.ShimmerwingEntity;
import com.morecritters.fabric.module.shimmerwing.ShimmerwingModule;
import com.morecritters.fabric.module.shimmerwing.ShimmerwormEntity;
import java.util.UUID;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.predicates.NbtPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/** The shimmerworm, the shimmerwing, the shimmering chrysalis and End's Blessing. */
public class ShimmerwingTests {
	@GameTest
	public void anEmptyHandPicksUpAShimmerworm(GameTestHelper helper) {
		TestScenes.floor(helper);
		ShimmerwormEntity worm = helper.spawn(ShimmerwingModule.SHIMMERWORM, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		worm.interact(player, InteractionHand.MAIN_HAND, worm.position());
		helper.assertTrue(worm.isRemoved(), "the worm is picked up");
		helper.assertValueEqual(player.getMainHandItem().getItem(), item(ShimmerwingIds.Items.SHIMMERWORM_ITEM), "item in hand");
		helper.succeed();
	}

	@GameTest
	public void aFullHandDoesNotPickUpAShimmerworm(GameTestHelper helper) {
		TestScenes.floor(helper);
		ShimmerwormEntity worm = helper.spawn(ShimmerwingModule.SHIMMERWORM, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		worm.interact(player, InteractionHand.MAIN_HAND, worm.position());
		helper.assertFalse(worm.isRemoved(), "the worm stays");
		helper.assertValueEqual(player.getMainHandItem().getItem(), Items.STICK, "item in hand");
		helper.succeed();
	}

	@GameTest
	public void theShimmerwormItemSetsTheWormDownAgainstTheClickedFace(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(ShimmerwingIds.Items.SHIMMERWORM_ITEM)));
		TestScenes.useItemOn(helper, player, new BlockPos(3, 1, 3), Direction.NORTH);
		helper.assertTrue(player.getMainHandItem().isEmpty(), "the worm leaves the hand");
		helper.assertEntityPresent(ShimmerwingModule.SHIMMERWORM, new BlockPos(3, 1, 2));
		helper.succeed();
	}

	@GameTest
	public void aShimmerwormGrowsIntoAShimmerwing(GameTestHelper helper) {
		TestScenes.floor(helper);
		ShimmerwormEntity worm = helper.spawn(ShimmerwingModule.SHIMMERWORM, new BlockPos(3, 1, 3));
		mergeData(worm, tag -> tag.putInt("TicksUntilWings", 5));
		helper.succeedWhen(() -> {
			helper.assertTrue(worm.isRemoved(), "the worm is gone");
			helper.assertEntityPresent(ShimmerwingModule.SHIMMERWING);
		});
	}

	@GameTest
	public void aWetShimmerwormIsHurtAndBlinksAway(GameTestHelper helper) {
		TestScenes.floor(helper);
		helper.setBlock(new BlockPos(3, 1, 3), Blocks.WATER);
		ShimmerwormEntity worm = helper.spawn(ShimmerwingModule.SHIMMERWORM, new BlockPos(3, 1, 3));
		Vec3 start = worm.position();
		helper.succeedWhen(() -> {
			helper.assertTrue(worm.getHealth() < worm.getMaxHealth(), "water hurts the worm");
			helper.assertTrue(worm.position().distanceTo(start) > 0.5, "the worm blinks away from the water");
		});
	}

	@GameTest
	public void brushingAShimmerwingShedsEndDustAndWearsTheBrush(GameTestHelper helper) {
		TestScenes.floor(helper);
		ShimmerwingEntity wing = helper.spawn(ShimmerwingModule.SHIMMERWING, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BRUSH));
		wing.interact(player, InteractionHand.MAIN_HAND, wing.position());
		helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 8, "brush wear");
		helper.assertItemEntityPresent(item(ShimmerwingIds.Items.END_DUST));
		helper.succeed();
	}

	@GameTest
	public void creativeBrushingDoesNotWearTheBrush(GameTestHelper helper) {
		TestScenes.floor(helper);
		ShimmerwingEntity wing = helper.spawn(ShimmerwingModule.SHIMMERWING, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		player.getAbilities().instabuild = true;
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BRUSH));
		wing.interact(player, InteractionHand.MAIN_HAND, wing.position());
		helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 0, "brush wear in creative");
		helper.assertItemEntityPresent(item(ShimmerwingIds.Items.END_DUST));
		helper.succeed();
	}

	@GameTest(maxTicks = 80)
	public void chorusFruitBlessesTheFeederThenTheShimmerwingBlinksAway(GameTestHelper helper) {
		TestScenes.floor(helper);
		ShimmerwingEntity wing = helper.spawn(ShimmerwingModule.SHIMMERWING, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CHORUS_FRUIT, 3));
		wing.interact(player, InteractionHand.MAIN_HAND, wing.position());
		MobEffectInstance blessing = player.getEffect(ShimmerwingModule.ENDS_BLESSING);
		helper.assertTrue(blessing != null, "the feeder gets End's Blessing");
		helper.assertValueEqual(blessing.getDuration(), 2400, "End's Blessing ticks");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "chorus fruit left");

		// While chewing it refuses another fruit.
		wing.interact(player, InteractionHand.MAIN_HAND, wing.position());
		helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "chorus fruit left after offering a second while it chews");

		Vec3[] beforeBlink = new Vec3[1];
		helper.startSequence()
			.thenIdle(30)
			.thenExecute(() -> beforeBlink[0] = wing.position())
			.thenWaitUntil(() -> helper.assertTrue(wing.position().distanceTo(beforeBlink[0]) > 0.5, "the shimmerwing blinks away after chewing"))
			.thenSucceed();
	}

	@GameTest
	public void endsBlessingRaisesJumpsAndSlowsFalls(GameTestHelper helper) {
		Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(3, 5, 3));
		pig.setNoGravity(false);
		pig.addEffect(new MobEffectInstance(ShimmerwingModule.ENDS_BLESSING, 200));
		helper.assertValueInBetween(0.999, pig.getAttributeValue(Attributes.JUMP_STRENGTH), 1.001, "jump strength while blessed (0.42 + 0.58)");
		helper.succeedWhen(() -> helper.assertTrue(pig.hasEffect(MobEffects.SLOW_FALLING), "an airborne blessed creature falls slowly"));
	}

	@GameTest
	public void aChrysalisFallsWithoutAChorusPlantBeside(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos chrysalis = new BlockPos(3, 1, 3);
		Block block = BuiltInRegistries.BLOCK.getValue(ShimmerwingIds.Blocks.SHIMMERING_CHRYSALIS);
		helper.assertFalse(block.defaultBlockState().canSurvive(helper.getLevel(), helper.absolutePos(chrysalis)), "a chrysalis cannot hang in the open");

		helper.setBlock(new BlockPos(4, 0, 3), Blocks.END_STONE);
		helper.setBlock(new BlockPos(4, 1, 3), Blocks.CHORUS_PLANT);
		helper.setBlock(chrysalis, block);
		helper.assertTrue(helper.getBlockState(chrysalis).canSurvive(helper.getLevel(), helper.absolutePos(chrysalis)), "a chrysalis hangs beside a chorus plant");
		helper.setBlock(new BlockPos(4, 1, 3), Blocks.AIR);
		helper.assertBlockPresent(Blocks.AIR, chrysalis);
		helper.succeed();
	}

	@GameTest
	public void aChrysalisHatchesAShimmerwormOnARandomTick(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos chrysalis = new BlockPos(3, 1, 3);
		helper.setBlock(new BlockPos(4, 0, 3), Blocks.END_STONE);
		helper.setBlock(new BlockPos(4, 1, 3), Blocks.CHORUS_PLANT);
		helper.setBlock(chrysalis, BuiltInRegistries.BLOCK.getValue(ShimmerwingIds.Blocks.SHIMMERING_CHRYSALIS));
		// One random tick in fifty hatches it; 2000 ticks miss every time with odds of about 1e-18.
		for (int i = 0; i < 2000 && !helper.getBlockState(chrysalis).isAir(); i++) {
			BlockState state = helper.getBlockState(chrysalis);
			state.randomTick(helper.getLevel(), helper.absolutePos(chrysalis), helper.getLevel().getRandom());
		}
		helper.assertBlockPresent(Blocks.AIR, chrysalis);
		helper.assertEntityPresent(ShimmerwingModule.SHIMMERWORM, chrysalis);
		helper.succeed();
	}

	/** Like {@code /data merge entity}: edits the entity's saved data and loads it back. */
	private static void mergeData(Entity entity, Consumer<CompoundTag> edit) {
		CompoundTag tag = NbtPredicate.getEntityTagToCompare(entity);
		edit.accept(tag);
		UUID uuid = entity.getUUID();
		entity.load(TagValueInput.create(ProblemReporter.DISCARDING, entity.registryAccess(), tag));
		entity.setUUID(uuid);
	}

	private static Item item(Identifier id) {
		return BuiltInRegistries.ITEM.getValue(id);
	}
}
