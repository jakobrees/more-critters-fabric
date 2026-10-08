package com.morecritters.fabric.test;

import com.morecritters.fabric.ids.CorpseCrewIds;
import com.morecritters.fabric.ids.CorpseGearIds;
import com.morecritters.fabric.module.corpse_gear.CorpseGearModule;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** The crew's gear: hardtack, the rums, the flying pearl, the stun, the cutlass and the pirate armour. */
public class CorpseGearTests {
	@GameTest
	public void throwingHardtackUsesOneBiscuitInSurvivalAndNoneInCreative(GameTestHelper helper) {
		TestScenes.floor(helper);
		Player survival = playerAt(helper, GameType.SURVIVAL, new Vec3(2.5, 1.0, 3.5));
		Player creative = playerAt(helper, GameType.CREATIVE, new Vec3(5.5, 1.0, 3.5));
		survival.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CorpseGearModule.HARDTACK, 16));
		creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CorpseGearModule.INFESTED_HARDTACK, 16));

		CorpseGearModule.HARDTACK.use(helper.getLevel(), survival, InteractionHand.MAIN_HAND);
		CorpseGearModule.INFESTED_HARDTACK.use(helper.getLevel(), creative, InteractionHand.MAIN_HAND);

		helper.assertValueEqual(survival.getMainHandItem().getCount(), 15, "hardtack left in a survival hand");
		helper.assertValueEqual(creative.getMainHandItem().getCount(), 16, "infested hardtack left in a creative hand");
		helper.assertValueEqual(helper.getEntities(CorpseGearModule.THROWN_HARDTACK).size(), 1, "thrown hardtack");
		helper.assertValueEqual(helper.getEntities(CorpseGearModule.THROWN_INFESTED_HARDTACK).size(), 1, "thrown infested hardtack");
		helper.succeed();
	}

	@GameTest
	public void infestedHardtackAlwaysStunsForThreeSeconds(GameTestHelper helper) {
		TestScenes.floor(helper);
		LivingEntity pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 1, 3));
		dropOnto(helper, CorpseGearModule.THROWN_INFESTED_HARDTACK, new Vec3(3.5, 4.0, 3.5));
		helper.succeedWhen(() -> {
			MobEffectInstance stun = pig.getEffect(CorpseGearModule.STUNNED);
			helper.assertTrue(stun != null, "the pig is stunned");
			helper.assertTrue(stun.getDuration() > 50 && stun.getDuration() <= 60, "stun lasts 60 ticks, has " + stun.getDuration());
		});
	}

	@GameTest
	public void hardtackOnTheGroundCrumblesAway(GameTestHelper helper) {
		TestScenes.floor(helper);
		Entity biscuit = dropOnto(helper, CorpseGearModule.THROWN_HARDTACK, new Vec3(3.5, 3.0, 3.5));
		helper.succeedWhen(() -> {
			helper.assertTrue(biscuit.isRemoved(), "the biscuit is gone after landing");
			helper.assertItemEntityNotPresent(CorpseGearModule.HARDTACK);
		});
	}

	@GameTest
	public void aStunnedMobBarelyMovesWhenShoved(GameTestHelper helper) {
		TestScenes.floor(helper);
		Mob stunned = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(1, 1, 2));
		Mob free = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(1, 1, 5));
		stunned.addEffect(new MobEffectInstance(CorpseGearModule.STUNNED, 100));
		double stunnedStart = stunned.getX();
		double freeStart = free.getX();
		helper.runAfterDelay(1, () -> {
			stunned.push(1.5, 0.0, 0.0);
			free.push(1.5, 0.0, 0.0);
		});
		helper.runAfterDelay(15, () -> {
			helper.assertTrue(free.getX() - freeStart > 2.0, "an unstunned pig is shoved over 2 blocks, went " + (free.getX() - freeStart));
			helper.assertTrue(Math.abs(stunned.getX() - stunnedStart) < 0.5, "a stunned pig stays put, went " + (stunned.getX() - stunnedStart));
			helper.succeed();
		});
	}

	/**
	 * The crew gets Regeneration II and anyone else Poison II. Only the lookout can take the regeneration: the rest of
	 * the crew is undead (MobType.UNDEAD in the original, the minecraft:undead tag here) and refuses both.
	 */
	@GameTest
	public void crewsRumPoisonsOutsidersAndRegeneratesTheCrew(GameTestHelper helper) {
		TestScenes.floor(helper);
		LivingEntity pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 1, 3));
		LivingEntity lookout = helper.spawnWithNoFreeWill(crew(CorpseCrewIds.Entities.CORPSE_LOOKOUT), new BlockPos(5, 1, 3));
		LivingEntity mate = helper.spawnWithNoFreeWill(crew(CorpseCrewIds.Entities.CORPSE_MATE), new BlockPos(5, 1, 6));
		for (LivingEntity drinker : List.of(pig, lookout, mate)) drinker.addEffect(new MobEffectInstance(CorpseGearModule.CREWS_RUM, 200));
		helper.succeedWhen(() -> {
			for (LivingEntity drinker : List.of(pig, lookout, mate)) {
				helper.assertFalse(drinker.hasEffect(CorpseGearModule.CREWS_RUM), "the rum wears off at once (" + drinker.getType() + ")");
			}
			assertEffect(helper, pig, MobEffects.POISON, 1, "pig");
			assertEffect(helper, lookout, MobEffects.REGENERATION, 1, "lookout");
			helper.assertFalse(pig.hasEffect(MobEffects.REGENERATION), "the pig is not regenerating");
			helper.assertFalse(lookout.hasEffect(MobEffects.POISON), "the lookout is not poisoned");
			helper.assertFalse(mate.hasEffect(MobEffects.POISON), "the mate is not poisoned");
		});
	}

	@GameTest
	public void healingRumBreaksOverAMateAndHealsTheCrewButNotTheLookout(GameTestHelper helper) {
		TestScenes.floor(helper);
		LivingEntity mate = helper.spawnWithNoFreeWill(crew(CorpseCrewIds.Entities.CORPSE_MATE), new BlockPos(1, 1, 3));
		LivingEntity captain = helper.spawnWithNoFreeWill(crew(CorpseCrewIds.Entities.CORPSE_CAPTAIN), new BlockPos(5, 1, 3));
		LivingEntity lookout = helper.spawnWithNoFreeWill(crew(CorpseCrewIds.Entities.CORPSE_LOOKOUT), new BlockPos(1, 1, 6));
		for (LivingEntity member : List.of(mate, captain, lookout)) member.setHealth(4.0F);
		Entity rum = helper.spawn(CorpseGearModule.HEALING_RUM_PROJECTILE, new Vec3(1.5, 2.5, 3.5));
		rum.setDeltaMovement(0.0, 0.05, 0.0);
		helper.succeedWhen(() -> {
			helper.assertTrue(rum.isRemoved(), "the bottle broke");
			helper.assertTrue(mate.getHealth() >= 9.0F && mate.getHealth() <= 14.0F, "mate healed by 5-10, has " + mate.getHealth());
			helper.assertTrue(captain.getHealth() >= 9.0F && captain.getHealth() <= 14.0F, "captain healed by 5-10, has " + captain.getHealth());
			helper.assertValueEqual(lookout.getHealth(), 4.0F, "lookout health");
			helper.assertEntityNotPresent(EntityTypes.AREA_EFFECT_CLOUD);
		});
	}

	/** The original ran the "near the crew" check on the landing tick too, so a bottle smashed at a mate's feet heals. */
	@GameTest
	public void healingRumLandingAtAMatesFeetStillHealsTheCrew(GameTestHelper helper) {
		TestScenes.floor(helper);
		LivingEntity mate = helper.spawnWithNoFreeWill(crew(CorpseCrewIds.Entities.CORPSE_MATE), new BlockPos(3, 1, 3));
		mate.setHealth(4.0F);
		Entity rum = helper.spawn(CorpseGearModule.HEALING_RUM_PROJECTILE, new Vec3(4.2, 1.3, 3.5));
		rum.setDeltaMovement(0.0, -0.8, 0.0);
		helper.succeedWhen(() -> {
			helper.assertTrue(rum.isRemoved(), "the bottle broke");
			helper.assertTrue(mate.getHealth() >= 9.0F, "mate healed by 5-10, has " + mate.getHealth());
		});
	}

	@GameTest(maxTicks = 80)
	public void healingRumOnTheGroundLeavesARegenerationCloud(GameTestHelper helper) {
		TestScenes.floor(helper);
		LivingEntity pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(4, 1, 3));
		dropOnto(helper, CorpseGearModule.HEALING_RUM_PROJECTILE, new Vec3(3.5, 3.0, 3.5));
		helper.succeedWhen(() -> {
			List<AreaEffectCloud> clouds = helper.getEntities(EntityTypes.AREA_EFFECT_CLOUD);
			helper.assertValueEqual(clouds.size(), 1, "clouds");
			helper.assertValueEqual(clouds.getFirst().getDuration(), 60, "cloud duration");
			helper.assertTrue(pig.hasEffect(MobEffects.REGENERATION), "a pig in the cloud regenerates");
		});
	}

	@GameTest
	public void aFlyingPearlBlowsUpWithoutBreakingBlocks(GameTestHelper helper) {
		TestScenes.floor(helper);
		LivingEntity pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(4, 1, 3));
		Entity pearl = dropOnto(helper, CorpseGearModule.FLYING_PEARL, new Vec3(3.5, 3.0, 3.5));
		helper.succeedWhen(() -> {
			helper.assertTrue(pearl.isRemoved(), "the pearl is gone");
			helper.assertTrue(!pig.isAlive() || pig.getHealth() < pig.getMaxHealth(), "the blast hurt the pig");
			for (int x = 0; x < 8; x++) {
				for (int z = 0; z < 8; z++) {
					helper.assertBlockPresent(Blocks.STONE, x, 0, z);
				}
			}
		});
	}

	@GameTest
	public void soulRumRaisesAMateQuartermasterOrTank(GameTestHelper helper) {
		TestScenes.floor(helper);
		dropOnto(helper, CorpseGearModule.SOUL_RUM_PROJECTILE, new Vec3(3.5, 3.0, 3.5));
		helper.succeedWhen(() -> {
			int raised = helper.getEntities(crew(CorpseCrewIds.Entities.CORPSE_MATE)).size()
				+ helper.getEntities(crew(CorpseCrewIds.Entities.CORPSE_QUARTERMASTER)).size()
				+ helper.getEntities(crew(CorpseCrewIds.Entities.CORPSE_TANK)).size();
			helper.assertValueEqual(raised, 1, "crew members raised");
		});
	}

	@GameTest
	public void theCutlassHitsForSixAtSpeedTwo(GameTestHelper helper) {
		ItemStack cutlass = new ItemStack(item(CorpseGearIds.Items.CUTLASS));
		ItemAttributeModifiers modifiers = cutlass.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		helper.assertValueEqual(modifiers.compute(Attributes.ATTACK_DAMAGE, 1.0, EquipmentSlot.MAINHAND), 6.0, "cutlass attack damage");
		helper.assertValueEqual(modifiers.compute(Attributes.ATTACK_SPEED, 4.0, EquipmentSlot.MAINHAND), 2.0, "cutlass attack speed");
		helper.assertValueEqual(cutlass.getMaxDamage(), 300, "cutlass durability");
		helper.assertTrue(cutlass.isValidRepairItem(new ItemStack(net.minecraft.world.item.Items.IRON_INGOT)), "iron repairs the cutlass");
		helper.succeed();
	}

	@GameTest
	public void pirateArmourGivesTwoEachAndIsMendedWithTatteredCloth(GameTestHelper helper) {
		ItemStack cloth = new ItemStack(item(CorpseCrewIds.Items.TATTERED_CLOTH));
		Object[][] pieces = {
			{CorpseGearIds.Items.PIRATE_HELMET, EquipmentSlot.HEAD, 165},
			{CorpseGearIds.Items.PIRATE_CHESTPLATE, EquipmentSlot.CHEST, 240},
			{CorpseGearIds.Items.PIRATE_LEGGINGS, EquipmentSlot.LEGS, 225},
			{CorpseGearIds.Items.PIRATE_BOOTS, EquipmentSlot.FEET, 195},
		};
		for (Object[] piece : pieces) {
			ItemStack stack = new ItemStack(item((net.minecraft.resources.Identifier) piece[0]));
			EquipmentSlot slot = (EquipmentSlot) piece[1];
			ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
			helper.assertValueEqual(modifiers.compute(Attributes.ARMOR, 0.0, slot), 2.0, piece[0] + " armour");
			helper.assertValueEqual(modifiers.compute(Attributes.ARMOR_TOUGHNESS, 0.0, slot), 0.5, piece[0] + " toughness");
			helper.assertValueEqual(stack.getMaxDamage(), (int) piece[2], piece[0] + " durability");
			helper.assertTrue(stack.isValidRepairItem(cloth), piece[0] + " is mended with tattered cloth");
		}
		helper.succeed();
	}

	// --- Helpers -------------------------------------------------------------------------

	private static Item item(net.minecraft.resources.Identifier id) {
		return BuiltInRegistries.ITEM.getValue(id);
	}

	@SuppressWarnings("unchecked")
	private static EntityType<Mob> crew(net.minecraft.resources.Identifier id) {
		return (EntityType<Mob>) BuiltInRegistries.ENTITY_TYPE.getValue(id);
	}

	/** A mock player standing at a spot of the test area. */
	private static Player playerAt(GameTestHelper helper, GameType gameType, Vec3 at) {
		Player player = helper.makeMockPlayer(gameType);
		// The plain mock player reports the game mode but keeps survival abilities (no infinite materials).
		gameType.updatePlayerAbilities(player.getAbilities());
		Vec3 absolute = helper.absoluteVec(at);
		player.snapTo(absolute.x, absolute.y, absolute.z);
		return player;
	}

	/** A projectile falling straight down from {@code from}. */
	private static <T extends Entity> T dropOnto(GameTestHelper helper, EntityType<T> type, Vec3 from) {
		T projectile = helper.spawn(type, from);
		projectile.setDeltaMovement(0.0, -0.8, 0.0);
		return projectile;
	}

	private static void assertEffect(GameTestHelper helper, LivingEntity entity, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
			int amplifier, String who) {
		MobEffectInstance instance = entity.getEffect(effect);
		helper.assertTrue(instance != null, who + " has " + effect.getRegisteredName());
		helper.assertValueEqual(instance.getAmplifier(), amplifier, who + "'s " + effect.getRegisteredName() + " amplifier");
		helper.assertTrue(instance.getDuration() <= 100 && instance.getDuration() > 80, who + "'s effect lasts 100 ticks, has " + instance.getDuration());
	}
}
