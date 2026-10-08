package com.morecritters.fabric.module.critter_atlas;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.CritterAtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * The critter atlas: the book item, and the display models some of its pages show (the closed book,
 * and critters in an atlas pose or a fixed look). The pages themselves are client-side; see
 * {@code CritterAtlasScreen}.
 */
public final class CritterAtlasModule implements Module {
	public static Item CRITTER_ATLAS;

	public static EntityType<AtlasDisplayModel> CRITTER_ATLAS_MODEL;
	public static EntityType<AtlasDisplayModel> BOUNCELIZARD_MODEL;
	public static EntityType<AtlasDisplayModel> BALLOON_RAT_MODEL;
	public static EntityType<AtlasDisplayModel> SHIMMERWING_MODEL;
	public static EntityType<AtlasDisplayModel> MIGHTSHROOM_MODEL;
	public static EntityType<AtlasDisplayModel> IROPOD_MODEL;
	public static EntityType<AtlasDisplayModel> KELPIRE_MODEL;
	public static EntityType<AtlasDisplayModel> NAUTICRAWL_MODEL;
	public static EntityType<AtlasDisplayModel> SHADELET_MODEL;
	public static EntityType<AtlasDisplayModel> NERVOID_MODEL;

	@Override
	public void register() {
		CRITTER_ATLAS = Registration.item(CritterAtlasIds.Items.CRITTER_ATLAS, CritterAtlasItem::new,
			Tooltips.describe(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON), "critter_atlas", 1));

		// The original's names: model_N is the Nth display model it made. The closed book and the
		// nervoid's atlas pose have no idle animation.
		CRITTER_ATLAS_MODEL = displayModel(CritterAtlasIds.Entities.CRITTER_ATLAS_MODEL, false);
		BOUNCELIZARD_MODEL = displayModel(CritterAtlasIds.Entities.MODEL_1, true);
		BALLOON_RAT_MODEL = displayModel(CritterAtlasIds.Entities.MODEL_7, true);
		SHIMMERWING_MODEL = displayModel(CritterAtlasIds.Entities.MODEL_9, true);
		MIGHTSHROOM_MODEL = displayModel(CritterAtlasIds.Entities.MODEL_10, true);
		IROPOD_MODEL = displayModel(CritterAtlasIds.Entities.MODEL_13, true);
		KELPIRE_MODEL = displayModel(CritterAtlasIds.Entities.MODEL_15, true);
		NAUTICRAWL_MODEL = displayModel(CritterAtlasIds.Entities.MODEL_16, true);
		SHADELET_MODEL = displayModel(CritterAtlasIds.Entities.MODEL_17, true);
		NERVOID_MODEL = displayModel(CritterAtlasIds.Entities.MODEL_19, false);
	}

	private static EntityType<AtlasDisplayModel> displayModel(Identifier id, boolean hasIdle) {
		return Registration.livingEntity(id,
			EntityType.Builder.<AtlasDisplayModel>of((type, level) -> new AtlasDisplayModel(type, level, hasIdle), MobCategory.MISC)
				.sized(0.6F, 1.8F).clientTrackingRange(8).updateInterval(3),
			AtlasDisplayModel.createAttributes());
	}
}
