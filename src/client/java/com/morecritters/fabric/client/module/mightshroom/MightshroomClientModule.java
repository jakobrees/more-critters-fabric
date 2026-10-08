package com.morecritters.fabric.client.module.mightshroom;

import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterModel;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.client.core.SpriteParticle;
import com.morecritters.fabric.module.mightshroom.MightshroomModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Rendering for the mightshroom, its echo, the two heal echoes and the feather particle. */
public final class MightshroomClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(MightshroomModule.MIGHTSHROOM, MightshroomRenderer::new);
		EntityRendererRegistry.register(MightshroomModule.MIGHTSHROOM_ECHO, context -> new CritterRenderer<>(context, "mightshroom_echo", 0.0F, 1.0F));
		// Both heal echoes share one model; their texture starts at heal_echo1 and fades.
		EntityRendererRegistry.register(MightshroomModule.HEAL_ECHO,
			context -> new CritterRenderer<>(context, new CritterModel<>("heal_echo", "heal_echo", "heal_echo1"), 0.0F, 1.0F));
		EntityRendererRegistry.register(MightshroomModule.SMALL_HEAL_ECHO,
			context -> new CritterRenderer<>(context, new CritterModel<>("heal_echo", "heal_echo", "heal_echo1"), 0.0F, 0.4F));
		// Feathers: thirteen frames, one tick each, a twelve-tick fall; translucent.
		SpriteParticle.register(MightshroomModule.FEATHER, SpriteParticle.Settings.of(0.2F, 1.5F, 12, 0.02F, true, 0.01, 13, 1)
			.translucentSheet().spin(0.4F, -0.07F));
	}
}
