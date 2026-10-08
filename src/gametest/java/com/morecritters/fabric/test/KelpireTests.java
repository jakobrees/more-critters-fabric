package com.morecritters.fabric.test;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.ids.BlubberfishIds;
import com.morecritters.fabric.ids.NauticrawlIds;
import com.morecritters.fabric.module.kelpire.KelpireEntity;
import com.morecritters.fabric.module.kelpire.KelpireModule;
import com.morecritters.fabric.module.kelpire.KelpireRollsBlock;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** The kelpire: taming with raw blubberfish, sitting, healing, drying out, its looks, burping bubbles, and the rolls. */
public class KelpireTests {
	@GameTest
	public void rawBlubberfishTamesAWildKelpireWhichThenSitsAndGrantsTheAdvancement(GameTestHelper helper) {
		pool(helper);
		KelpireEntity kelpire = helper.spawn(KelpireModule.KELPIRE, new BlockPos(3, 1, 3));
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(rawBlubberfish(), 64));

		int fed = 0;
		while (!kelpire.isTame() && fed < 64) {
			kelpire.mobInteract(player, InteractionHand.MAIN_HAND);
			fed++;
		}

		helper.assertTrue(kelpire.isTame(), "tamed after " + fed + " fish (1 in 3 each)");
		helper.assertTrue(kelpire.isOwnedBy(player), "the feeder owns it");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 64 - fed, "fish left");
		// As in the original, the owner's taming click also toggles sitting.
		helper.assertTrue(kelpire.isOrderedToSit(), "sitting after the taming click");
		AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(MoreCritters.id("tame_kelpire"));
		helper.assertTrue(advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone(), "tame_kelpire awarded");
		helper.succeed();
	}

	@GameTest
	public void otherItemsNeitherTameNorAreUsedUp(GameTestHelper helper) {
		pool(helper);
		KelpireEntity kelpire = helper.spawn(KelpireModule.KELPIRE, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.COD, 64));
		for (int i = 0; i < 20; i++) {
			kelpire.mobInteract(player, InteractionHand.MAIN_HAND);
		}
		helper.assertFalse(kelpire.isTame(), "a kelpire fed cod stays wild");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 64, "cod left");
		helper.succeed();
	}

	@GameTest
	public void theOwnersRightClickTogglesSittingAndAStrangersDoesNot(GameTestHelper helper) {
		pool(helper);
		KelpireEntity kelpire = helper.spawn(KelpireModule.KELPIRE, new BlockPos(3, 1, 3));
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
		kelpire.tame(owner);

		kelpire.mobInteract(owner, InteractionHand.MAIN_HAND);
		helper.assertTrue(kelpire.isOrderedToSit(), "sits after the owner's click");
		kelpire.mobInteract(stranger, InteractionHand.MAIN_HAND);
		helper.assertTrue(kelpire.isOrderedToSit(), "still sitting after a stranger's click");
		kelpire.mobInteract(owner, InteractionHand.MAIN_HAND);
		helper.assertFalse(kelpire.isOrderedToSit(), "stands after the owner's second click");
		helper.succeed();
	}

	@GameTest
	public void feedingAHurtTameKelpireHealsItByTheFishsNutrition(GameTestHelper helper) {
		pool(helper);
		KelpireEntity kelpire = helper.spawn(KelpireModule.KELPIRE, new BlockPos(3, 1, 3));
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		kelpire.tame(owner);
		kelpire.setHealth(30.0F);
		owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(rawBlubberfish(), 5));

		kelpire.mobInteract(owner, InteractionHand.MAIN_HAND);

		int nutrition = new ItemStack(rawBlubberfish()).get(DataComponents.FOOD).nutrition();
		helper.assertValueEqual(kelpire.getHealth(), 30.0F + nutrition, "health after feeding");
		helper.assertValueEqual(owner.getMainHandItem().getCount(), 4, "fish left");
		helper.succeed();
	}

	@GameTest(maxTicks = 300)
	public void twoKelpiresFedRawBlubberfishBreedABaby(GameTestHelper helper) {
		pool(helper);
		KelpireEntity a = helper.spawn(KelpireModule.KELPIRE, new BlockPos(3, 1, 3));
		KelpireEntity b = helper.spawn(KelpireModule.KELPIRE, new BlockPos(4, 1, 3));
		for (KelpireEntity kelpire : new KelpireEntity[] {a, b}) {
			Player owner = helper.makeMockServerPlayer(GameType.SURVIVAL);
			kelpire.tame(owner);
			owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(rawBlubberfish()));
			kelpire.mobInteract(owner, InteractionHand.MAIN_HAND);
			helper.assertTrue(kelpire.isInLove(), "in love after raw blubberfish at full health");
			kelpire.mobInteract(owner, InteractionHand.MAIN_HAND); // the feeding click made it sit; stand it up again
		}
		helper.succeedWhen(() -> helper.assertTrue(
			helper.getEntities(KelpireModule.KELPIRE).stream().anyMatch(KelpireEntity::isBaby), "a baby kelpire is born"));
	}

	@GameTest(maxTicks = 60)
	public void outOfWaterAKelpireDriesOutOneHealthASecondOnceItsAirIsGone(GameTestHelper helper) {
		TestScenes.floor(helper);
		KelpireEntity kelpire = helper.spawnWithNoFreeWill(KelpireModule.KELPIRE, new BlockPos(3, 1, 3));
		setAir(kelpire, 3);
		helper.runAfterDelay(8, () -> helper.assertValueEqual(kelpire.getHealth(), 49.0F, "health once the air ran out"));
		helper.runAfterDelay(18, () -> helper.assertValueEqual(kelpire.getHealth(), 49.0F, "health half a second later"));
		helper.runAfterDelay(30, () -> {
			helper.assertValueEqual(kelpire.getHealth(), 48.0F, "health a second later");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 60)
	public void inWaterAKelpireDoesNotDryOut(GameTestHelper helper) {
		pool(helper);
		KelpireEntity kelpire = helper.spawnWithNoFreeWill(KelpireModule.KELPIRE, new BlockPos(3, 1, 3));
		setAir(kelpire, 3);
		helper.runAfterDelay(40, () -> {
			helper.assertValueEqual(kelpire.getHealth(), 50.0F, "health after two seconds in water");
			helper.succeed();
		});
	}

	@GameTest
	public void aKelpireIsNotHurtByDrowning(GameTestHelper helper) {
		TestScenes.floor(helper);
		KelpireEntity kelpire = helper.spawn(KelpireModule.KELPIRE, new BlockPos(3, 1, 3));
		kelpire.hurtServer(helper.getLevel(), helper.getLevel().damageSources().drown(), 5.0F);
		helper.assertValueEqual(kelpire.getHealth(), 50.0F, "health after drowning damage");
		helper.succeed();
	}

	@GameTest
	public void aWildKelpireTurnsInvisibleBesideKelpATameOneShowsItsTameLookAndCrustyIsAlwaysCrusty(GameTestHelper helper) {
		pool(helper);
		KelpireEntity wild = helper.spawnWithNoFreeWill(KelpireModule.KELPIRE, new BlockPos(1, 1, 1));
		helper.assertValueEqual(wild.textureName(), "kelpire", "wild, no kelp");
		helper.setBlock(new BlockPos(2, 1, 1), Blocks.KELP_PLANT);
		helper.assertValueEqual(wild.textureName(), "kelpire_invisible", "wild, kelp to the east");

		KelpireEntity tame = helper.spawnWithNoFreeWill(KelpireModule.KELPIRE, new BlockPos(5, 1, 5));
		tame.tame(helper.makeMockPlayer(GameType.SURVIVAL));
		helper.setBlock(new BlockPos(6, 1, 5), Blocks.KELP_PLANT);
		helper.assertValueEqual(tame.textureName(), "kelpire_tamed", "tame, beside kelp");

		wild.setCustomName(Component.literal("Crusty"));
		tame.setCustomName(Component.literal("crusty"));
		helper.assertValueEqual(wild.textureName(), "kelpire_crusty", "wild Crusty beside kelp");
		helper.assertValueEqual(tame.textureName(), "kelpire_crusty", "tame crusty");
		helper.succeed();
	}

	@GameTest(maxTicks = 80)
	public void aTameKelpireThatKillsBigPreyInWaterBurpsUpTwoOrThreeBubbles(GameTestHelper helper) {
		killAndCountBubbles(helper, EntityTypes.ZOMBIE, 2, 3);
	}

	@GameTest(maxTicks = 80)
	public void aTameKelpireThatKillsSmallPreyInWaterBurpsUpOneBubble(GameTestHelper helper) {
		killAndCountBubbles(helper, EntityTypes.COD, 1, 1);
	}

	@GameTest(maxTicks = 60)
	public void aWildKelpireDoesNotBurp(GameTestHelper helper) {
		pool(helper);
		KelpireEntity kelpire = helper.spawnWithNoFreeWill(KelpireModule.KELPIRE, new BlockPos(3, 1, 3));
		LivingEntity victim = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(5, 1, 3));
		victim.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(kelpire), 1000.0F);
		helper.runAfterDelay(40, () -> {
			helper.assertEntityNotPresent(bubbleType());
			helper.succeed();
		});
	}

	@GameTest
	public void kelpireRollsGiveFivePiecesOneRollAtATime(GameTestHelper helper) {
		TestScenes.floor(helper);
		BlockPos pos = new BlockPos(3, 1, 3);
		helper.setBlock(pos, KelpireModule.ROLLS);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		int[] expectedStates = {2, 3, 4, 5};
		for (int i = 0; i < 4; i++) {
			helper.useBlock(pos, player);
			helper.assertValueEqual(helper.getBlockState(pos).getValue(KelpireRollsBlock.BITES), expectedStates[i], "plate state after click " + (i + 1));
			helper.assertValueEqual(player.getInventory().countItem(KelpireModule.ROLL_PIECE), i + 1, "pieces after click " + (i + 1));
		}
		helper.useBlock(pos, player);
		helper.assertBlockNotPresent(KelpireModule.ROLLS, pos);
		helper.assertValueEqual(player.getInventory().countItem(KelpireModule.ROLL_PIECE), 5, "pieces after the last click");
		helper.succeed();
	}

	// --- helpers --------------------------------------------------------------------------

	private static void killAndCountBubbles(GameTestHelper helper, EntityType<? extends net.minecraft.world.entity.Mob> preyType, int min, int max) {
		pool(helper);
		KelpireEntity kelpire = helper.spawnWithNoFreeWill(KelpireModule.KELPIRE, new BlockPos(3, 1, 3));
		kelpire.tame(helper.makeMockPlayer(GameType.SURVIVAL));
		net.minecraft.world.entity.Mob victim = helper.spawnWithNoFreeWill(preyType, new BlockPos(5, 1, 3));
		helper.runAfterDelay(2, () -> {
			victim.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(kelpire), 1000.0F);
			helper.assertTrue(victim.isDeadOrDying(), "the prey is killed");
		});
		helper.runAfterDelay(12, () -> helper.assertEntityNotPresent(bubbleType()));
		helper.succeedWhen(() -> {
			int bubbles = helper.getEntities(bubbleType()).size();
			helper.assertTrue(bubbles >= min && bubbles <= max, "bubbles: " + bubbles + ", expected " + min + " to " + max);
		});
	}

	private static EntityType<?> bubbleType() {
		return BuiltInRegistries.ENTITY_TYPE.getValue(NauticrawlIds.Entities.BUBBLE_ENTITY);
	}

	private static Item rawBlubberfish() {
		return BuiltInRegistries.ITEM.getValue(BlubberfishIds.Items.RAW_BLUBBERFISH);
	}

	private static void pool(GameTestHelper helper) {
		TestScenes.floor(helper);
		for (int x = 0; x < 8; x++) {
			for (int y = 1; y <= 4; y++) {
				for (int z = 0; z < 8; z++) {
					helper.setBlock(x, y, z, Blocks.WATER);
				}
			}
		}
	}

	private static void setAir(Entity entity, int air) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
		entity.saveWithoutId(output);
		CompoundTag tag = output.buildResult();
		tag.putInt("Air", air);
		entity.load(TagValueInput.create(ProblemReporter.DISCARDING, entity.registryAccess(), tag));
	}
}
