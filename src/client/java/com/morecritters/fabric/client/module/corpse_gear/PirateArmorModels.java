package com.morecritters.fabric.client.module.corpse_gear;

import com.morecritters.fabric.client.core.CritterModel;
import com.morecritters.fabric.client.core.ModelArmor;
import com.morecritters.fabric.ids.CorpseGearIds;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * The worn pirate armour, each piece its own model and texture as in the original ({@code Modeltricorne},
 * {@code Modelpirate_coat}, {@code Modelpirate_pants}, {@code Modelpirate_boots}): a two-tier tricorne, a long coat
 * with puffed sleeves, trouser legs and turned-down boots.
 */
final class PirateArmorModels {
	static void register() {
		piece(CorpseGearIds.Items.PIRATE_HELMET, "tricorne", PirateArmorModels::tricorne);
		piece(CorpseGearIds.Items.PIRATE_CHESTPLATE, "pirate_coat", PirateArmorModels::coat);
		piece(CorpseGearIds.Items.PIRATE_LEGGINGS, "pirate_pants", PirateArmorModels::pants);
		piece(CorpseGearIds.Items.PIRATE_BOOTS, "pirate_boots", PirateArmorModels::boots);
	}

	/** The layer and the texture {@code textures/entities/<name>.png} share the piece's name. */
	private static void piece(Identifier item, String name, ModelLayerRegistry.TexturedLayerDefinitionProvider layer) {
		ModelArmor.register(BuiltInRegistries.ITEM.getValue(item), name, layer, CritterModel.texture(name));
	}

	private static LayerDefinition tricorne() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create()
			.texOffs(28, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 4.0F, 8.0F, new CubeDeformation(0.2F))
			.texOffs(0, 23).addBox(-4.5F, -9.0F, -4.5F, 9.0F, 4.0F, 9.0F, new CubeDeformation(0.2F)), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}

	private static LayerDefinition coat() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create()
			.texOffs(0, 0).addBox(-4.5F, -0.5F, -2.5F, 9.0F, 18.0F, 5.0F, new CubeDeformation(0.1F)), PartPose.ZERO);
		root.addOrReplaceChild("right_arm", CubeListBuilder.create()
			.texOffs(0, 23).addBox(-3.0F, -2.25F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.3F)), PartPose.ZERO);
		root.addOrReplaceChild("left_arm", CubeListBuilder.create()
			.texOffs(0, 23).mirror().addBox(-1.0F, -2.25F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.3F)), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}

	/** The original model also had a belt box, but never attached it to the wearer, so it was never drawn. */
	private static LayerDefinition pants() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("right_leg", CubeListBuilder.create()
			.texOffs(0, 7).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.2F)), PartPose.ZERO);
		root.addOrReplaceChild("left_leg", CubeListBuilder.create()
			.texOffs(0, 7).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.2F)), PartPose.ZERO);
		return LayerDefinition.create(mesh, 32, 32);
	}

	private static LayerDefinition boots() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("right_leg", CubeListBuilder.create()
			.texOffs(0, 0).addBox(-2.0F, 7.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.3F)), PartPose.ZERO);
		root.addOrReplaceChild("left_leg", CubeListBuilder.create()
			.texOffs(0, 0).mirror().addBox(-2.0F, 7.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.3F)), PartPose.ZERO);
		return LayerDefinition.create(mesh, 16, 16);
	}

	private PirateArmorModels() {}
}
