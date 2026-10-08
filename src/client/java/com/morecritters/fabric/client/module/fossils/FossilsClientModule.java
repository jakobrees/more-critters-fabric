package com.morecritters.fabric.client.module.fossils;

import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterModel;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.fossils.FossilsModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Rendering for the ancient skeleton exhibit; the fossil blocks and displays are plain block models. */
public final class FossilsClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(FossilsModule.ANCIENT_SKELETON_EXHIBIT, context -> new CritterRenderer<>(context,
			new CritterModel<>("ancien_skeleton_exhibit", "ancien_skeleton_exhibit", "ancient_skeleton_exhibit"), 0.9F, 1.0F));
	}
}
