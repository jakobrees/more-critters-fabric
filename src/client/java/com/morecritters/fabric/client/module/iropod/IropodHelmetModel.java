package com.morecritters.fabric.client.module.iropod;

import com.morecritters.fabric.client.core.ModelArmor;
import com.morecritters.fabric.ids.IropodIds;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.core.registries.BuiltInRegistries;

/** The worn iropod helmet: a shell around the head with a flap down the back of the neck (the original's {@code Modeliropod_helmet}). */
final class IropodHelmetModel {
	static void register() {
		ModelArmor.register(BuiltInRegistries.ITEM.getValue(IropodIds.Items.IROPOD_HELMET_HELMET), "iropod_helmet", IropodHelmetModel::create,
			ModelArmor.armorTexture("iropod_helmet_layer_1"));
	}

	private static LayerDefinition create() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create()
			.texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.75F))
			.texOffs(24, 0).addBox(-4.0F, 0.75F, 4.0F, 8.0F, 4.0F, 0.0F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 32);
	}

	private IropodHelmetModel() {}
}
