package com.morecritters.fabric.client.module.corpse_gear;

import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.module.corpse_gear.CorpseGearModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

/** The crew's thrown pearls, rum bottles and hardtack are drawn as their items, as in the original; worn pirate armour has its own models. */
public final class CorpseGearClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(CorpseGearModule.FLYING_PEARL, ThrownItemRenderer::new);
		EntityRendererRegistry.register(CorpseGearModule.HEALING_RUM_PROJECTILE, ThrownItemRenderer::new);
		EntityRendererRegistry.register(CorpseGearModule.SOUL_RUM_PROJECTILE, ThrownItemRenderer::new);
		EntityRendererRegistry.register(CorpseGearModule.THROWN_HARDTACK, ThrownItemRenderer::new);
		EntityRendererRegistry.register(CorpseGearModule.THROWN_INFESTED_HARDTACK, ThrownItemRenderer::new);
		PirateArmorModels.register();
	}
}
