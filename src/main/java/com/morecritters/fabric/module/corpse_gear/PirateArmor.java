package com.morecritters.fabric.module.corpse_gear;

import com.morecritters.fabric.ids.CorpseGearIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/** The pirate armour material (tricorne, coat, pants, boots; repaired with tattered cloth) and the cutlass's tool material. */
public final class PirateArmor {
	private static final int DURABILITY = 15;
	private static final int DEFENSE_PER_PIECE = 2;
	private static final int ENCHANTABILITY = 9;
	private static final float TOUGHNESS = 0.5F;

	/**
	 * The equipment asset {@code assets/more_critters/equipment/pirate.json}, which does not exist yet: the original
	 * drew the pieces with model classes instead of flat layer textures (see NOTES.md, Needs).
	 */
	private static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, CorpseGearIds.ArmorMaterials.PIRATE);
	/** Tag {@code more_critters:pirate}: tattered cloth (the corpse crew's drop) in the original. */
	private static final TagKey<Item> REPAIRS_PIRATE_ARMOR = TagKey.create(Registries.ITEM, CorpseGearIds.ArmorMaterials.PIRATE);

	/** The original equipped silently. */
	public static final ArmorMaterial MATERIAL = new ArmorMaterial(DURABILITY,
		ArmorMaterials.makeDefense(DEFENSE_PER_PIECE, DEFENSE_PER_PIECE, DEFENSE_PER_PIECE, DEFENSE_PER_PIECE, DEFENSE_PER_PIECE), ENCHANTABILITY,
		BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.EMPTY), TOUGHNESS, 0.0F, REPAIRS_PIRATE_ARMOR, ASSET);

	/** Iron-tier mining, 300 uses, speed 4, +2 damage, enchantability 2, repaired with iron ingots. */
	static final ToolMaterial CUTLASS_MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 300, 4.0F, 2.0F, 2, ItemTags.IRON_TOOL_MATERIALS);

	private PirateArmor() {}
}
