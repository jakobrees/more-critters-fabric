package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.CorpseGearIds;
import com.morecritters.fabric.ids.MightshroomIds;
import com.morecritters.fabric.ids.NightshroomIds;
import com.morecritters.fabric.module.mightshroom.EchoEntity;
import com.morecritters.fabric.module.mightshroom.MightshroomEntity;
import com.morecritters.fabric.module.mightshroom.MightshroomModule;
import com.morecritters.fabric.module.mightshroom.ShroomRaisable;
import com.morecritters.fabric.module.nightshroom.AncientSkeletonEntity;
import com.morecritters.fabric.module.nightshroom.NightshroomModule;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The mightshroom system: the raising ritual on an ancient skeleton, the mightshroom's scream and special
 * attacks, its zapping echo, the stews and ribs, the fungal staff, Gift of Life and Imminent Death, and the
 * vita / mori shrooms with their pots, huge caps and cap blocks.
 */
public class MightshroomTests {
	// --- The raising ritual -------------------------------------------------------------------------------

	@GameTest
	public void aStewPouredOnASkeletonTakesRootOnceAndGivesTheBowlBack(GameTestHelper helper) {
		TestScenes.floor(helper);
		AncientSkeletonEntity skeleton = helper.spawn(NightshroomModule.ANCIENT_SKELETON, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(MightshroomIds.Items.DEATH_STEW)));
		rightClick(player, skeleton);
		helper.assertValueEqual(skeleton.soup(), ShroomRaisable.Soup.DEATH, "soup after the stew of death");
		helper.assertValueEqual(skeleton.textureName(), "ancient_skeleton_mori", "skeleton texture after the stew of death");
		helper.assertTrue(player.getMainHandItem().is(Items.BOWL), "the bowl comes back");

		// A second stew finds no room.
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(MightshroomIds.Items.LIFE_STEW)));
		rightClick(player, skeleton);
		helper.assertValueEqual(skeleton.soup(), ShroomRaisable.Soup.DEATH, "soup after a second stew");
		helper.assertTrue(player.getMainHandItem().is(item(MightshroomIds.Items.LIFE_STEW)), "the second stew stays in hand");
		helper.succeed();
	}

	@GameTest(maxTicks = 160)
	public void aBareSkeletonGivenPurgatorialMixtureRisesAsAMightshroom(GameTestHelper helper) {
		raiseSkeleton(helper, null, MightshroomModule.MIGHTSHROOM);
	}

	@GameTest(maxTicks = 160)
	public void aSkeletonWithTheStewOfDeathRisesAsAFrightshroom(GameTestHelper helper) {
		raiseSkeleton(helper, MightshroomIds.Items.DEATH_STEW, NightshroomModule.FRIGHTSHROOM);
	}

	@GameTest(maxTicks = 160)
	public void aSkeletonWithTheStewOfLifeRisesAsANightshroom(GameTestHelper helper) {
		raiseSkeleton(helper, MightshroomIds.Items.LIFE_STEW, NightshroomModule.NIGHTSHROOM);
	}

	@GameTest
	public void aRisingSkeletonTakesNoSecondMixture(GameTestHelper helper) {
		TestScenes.floor(helper);
		AncientSkeletonEntity skeleton = helper.spawn(NightshroomModule.ANCIENT_SKELETON, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(CorpseGearIds.Items.PURGATORIAL_MIXTURE)));
		rightClick(player, skeleton);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(CorpseGearIds.Items.PURGATORIAL_MIXTURE)));
		rightClick(player, skeleton);
		helper.assertTrue(player.getMainHandItem().is(item(CorpseGearIds.Items.PURGATORIAL_MIXTURE)), "the second mixture stays in hand");
		helper.succeed();
	}

	/** Optional stew, then purgatorial mixture; 110 ticks later the skeleton is replaced by {@code risen}. */
	private static void raiseSkeleton(GameTestHelper helper, Identifier stew, EntityType<?> risen) {
		TestScenes.floor(helper);
		AncientSkeletonEntity skeleton = helper.spawn(NightshroomModule.ANCIENT_SKELETON, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		if (stew != null) {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(stew)));
			rightClick(player, skeleton);
		}
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(CorpseGearIds.Items.PURGATORIAL_MIXTURE)));
		rightClick(player, skeleton);
		helper.assertTrue(player.getMainHandItem().is(Items.GLASS_BOTTLE), "the bottle comes back");
		helper.assertTrue(skeleton.hasEffect(MightshroomModule.SPAWN_MIGHTSHROOM), "the skeleton starts rising");

		helper.runAtTickTime(90, () -> {
			helper.assertTrue(skeleton.isAlive(), "still bones 4.5 s in");
			helper.assertEntityNotPresent(risen);
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(skeleton.isRemoved(), "the skeleton is gone");
			helper.assertValueEqual(helper.getEntities(risen).size(), 1, "risen " + BuiltInRegistries.ENTITY_TYPE.getKey(risen).getPath());
		});
	}

	// --- The mightshroom and its echo ---------------------------------------------------------------------

	@GameTest(maxTicks = 40)
	public void itScreamsAtANewTargetAndTheTargetTrembles(GameTestHelper helper) {
		TestScenes.floor(helper);
		MightshroomEntity mightshroom = helper.spawn(MightshroomModule.MIGHTSHROOM, new BlockPos(1, 1, 1));
		Husk husk = helper.spawn(EntityTypes.HUSK, new BlockPos(6, 1, 6));
		husk.setNoAi(true);
		mightshroom.setTarget(husk);
		helper.runAtTickTime(5, () -> {
			helper.assertTrue(mightshroom.hasEffect(MobEffects.SLOWNESS), "rooted while it screams");
			helper.assertFalse(husk.hasEffect(MightshroomModule.TREMBLE), "the target trembles only after half a second");
		});
		helper.succeedWhen(() -> helper.assertTrue(husk.hasEffect(MightshroomModule.TREMBLE), "the target trembles"));
	}

	@GameTest(maxTicks = 100)
	public void inAFightItStompsUpFungalZombiesOrLeapsAndLeavesAnEcho(GameTestHelper helper) {
		TestScenes.floor(helper);
		MightshroomEntity mightshroom = helper.spawn(MightshroomModule.MIGHTSHROOM, new BlockPos(2, 1, 2));
		Husk husk = helper.spawn(EntityTypes.HUSK, new BlockPos(6, 1, 6));
		husk.setNoAi(true);
		mightshroom.setTarget(husk);
		setSavedInt(helper, mightshroom, "SpecialTimer", 3);
		// One roll in three stomps (fungal zombies burst from the stone at its corners), otherwise it leaps and crashes.
		helper.succeedWhen(() -> {
			int zombies = helper.getEntities(NightshroomModule.FUNGAL_ZOMBIE).size();
			int echoes = helper.getEntities(MightshroomModule.MIGHTSHROOM_ECHO).size();
			helper.assertTrue(zombies == 4 || echoes == 1, "four fungal zombies or one echo (were " + zombies + " and " + echoes + ")");
		});
	}

	@GameTest
	public void itTakesNoFallDamage(GameTestHelper helper) {
		TestScenes.floor(helper);
		MightshroomEntity mightshroom = helper.spawn(MightshroomModule.MIGHTSHROOM, new BlockPos(3, 1, 3));
		mightshroom.setNoAi(true);
		ServerLevel level = helper.getLevel();
		mightshroom.hurtServer(level, level.damageSources().fall(), 50.0F);
		helper.assertValueEqual(mightshroom.getHealth(), 200.0F, "health after a fall");
		mightshroom.hurtServer(level, level.damageSources().generic(), 50.0F);
		helper.assertValueEqual(mightshroom.getHealth(), 150.0F, "health after a generic blow");
		helper.succeed();
	}

	@GameTest(maxTicks = 60)
	public void itsEchoZapsCreaturesNearbyButNotMightshroomsAndFadesAfterTwoSeconds(GameTestHelper helper) {
		TestScenes.floor(helper);
		EchoEntity echo = helper.spawn(MightshroomModule.MIGHTSHROOM_ECHO, new BlockPos(3, 1, 3));
		Husk husk = helper.spawn(EntityTypes.HUSK, new BlockPos(5, 1, 3));
		husk.setNoAi(true);
		MightshroomEntity mightshroom = helper.spawn(MightshroomModule.MIGHTSHROOM, new BlockPos(1, 1, 3));
		mightshroom.setNoAi(true);
		helper.runAtTickTime(30, () -> helper.assertTrue(echo.isAlive(), "the echo lasts two seconds"));
		helper.succeedWhen(() -> {
			helper.assertTrue(echo.isRemoved(), "the echo fades away");
			helper.assertTrue(husk.getHealth() < husk.getMaxHealth(), "the husk was zapped");
			helper.assertValueEqual(mightshroom.getHealth(), 200.0F, "mightshroom health beside its echo");
		});
	}

	// --- Food ----------------------------------------------------------------------------------------------

	@GameTest
	public void eatingStewsAndRibsGivesTheirEffects(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack left = eat(helper, player, MightshroomIds.Items.DEATH_STEW);
		helper.assertTrue(left.is(Items.BOWL), "the stew of death leaves its bowl");
		assertEffect(helper, player, MightshroomModule.IMMINENT_DEATH, 100, 0, "stew of death");

		left = eat(helper, player, MightshroomIds.Items.LIFE_STEW);
		helper.assertTrue(left.is(Items.BOWL), "the stew of life leaves its bowl");
		assertEffect(helper, player, MightshroomModule.GIFT_OF_LIFE, 60, 0, "stew of life");

		eat(helper, player, MightshroomIds.Items.MIGHTSHROOM_RIBS);
		assertEffect(helper, player, MobEffects.POISON, 100, 1, "mightshroom ribs");
		helper.succeed();
	}

	// --- Effects -------------------------------------------------------------------------------------------

	@GameTest(maxTicks = 200)
	public void giftOfLifeHealsOverTime(GameTestHelper helper) {
		TestScenes.floor(helper);
		Husk husk = helper.spawn(EntityTypes.HUSK, new BlockPos(3, 1, 3));
		husk.setNoAi(true);
		husk.setHealth(5.0F);
		husk.addEffect(new MobEffectInstance(MightshroomModule.GIFT_OF_LIFE, 180));
		helper.succeedWhen(() -> helper.assertTrue(husk.getHealth() >= 6.0F, "the husk has been healed"));
	}

	@GameTest(maxTicks = 200)
	public void imminentDeathStrikesForThreeToSix(GameTestHelper helper) {
		TestScenes.floor(helper);
		Husk husk = helper.spawn(EntityTypes.HUSK, new BlockPos(3, 1, 3));
		husk.setNoAi(true);
		husk.addEffect(new MobEffectInstance(MightshroomModule.IMMINENT_DEATH, 160));
		helper.succeedWhen(() -> {
			float lost = husk.getMaxHealth() - husk.getHealth();
			helper.assertTrue(lost >= 3.0F, "the reaper has struck (lost " + lost + ")");
		});
	}

	// --- Fungal staff --------------------------------------------------------------------------------------

	@GameTest
	public void theStaffGivesItsUserTheGiftOfLife(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = playerAt(helper, GameType.SURVIVAL, new BlockPos(3, 1, 3));
		ItemStack staff = new ItemStack(item(MightshroomIds.Items.FUNGAL_STAFF));
		player.setItemInHand(InteractionHand.MAIN_HAND, staff);
		staff.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		assertEffect(helper, player, MightshroomModule.GIFT_OF_LIFE, 30, 0, "plain staff use");
		helper.assertTrue(player.getCooldowns().isOnCooldown(staff), "the staff cools down");
		helper.assertValueEqual(helper.getEntities(MightshroomModule.SMALL_HEAL_ECHO).size(), 1, "small heal echoes");
		helper.succeed();
	}

	@GameTest
	public void crouchingWithTheStaffHealsYourPetsButNotOthers(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = playerAt(helper, GameType.SURVIVAL, new BlockPos(3, 1, 3));
		Wolf pet = helper.spawn(EntityTypes.WOLF, new BlockPos(1, 1, 1));
		pet.tame(player);
		Wolf stray = helper.spawn(EntityTypes.WOLF, new BlockPos(6, 1, 6));
		ItemStack staff = new ItemStack(item(MightshroomIds.Items.FUNGAL_STAFF));
		player.setItemInHand(InteractionHand.MAIN_HAND, staff);
		player.setShiftKeyDown(true);
		staff.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		assertEffect(helper, pet, MightshroomModule.GIFT_OF_LIFE, 30, 0, "the pet's gift");
		helper.assertFalse(stray.hasEffect(MightshroomModule.GIFT_OF_LIFE), "a stray wolf gets the gift");
		helper.assertTrue(player.getCooldowns().isOnCooldown(staff), "the staff cools down");
		helper.assertValueEqual(helper.getEntities(MightshroomModule.HEAL_ECHO).size(), 1, "heal echoes (one per creature healed)");
		helper.succeed();
	}

	@GameTest
	public void aStaffHitBringsImminentDeathOrMoriRoots(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = playerAt(helper, GameType.SURVIVAL, new BlockPos(1, 1, 1));
		Husk husk = helper.spawn(EntityTypes.HUSK, new BlockPos(4, 1, 4));
		husk.setNoAi(true);
		ItemStack staff = new ItemStack(item(MightshroomIds.Items.FUNGAL_STAFF));
		player.setItemInHand(InteractionHand.MAIN_HAND, staff);

		staff.hurtEnemy(husk, player);
		MobEffectInstance doom = husk.getEffect(MightshroomModule.IMMINENT_DEATH);
		helper.assertTrue(doom != null, "the husk has Imminent Death");
		int roots = helper.getEntities(NightshroomModule.MORI_ROOTS).size();
		if (roots == 0) {
			helper.assertValueEqual(doom.getDuration(), 20, "Imminent Death from a plain hit");
		} else {
			helper.assertValueEqual(roots, 1, "mori roots");
			helper.assertValueEqual(doom.getDuration(), 40, "Imminent Death when the roots trap it");
		}
		helper.assertTrue(player.getCooldowns().isOnCooldown(staff), "the staff cools down after a hit");

		// On cooldown, a hit brings nothing more.
		husk.removeAllEffects();
		staff.hurtEnemy(husk, player);
		helper.assertFalse(husk.hasEffect(MightshroomModule.IMMINENT_DEATH), "a hit during the cooldown gives Imminent Death");
		helper.succeed();
	}

	// --- Shrooms, pots and caps ----------------------------------------------------------------------------

	@GameTest
	public void shroomsGrowOnlyOnDirtAndNeverBesideTheirOwnPot(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Block vita = block(MightshroomIds.Blocks.VITA_SHROOM);
		helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
		helper.setBlock(new BlockPos(3, 0, 3), Blocks.DIRT);
		helper.setBlock(new BlockPos(5, 0, 5), Blocks.GRASS_BLOCK);
		helper.setBlock(new BlockPos(5, 1, 6), block(MightshroomIds.Blocks.POT_MORI));
		helper.assertFalse(vita.defaultBlockState().canSurvive(level, helper.absolutePos(new BlockPos(1, 1, 1))), "a vita shroom survives on stone");
		helper.assertTrue(vita.defaultBlockState().canSurvive(level, helper.absolutePos(new BlockPos(3, 1, 3))), "a vita shroom survives on dirt");
		helper.assertTrue(vita.defaultBlockState().canSurvive(level, helper.absolutePos(new BlockPos(5, 1, 5))), "a vita shroom survives on grass beside a potted mori");
		// The 1.20.1 dirt tag also held mycelium, podzol, moss and mud.
		helper.setBlock(new BlockPos(1, 0, 6), Blocks.MYCELIUM);
		helper.assertTrue(vita.defaultBlockState().canSurvive(level, helper.absolutePos(new BlockPos(1, 1, 6))), "a vita shroom survives on mycelium");
		helper.setBlock(new BlockPos(5, 1, 6), block(MightshroomIds.Blocks.POT_VITA));
		helper.assertFalse(vita.defaultBlockState().canSurvive(level, helper.absolutePos(new BlockPos(5, 1, 5))), "a vita shroom survives beside a potted vita");
		helper.succeed();
	}

	@GameTest
	public void aShroomGoesIntoAFlowerPotAndAnEmptyHandTakesItOut(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos pot = new BlockPos(3, 1, 3);
		helper.setBlock(pot, Blocks.FLOWER_POT);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(MightshroomIds.Blocks.MORI_SHROOM), 2));
		BlockHitResult hit = hitOn(helper, pot, Direction.UP);

		UseBlockCallback.EVENT.invoker().interact(player, helper.getLevel(), InteractionHand.MAIN_HAND, hit);
		helper.assertBlockPresent(block(MightshroomIds.Blocks.POT_MORI), pot);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "shrooms left in hand");

		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.getBlockState(pot).useWithoutItem(helper.getLevel(), player, hit);
		helper.assertBlockPresent(Blocks.FLOWER_POT, pot);
		helper.assertTrue(player.getMainHandItem().is(item(MightshroomIds.Blocks.MORI_SHROOM)), "the shroom is back in hand");
		helper.succeed();
	}

	@GameTest
	public void boneMealGrowsAHugeShroomOnlyWithRoomAround(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(x, 0, z, Blocks.DIRT);
			}
		}
		Block vita = block(MightshroomIds.Blocks.VITA_SHROOM);
		Block cap = block(MightshroomIds.Blocks.VITA_SHROOM_BLOCK);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		BlockPos crowded = new BlockPos(6, 1, 6);
		helper.setBlock(crowded, vita);
		helper.setBlock(new BlockPos(7, 1, 6), Blocks.STONE);
		BlockPos open = new BlockPos(3, 1, 3);
		helper.setBlock(open, vita);

		for (int i = 0; i < 40; i++) {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL));
			TestScenes.useItemOn(helper, player, crowded, Direction.UP);
		}
		helper.assertBlockPresent(vita, crowded);
		helper.assertBlockPresent(Blocks.AIR, crowded.above());

		// One try in five grows the 5 x 7 x 5 huge shroom over the plant.
		boolean grown = false;
		for (int i = 0; i < 60 && !grown; i++) {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL));
			TestScenes.useItemOn(helper, player, open, Direction.UP);
			grown = countBlocks(helper, new BlockPos(1, 1, 1), new BlockPos(5, 7, 5), cap) > 0;
		}
		helper.assertTrue(grown, "a huge vita shroom grew within 60 bone meal");
		helper.succeed();
	}

	@GameTest
	public void aCapBlockDropsItselfWithSilkTouchAndAtMostTwoShroomsWithout(GameTestHelper helper) {
		TestScenes.floor(helper);
		ServerLevel level = helper.getLevel();
		Block cap = block(MightshroomIds.Blocks.MORI_SHROOM_BLOCK);
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);

		BlockPos silkAt = new BlockPos(1, 1, 1);
		ItemStack silkPick = new ItemStack(Items.DIAMOND_PICKAXE);
		silkPick.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
		breakBlock(helper, player, silkAt, cap, silkPick);
		helper.assertValueEqual(countItems(helper, silkAt, item(MightshroomIds.Blocks.MORI_SHROOM_BLOCK)), 1, "cap blocks from silk touch");
		helper.assertValueEqual(countItems(helper, silkAt, item(MightshroomIds.Blocks.MORI_SHROOM)), 0, "shrooms from silk touch");

		BlockPos plainAt = new BlockPos(6, 1, 6);
		for (int i = 0; i < 10; i++) {
			breakBlock(helper, player, plainAt, cap, ItemStack.EMPTY);
			int shrooms = countItems(helper, plainAt, item(MightshroomIds.Blocks.MORI_SHROOM));
			helper.assertTrue(shrooms <= 2, "at most two shrooms per cap block (got " + shrooms + ")");
			helper.assertValueEqual(countItems(helper, plainAt, item(MightshroomIds.Blocks.MORI_SHROOM_BLOCK)), 0, "cap blocks without silk touch");
			for (ItemEntity drop : itemsAround(helper, plainAt)) {
				drop.discard();
			}
		}
		helper.succeed();
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

	private static ItemStack eat(GameTestHelper helper, Player player, Identifier food) {
		return new ItemStack(item(food)).finishUsingItem(helper.getLevel(), player);
	}

	private static void assertEffect(GameTestHelper helper, LivingEntity entity, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
	                                 int duration, int amplifier, String what) {
		MobEffectInstance instance = entity.getEffect(effect);
		helper.assertTrue(instance != null, what + ": effect missing");
		helper.assertValueEqual(instance.getDuration(), duration, what + ": duration");
		helper.assertValueEqual(instance.getAmplifier(), amplifier, what + ": amplifier");
	}

	private static Player playerAt(GameTestHelper helper, GameType mode, BlockPos pos) {
		Player player = helper.makeMockPlayer(mode);
		Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		return player;
	}

	private static BlockHitResult hitOn(GameTestHelper helper, BlockPos pos, Direction face) {
		BlockPos absolute = helper.absolutePos(pos);
		return new BlockHitResult(Vec3.atCenterOf(absolute).relative(face, 0.5), face, absolute, false);
	}

	private static void breakBlock(GameTestHelper helper, ServerPlayer player, BlockPos pos, Block block, ItemStack tool) {
		helper.setBlock(pos, block);
		BlockPos absolute = helper.absolutePos(pos);
		BlockState state = helper.getBlockState(pos);
		helper.getLevel().removeBlock(absolute, false);
		block.playerDestroy(helper.getLevel(), player, absolute, state, null, tool);
	}

	private static java.util.List<ItemEntity> itemsAround(GameTestHelper helper, BlockPos pos) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(1.5));
	}

	private static int countItems(GameTestHelper helper, BlockPos pos, Item item) {
		return itemsAround(helper, pos).stream().filter(drop -> drop.getItem().is(item)).mapToInt(drop -> drop.getItem().getCount()).sum();
	}

	private static int countBlocks(GameTestHelper helper, BlockPos from, BlockPos to, Block block) {
		int count = 0;
		for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
			if (helper.getBlockState(pos).is(block)) {
				count++;
			}
		}
		return count;
	}

	/** Rewrites one saved field of an entity, for timers that are not otherwise reachable. */
	private static void setSavedInt(GameTestHelper helper, Entity entity, String key, int value) {
		var registries = helper.getLevel().registryAccess();
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		entity.saveWithoutId(output);
		CompoundTag tag = output.buildResult();
		tag.putInt(key, value);
		entity.load(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
	}

	private static Item item(Identifier id) {
		return BuiltInRegistries.ITEM.getValue(id);
	}

	private static Block block(Identifier id) {
		return BuiltInRegistries.BLOCK.getValue(id);
	}
}
