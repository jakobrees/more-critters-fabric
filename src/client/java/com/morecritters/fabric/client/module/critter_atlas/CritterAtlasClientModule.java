package com.morecritters.fabric.client.module.critter_atlas;

import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterModel;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.critter_atlas.AtlasDisplayModel;
import com.morecritters.fabric.module.critter_atlas.CritterAtlasItem;
import com.morecritters.fabric.module.critter_atlas.CritterAtlasModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** The atlas screen, and renderers for the display models its pages show. */
public final class CritterAtlasClientModule implements ClientModule {
	@Override
	public void registerClient() {
		CritterAtlasItem.screenOpener = sneaking ->
			Minecraft.getInstance().gui.setScreen(new CritterAtlasScreen(sneaking ? AtlasPages.COVER : AtlasPages.ROOT));

		// Model, animations and texture names from the original's Model<N>Model classes.
		EntityRendererRegistry.register(CritterAtlasModule.CRITTER_ATLAS_MODEL, displayModel("critter_atlas", "critter_atlas", "critter_atlas"));
		EntityRendererRegistry.register(CritterAtlasModule.BOUNCELIZARD_MODEL, displayModel("bouncelizard", "bouncelizard", "bouncelizard0"));
		EntityRendererRegistry.register(CritterAtlasModule.BALLOON_RAT_MODEL, displayModel("balloon_rat", "balloon_rat", "balloon_rat"));
		EntityRendererRegistry.register(CritterAtlasModule.SHIMMERWING_MODEL, displayModel("shimmerwing_atlas_model", "shimmerwing_atlas_model", "shimmerwing"));
		EntityRendererRegistry.register(CritterAtlasModule.MIGHTSHROOM_MODEL, displayModel("mightshroom_atlas_model", "mightshroom_atlas_model", "mightshroom"));
		EntityRendererRegistry.register(CritterAtlasModule.IROPOD_MODEL, displayModel("iropod_atlas_model", "iropod_atlas_model", "iropod"));
		EntityRendererRegistry.register(CritterAtlasModule.KELPIRE_MODEL, displayModel("kelpire", "kelpire", "kelpire"));
		EntityRendererRegistry.register(CritterAtlasModule.NAUTICRAWL_MODEL, displayModel("nauticrawl_atlas_model", "nauticrawl_atlas_model", "nauticrawl"));
		EntityRendererRegistry.register(CritterAtlasModule.SHADELET_MODEL, displayModel("shadelet", "shadelet", "shadelet_full"));
		EntityRendererRegistry.register(CritterAtlasModule.NERVOID_MODEL, displayModel("nervoid_atlas_model", "nervoid_atlas_model", "nervoid"));
	}

	private static EntityRendererProvider<AtlasDisplayModel> displayModel(String model, String animations, String texture) {
		return context -> new CritterRenderer<>(context, new CritterModel<>(model, animations, texture), 0.5F, 1.0F);
	}
}
