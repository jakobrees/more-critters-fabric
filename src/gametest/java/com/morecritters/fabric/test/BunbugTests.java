package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.BlubberfishIds;
import com.morecritters.fabric.ids.BunbugIds;
import com.morecritters.fabric.module.bunbug.BunbugEntity;
import com.morecritters.fabric.module.bunbug.BunbugModule;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** The bunbug: decorated by right-click, hatched from eggs, born as a litter. */
public class BunbugTests {
	@GameTest
	public void decoratingInOrder(GameTestHelper helper) {
		TestScenes.floor(helper);
		BunbugEntity bunbug = helper.spawn(BunbugModule.BUNBUG, new BlockPos(3, 1, 3));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);

		// Berries are refused until the bunbug is iced and sprinkled.
		feed(player, bunbug, new ItemStack(Items.SWEET_BERRIES, 4));
		helper.assertValueEqual(bunbug.textureName(), "bunbug_0_0_0", "texture after berries on a plain bunbug");

		feed(player, bunbug, new ItemStack(Items.COCOA_BEANS, 4));
		helper.assertValueEqual(bunbug.textureName(), "bunbug_2_0_0", "texture after cocoa");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 3, "cocoa left in a survival hand");

		feed(player, bunbug, new ItemStack(BuiltInRegistries.ITEM.getValue(BlubberfishIds.Items.SPRINKLES), 4));
		helper.assertValueEqual(bunbug.textureName(), "bunbug_2_1_0", "texture after sprinkles");

		feed(player, bunbug, new ItemStack(Items.GLOW_BERRIES, 4));
		helper.assertValueEqual(bunbug.textureName(), "bunbug_2_1_2", "texture after glow berries");
		helper.succeed();
	}

	@GameTest
	public void eggsHatchABabyOnTheClickedFace(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.getValue(BunbugIds.Items.BUNBUG_EGGS), 2));
		TestScenes.useItemOn(helper, player, new BlockPos(3, 0, 3), Direction.UP);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "eggs left");
		helper.succeedWhen(() -> helper.assertEntityPresent(BunbugModule.BABY_BUNBUG, new BlockPos(3, 1, 3)));
	}

	@GameTest
	public void aBredBunbugBecomesALitterOfFour(GameTestHelper helper) {
		TestScenes.floor(helper);
		BunbugEntity newborn = helper.spawn(BunbugModule.BUNBUG, new BlockPos(3, 1, 3));
		newborn.setAge(-24000);
		helper.succeedWhen(() -> {
			helper.assertValueEqual(helper.getEntities(BunbugModule.BABY_BUNBUG).size(), 4, "baby bunbugs");
			helper.assertTrue(newborn.isRemoved(), "the bred baby is replaced by the litter");
		});
	}

	private static void feed(Player player, BunbugEntity bunbug, ItemStack stack) {
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		bunbug.mobInteract(player, InteractionHand.MAIN_HAND);
	}
}
