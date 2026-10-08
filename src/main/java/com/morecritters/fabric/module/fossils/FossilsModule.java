package com.morecritters.fabric.module.fossils;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.FossilsIds;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;

/**
 * Fossils dug out of fossil ore, the display stand that shows any one of them, the grave
 * brush that digs mobs out of dirt, and the ancient skeleton exhibit.
 */
public final class FossilsModule implements Module {
	public static SoundEvent SKELETON_PLACE_SOUND;
	public static SoundEvent SKELETON_HURT_SOUND;
	public static SoundEvent GRAVE_BRUSH_SOUND;

	public static EntityType<AncientSkeletonExhibitEntity> ANCIENT_SKELETON_EXHIBIT;
	public static FossilDisplayBlock EMPTY_DISPLAY;

	@Override
	public void register() {
		SKELETON_PLACE_SOUND = Registration.sound(FossilsIds.Sounds.ENTITY_ANCIENT_SKELETON_PLACE);
		SKELETON_HURT_SOUND = Registration.sound(FossilsIds.Sounds.ENTITY_ANCIENT_SKELETON_HURT);
		GRAVE_BRUSH_SOUND = Registration.sound(FossilsIds.Sounds.GRAVE_BRUSH_USE);

		ANCIENT_SKELETON_EXHIBIT = Registration.livingEntity(FossilsIds.Entities.ANCIENT_SKELETON_EXHIBIT,
			EntityType.Builder.of(AncientSkeletonExhibitEntity::new, MobCategory.MONSTER)
				.sized(2.0F, 3.0F).fireImmune().clientTrackingRange(8).updateInterval(3),
			AncientSkeletonExhibitEntity.createAttributes());

		registerItems();
		registerBlocks();
		addFossilOre();
	}

	private static void registerItems() {
		for (Identifier fossil : Fossils.OWN_FOSSILS) {
			Registration.item(fossil, Tooltips.describe(new Item.Properties().rarity(Rarity.UNCOMMON), fossil.getPath(), 2));
		}
		Registration.item(FossilsIds.Items.ANCIENT_SKELETON_EXHIBIT_ITEM, AncientSkeletonExhibitItem::new,
			new Item.Properties().stacksTo(1));
		Registration.item(FossilsIds.Items.GRAVE_BRUSH, GraveBrushItem::new,
			new Item.Properties().durability(10).rarity(Rarity.UNCOMMON));
	}

	private static void registerBlocks() {
		Registration.blockWithItem(FossilsIds.Blocks.FOSSIL_BLOCK, FossilBlock::new,
			BlockBehaviour.Properties.of().sound(SoundType.STONE).strength(1.5F, 6.0F).requiresCorrectToolForDrops());
		Registration.blockWithItem(FossilsIds.Blocks.DEEPSLATE_FOSSIL_BLOCK, FossilBlock::new,
			BlockBehaviour.Properties.of().sound(SoundType.DEEPSLATE).strength(3.0F, 6.0F).requiresCorrectToolForDrops());

		EMPTY_DISPLAY = Registration.blockWithItem(FossilsIds.Blocks.FOSSIL_DISPLAY,
			properties -> new FossilDisplayBlock(properties, null), displayProperties());
		for (int i = 0; i < Fossils.DISPLAYS.size(); i++) {
			Identifier fossil = Fossils.ALL_FOSSILS.get(i);
			Registration.blockWithItem(Fossils.DISPLAYS.get(i),
				properties -> new FossilDisplayBlock(properties, fossil), displayProperties());
		}
	}

	private static BlockBehaviour.Properties displayProperties() {
		return BlockBehaviour.Properties.of().sound(SoundType.WOOD).strength(2.0F).noOcclusion()
			.isRedstoneConductor((state, level, pos) -> false);
	}

	/** The original's biome modifiers add both ores to every biome. */
	private static void addFossilOre() {
		BiomeModifications.addFeature(BiomeSelectors.all(), GenerationStep.Decoration.UNDERGROUND_ORES,
			ResourceKey.create(Registries.PLACED_FEATURE, FossilsIds.Blocks.FOSSIL_BLOCK));
		BiomeModifications.addFeature(BiomeSelectors.all(), GenerationStep.Decoration.UNDERGROUND_ORES,
			ResourceKey.create(Registries.PLACED_FEATURE, FossilsIds.Blocks.DEEPSLATE_FOSSIL_BLOCK));
	}
}
