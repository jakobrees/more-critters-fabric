package com.morecritters.fabric.client.module.armossillo;

import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.armossillo.ArmossilloEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Client side of the armossillo module's entities: renderers for the armossillo and its baby. */
final class ArmossilloEntitiesClient {
	static void register() {
		EntityRendererRegistry.register(ArmossilloEntities.ARMOSSILLO, context -> new CritterRenderer<>(context, "armossillo", 0.5F, 1.2F));
		EntityRendererRegistry.register(ArmossilloEntities.BABY_ARMOSSILLO, context -> new CritterRenderer<>(context, "baby_armossillo", 0.4F, 1.0F));
	}

	private ArmossilloEntitiesClient() {}
}
