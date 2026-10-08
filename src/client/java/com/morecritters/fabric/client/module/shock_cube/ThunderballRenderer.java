package com.morecritters.fabric.client.module.shock_cube;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.client.core.CritterModel;
import com.morecritters.fabric.module.shock_cube.ThunderballProjectile;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * A thunderball in flight: the original's electric bullet model, a small cube inside a larger
 * one, turned along its path and lit by the world like any entity (not full bright).
 */
final class ThunderballRenderer extends EntityRenderer<ThunderballProjectile, ThunderballRenderer.State> {
	static final ModelLayerLocation LAYER = new ModelLayerLocation(MoreCritters.id("electric_bullet"), "main");
	private static final Identifier TEXTURE = CritterModel.texture("electric_bullet");

	private final Model model;

	ThunderballRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new Model(context.bakeLayer(LAYER));
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.rotateDegrees(Axis.YP, state.yRot - 90.0F);
		poseStack.rotateDegrees(Axis.ZP, 90.0F + state.xRot);
		collector.submitModel(this.model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(ThunderballProjectile entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.xRot = entity.getXRot(partialTicks);
		state.yRot = entity.getYRot(partialTicks);
	}

	static final class State extends EntityRenderState {
		float yRot;
		float xRot;
	}

	/** A 4x4x4 core in a 6x6x6 shell, off-centre as in the original model. */
	static final class Model extends EntityModel<EntityRenderState> {
		Model(ModelPart root) {
			super(root, RenderTypes::entityCutout);
		}

		static LayerDefinition createBodyLayer() {
			MeshDefinition mesh = new MeshDefinition();
			mesh.getRoot().addOrReplaceChild("bone",
				CubeListBuilder.create()
					.texOffs(0, 12).addBox(-10.0F, -10.0F, 6.0F, 4.0F, 4.0F, 4.0F)
					.texOffs(0, 0).addBox(-11.0F, -11.0F, 5.0F, 6.0F, 6.0F, 6.0F),
				PartPose.offset(8.0F, 24.0F, -8.0F));
			return LayerDefinition.create(mesh, 32, 32);
		}
	}
}
