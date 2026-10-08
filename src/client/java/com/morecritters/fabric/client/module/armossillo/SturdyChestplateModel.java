package com.morecritters.fabric.client.module.armossillo;

import com.morecritters.fabric.client.core.ModelArmor;
import com.morecritters.fabric.module.armossillo.ArmossilloItems;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The worn sturdy chestplate: a thick shell over the body and shoulder plates that reach three pixels above the
 * shoulders (the original's {@code Modelarmor_layer_1}); its texture does not fit the vanilla chestplate shape.
 */
final class SturdyChestplateModel {
	static void register() {
		ModelArmor.register(ArmossilloItems.STURDY_CHESTPLATE, "sturdy_chestplate", SturdyChestplateModel::create,
			ModelArmor.armorTexture("sturdy_chestplate_layer_1"));
	}

	private static LayerDefinition create() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		CubeDeformation shell = new CubeDeformation(0.75F);
		root.addOrReplaceChild("body", CubeListBuilder.create()
			.texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, shell), PartPose.ZERO);
		root.addOrReplaceChild("right_arm", CubeListBuilder.create()
			.texOffs(40, 13).addBox(-3.0F, -5.0F, -2.0F, 4.0F, 15.0F, 4.0F, shell), PartPose.ZERO);
		root.addOrReplaceChild("left_arm", CubeListBuilder.create()
			.texOffs(40, 13).mirror().addBox(-1.0F, -5.0F, -2.0F, 4.0F, 15.0F, 4.0F, shell), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 32);
	}

	private SturdyChestplateModel() {}
}
