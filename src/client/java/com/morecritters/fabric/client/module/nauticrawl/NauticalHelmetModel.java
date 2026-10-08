package com.morecritters.fabric.client.module.nauticrawl;

import com.morecritters.fabric.client.core.ModelArmor;
import com.morecritters.fabric.module.nauticrawl.NauticrawlItems;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * The worn nautical helmet: a shell around the head with three fins running front to back over the top and a flap
 * down the back of the neck (the original's {@code Modelnautical_helmet}).
 */
final class NauticalHelmetModel {
	static void register() {
		ModelArmor.register(NauticrawlItems.NAUTICAL_HELMET, "nautical_helmet", NauticalHelmetModel::create,
			ModelArmor.armorTexture("nautical_helmet_layer_1"));
	}

	private static LayerDefinition create() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create()
			.texOffs(0, 0).addBox(-4.0F, -8.25F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.75F))
			.texOffs(1, 13).addBox(-3.5F, -12.25F, -3.0F, 0.0F, 9.0F, 10.0F)
			.texOffs(1, 13).addBox(3.5F, -12.25F, -3.0F, 0.0F, 9.0F, 10.0F)
			.texOffs(1, 13).addBox(0.0F, -12.25F, -3.0F, 0.0F, 9.0F, 10.0F)
			.texOffs(24, 0).addBox(-4.0F, 0.5F, 4.0F, 8.0F, 4.0F, 0.0F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 32);
	}

	private NauticalHelmetModel() {}
}
