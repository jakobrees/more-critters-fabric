package com.morecritters.fabric.client.module.armossillo;

import com.morecritters.fabric.client.core.SpriteParticle;
import com.morecritters.fabric.module.armossillo.ArmossilloItems;
import com.morecritters.fabric.module.armossillo.ArmossilloModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Client side of the armossillo module's items: the thrown ooze rod, the worn sturdy chestplate and its debris. */
final class ArmossilloItemsClient {
	static void register() {
		EntityRendererRegistry.register(ArmossilloItems.FLYING_OOZE_ROD, FlyingOozeRodRenderer::new);
		SturdyChestplateModel.register();
		// Four shell shards, cycled over the particle's life in place of the original's random shard and spin.
		SpriteParticle.register(ArmossilloModule.STURDY_DEBRIS, SpriteParticle.Settings.of(0.2F, 1.5F, 20, 0.5F, true, 1.0, 1, 0)
			.randomSprite().spin(1.0F, -0.02F));
	}

	private ArmossilloItemsClient() {}
}
