package com.morecritters.fabric.module.armossillo;

import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.ids.ArmossilloIds;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * The sturdy armour material (only a chestplate is made of it) and the chestplate's trick:
 * one hit in five against its wearer is shrugged off entirely, with a clank and a spray of
 * shell debris from the wearer's shoulders.
 */
public final class SturdyArmor {
	/** Durability multiplier, defense (boots, leggings, chestplate, helmet), enchantability, toughness, knockback resistance from the original. */
	private static final int DURABILITY = 27;
	private static final int ENCHANTABILITY = 9;
	private static final float TOUGHNESS = 2.5F;
	private static final float KNOCKBACK_RESISTANCE = 0.1F;

	/** The equipment asset {@code assets/more_critters/equipment/sturdy.json}. */
	private static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, ArmossilloIds.ArmorMaterials.STURDY);
	/** Repair tag the material refers to; the chestplate itself is repaired with sturdy shells (set on the item). */
	private static final TagKey<Item> REPAIRS_STURDY_ARMOR = TagKey.create(Registries.ITEM, ArmossilloIds.ArmorMaterials.STURDY);

	public static final ArmorMaterial MATERIAL = new ArmorMaterial(DURABILITY, ArmorMaterials.makeDefense(2, 5, 9, 2, 9), ENCHANTABILITY,
		SoundEvents.ARMOR_EQUIP_NETHERITE, TOUGHNESS, KNOCKBACK_RESISTANCE, REPAIRS_STURDY_ARMOR, ASSET);

	/** One hit in this many is nullified. */
	private static final int NULLIFY_ONE_IN = 5;
	/** Debris particles per nullified hit: 4 to 7. */
	private static final int DEBRIS_MIN = 4;
	private static final int DEBRIS_MAX = 7;
	private static final double DEBRIS_SPEED = 0.05;

	static void register() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(SturdyArmor::allowDamage);
	}

	private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (!wearsSturdyChestplate(entity) || isCreativeOrSpectator(entity)) return true;
		if (entity.getRandom().nextInt(NULLIFY_ONE_IN) != 0) return true;
		nullifyEffects(entity);
		return false;
	}

	private static boolean wearsSturdyChestplate(LivingEntity entity) {
		return entity.getItemBySlot(EquipmentSlot.CHEST).is(ArmossilloItems.STURDY_CHESTPLATE);
	}

	private static boolean isCreativeOrSpectator(LivingEntity entity) {
		return entity instanceof ServerPlayer player && (player.isCreative() || player.isSpectator());
	}

	/** The clank and the debris burst at the top of the wearer. */
	private static void nullifyEffects(LivingEntity entity) {
		Sounds.playAt(entity, ArmossilloItems.STURDY_CHESTPLATE_NULLIFY_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
		if (!(entity.level() instanceof ServerLevel level)) return;
		int count = Mth.nextInt(entity.getRandom(), DEBRIS_MIN, DEBRIS_MAX);
		level.sendParticles(ArmossilloModule.STURDY_DEBRIS, true, false,
			entity.getX(), entity.getY() + entity.getBbHeight(), entity.getZ(), count, 0.0, 0.0, 0.0, DEBRIS_SPEED);
	}

	private SturdyArmor() {}
}
