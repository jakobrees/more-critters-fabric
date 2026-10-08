package com.morecritters.fabric.module.nauticrawl;

import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.NauticrawlIds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;

/**
 * The nauticrawl's spoils: shell pieces, the edible tentacle, the nautical axe (which hits
 * anything wet for 15 extra damage), the nautical helmet (sneak to tuck in: Resistance III
 * and Slowness) and the zombie nauticrawl spawn doll.
 */
public final class NauticrawlItems {
	public static Item SHELL_PIECES, TENTACLE, NAUTICAL_AXE, NAUTICAL_HELMET, ZOMBIE_SPAWN_DOLL;

	/** Durability multiplier, defense (boots, leggings, chestplate, helmet), enchantability, toughness from the original. */
	private static final ArmorMaterial HELMET_MATERIAL = new ArmorMaterial(15, ArmorMaterials.makeDefense(2, 5, 6, 3, 6), 9,
		SoundEvents.ARMOR_EQUIP_GENERIC, 0.5F, 0.0F,
		TagKey.create(Registries.ITEM, NauticrawlIds.ArmorMaterials.NAUTICAL_HELMET),
		ResourceKey.create(EquipmentAssets.ROOT_ID, NauticrawlIds.ArmorMaterials.NAUTICAL_HELMET));

	/** 582 uses, speed 8, +7 damage, diamond-level mining, enchantability 2. */
	private static final ToolMaterial AXE_MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 582, 8.0F, 7.0F, 2,
		TagKey.create(Registries.ITEM, NauticrawlIds.Items.NAUTICAL_AXE));
	private static final float AXE_WET_BONUS_DAMAGE = 15.0F;

	static void register() {
		SHELL_PIECES = Registration.item(NauticrawlIds.Items.SHELL_PIECES, new Item.Properties());
		TENTACLE = Registration.item(NauticrawlIds.Items.NAUTICRAWL_TENTACLE,
			new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build()));
		NAUTICAL_AXE = Registration.item(NauticrawlIds.Items.NAUTICAL_AXE, NauticalAxe::new, Tooltips.describe(new Item.Properties()
			.axe(AXE_MATERIAL, 1.0F, -3.0F).repairable(SHELL_PIECES), "nautical_axe", 1));
		NAUTICAL_HELMET = Registration.item(NauticrawlIds.Items.NAUTICAL_HELMET_HELMET, Tooltips.describe(new Item.Properties()
			.humanoidArmor(HELMET_MATERIAL, ArmorType.HELMET).repairable(SHELL_PIECES), "nautical_helmet_helmet", 1));
		ZOMBIE_SPAWN_DOLL = Registration.item(NauticrawlIds.Items.ZOMBIE_NAUTICRAWL_SPAWN_DOLL, SpawnDoll::new,
			Tooltips.describe(new Item.Properties(), "zombie_nauticrawl_spawn_doll", 3));

		ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(NauticrawlItems::tuckIntoHelmet));
	}

	/** A sneaking wearer of the nautical helmet ducks into it: Resistance III and Slowness while crouched. */
	private static void tuckIntoHelmet(ServerPlayer player) {
		if (!player.isShiftKeyDown() || !player.getItemBySlot(EquipmentSlot.HEAD).is(NAUTICAL_HELMET)) return;
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 1, 2, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 1, 0, false, true));
	}

	/** Deals 15 extra damage to anything standing in water or rain. */
	static final class NauticalAxe extends Item {
		NauticalAxe(Properties properties) {
			super(properties);
		}

		@Override
		public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
			super.hurtEnemy(stack, target, attacker);
			if (target.isInWaterOrRain() && target.level() instanceof ServerLevel level) {
				target.hurtServer(level, level.damageSources().generic(), AXE_WET_BONUS_DAMAGE);
			}
		}
	}

	/** Used on a block, summons a zombie nauticrawl against the clicked face. */
	static final class SpawnDoll extends Item {
		SpawnDoll(Properties properties) {
			super(properties);
		}

		@Override
		public InteractionResult useOn(UseOnContext context) {
			context.getItemInHand().shrink(1);
			if (context.getLevel() instanceof ServerLevel level) {
				NauticrawlModule.ZOMBIE_NAUTICRAWL.spawn(level, context.getClickedPos().relative(context.getClickedFace()), EntitySpawnReason.MOB_SUMMONED);
			}
			return InteractionResult.SUCCESS;
		}
	}

	private NauticrawlItems() {}
}
