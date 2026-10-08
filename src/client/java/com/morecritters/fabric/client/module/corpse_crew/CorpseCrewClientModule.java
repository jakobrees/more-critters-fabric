package com.morecritters.fabric.client.module.corpse_crew;

import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterModel;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.corpse_crew.CorpseCrewModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

/** Rendering for the corpse crew, the crew muster, the tamed parrot, pebbles and lookout spit. */
public final class CorpseCrewClientModule implements ClientModule {
	@Override
	public void registerClient() {
		// Shadow and scale from the original renderers. Mate and captain pick their texture per frame.
		EntityRendererRegistry.register(CorpseCrewModule.CORPSE_MATE,
			context -> new CrewRenderer<>(context, new CritterModel<>("corpse_mate", "corpse_mate", "corpse_mate0"), 0.5F, 1.1F));
		EntityRendererRegistry.register(CorpseCrewModule.CORPSE_CAPTAIN,
			context -> new CrewRenderer<>(context, new CritterModel<>("corpse_captain", "corpse_captain", "corpse_captain1"), 0.5F, 1.1F));
		EntityRendererRegistry.register(CorpseCrewModule.CORPSE_QUARTERMASTER, context -> new CrewRenderer<>(context, "corpse_quartermaster", 0.5F, 1.1F));
		EntityRendererRegistry.register(CorpseCrewModule.CORPSE_TANK, context -> new CrewRenderer<>(context, "corpse_tank", 0.5F, 1.1F));
		EntityRendererRegistry.register(CorpseCrewModule.CORPSE_LOOKOUT, context -> new CrewRenderer<>(context, "coprse_lookout", 0.5F, 1.0F));
		EntityRendererRegistry.register(CorpseCrewModule.CORPSE_PARROT, context -> new CrewRenderer<>(context, "corpse_parrot", 0.4F, 1.0F));
		EntityRendererRegistry.register(CorpseCrewModule.TAMED_CORPSE_PARROT, context -> new CrewRenderer<>(context, "corpse_parrot_tamed", 0.4F, 1.0F));
		// The muster looks like a plain mate for the one tick it exists.
		EntityRendererRegistry.register(CorpseCrewModule.CORPSE_CREW,
			context -> new CritterRenderer<>(context, new CritterModel<>("corpse_mate", "corpse_mate", "corpse_mate0"), 0.5F, 1.0F));

		EntityRendererRegistry.register(CorpseCrewModule.PEBBLE, ThrownItemRenderer::new);
		EntityRendererRegistry.register(CorpseCrewModule.LOOKOUT_SPIT, ThrownItemRenderer::new);
	}
}
