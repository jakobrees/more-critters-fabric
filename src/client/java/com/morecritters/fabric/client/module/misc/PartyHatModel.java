package com.morecritters.fabric.client.module.misc;

import com.morecritters.fabric.client.core.CritterModel;
import com.morecritters.fabric.client.core.ModelArmor;
import com.morecritters.fabric.ids.MiscIds;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.core.registries.BuiltInRegistries;

/** The worn party hat: a stepped cone of four boxes standing on top of the head (the original's {@code Modelparty_hat}). */
final class PartyHatModel {
	static void register() {
		ModelArmor.register(BuiltInRegistries.ITEM.getValue(MiscIds.Items.PARTY_HAT_HELMET), "party_hat", PartyHatModel::create,
			CritterModel.texture("party_hat"));
	}

	private static LayerDefinition create() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create()
			.texOffs(0, 0).addBox(-3.0F, -9.0F, -3.0F, 6.0F, 1.0F, 6.0F)
			.texOffs(0, 7).addBox(-2.0F, -12.0F, -2.0F, 4.0F, 3.0F, 4.0F)
			.texOffs(12, 14).addBox(-1.0F, -15.0F, -1.0F, 2.0F, 3.0F, 2.0F)
			.texOffs(0, 14).addBox(-1.5F, -18.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 32, 32);
	}

	private PartyHatModel() {}
}
