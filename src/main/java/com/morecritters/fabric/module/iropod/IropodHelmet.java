package com.morecritters.fabric.module.iropod;

import com.morecritters.fabric.ids.IropodIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import org.jspecify.annotations.Nullable;

/** The iropod helmet: worn, it gives Resistance while sneaking and Dolphin's Grace when wet on the ground. */
public class IropodHelmet extends Item {
	private static final int DURABILITY = 15;
	private static final int ENCHANTABILITY = 9;
	private static final float TOUGHNESS = 0.5F;
	private static final float KNOCKBACK_RESISTANCE = 0.0F;
	private static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, IropodIds.ArmorMaterials.IROPOD_HELMET);
	private static final TagKey<Item> REPAIRS_IROPOD_HELMET = TagKey.create(Registries.ITEM, IropodIds.ArmorMaterials.IROPOD_HELMET);
	/** Defense as in the original: boots 2, leggings 5, chestplate 6, helmet 3 (body armor unused, kept at the chestplate value). */
	public static final ArmorMaterial MATERIAL = new ArmorMaterial(DURABILITY, ArmorMaterials.makeDefense(2, 5, 6, 3, 6), ENCHANTABILITY,
		SoundEvents.ARMOR_EQUIP_GENERIC, TOUGHNESS, KNOCKBACK_RESISTANCE, REPAIRS_IROPOD_HELMET, ASSET);

	private static final int DOLPHINS_GRACE_AMPLIFIER = 2;

	public IropodHelmet(Properties properties) {
		super(properties);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		super.inventoryTick(stack, level, owner, slot);
		if (slot != EquipmentSlot.HEAD || !(owner instanceof Player player)) return;
		if (player.isShiftKeyDown()) player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 1, 0, false, true));
		if (player.isInWaterOrRain() && player.onGround()) {
			player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 1, DOLPHINS_GRACE_AMPLIFIER, false, true));
		}
	}
}
