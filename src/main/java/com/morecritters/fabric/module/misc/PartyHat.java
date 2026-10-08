package com.morecritters.fabric.module.misc;

import com.morecritters.fabric.ids.MiscIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/** The party hat armour material; only a helmet is made of it, and nothing repairs it. */
public final class PartyHat {
	private static final int DURABILITY = 15;
	private static final int ENCHANTABILITY = 9;

	/**
	 * The equipment asset {@code assets/more_critters/equipment/party_hat.json}, which does not exist yet:
	 * the original drew the hat with a model class instead of a flat texture (see NOTES.md, Needs).
	 */
	private static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, MiscIds.ArmorMaterials.PARTY_HAT);
	/** The original repaired it with nothing; this tag is empty. */
	private static final TagKey<Item> REPAIRS_PARTY_HAT = TagKey.create(Registries.ITEM, MiscIds.ArmorMaterials.PARTY_HAT);

	public static final ArmorMaterial MATERIAL = new ArmorMaterial(DURABILITY, ArmorMaterials.makeDefense(2, 5, 6, 2, 6), ENCHANTABILITY,
		SoundEvents.ARMOR_EQUIP_GENERIC, 0.0F, 0.0F, REPAIRS_PARTY_HAT, ASSET);

	private PartyHat() {}
}
