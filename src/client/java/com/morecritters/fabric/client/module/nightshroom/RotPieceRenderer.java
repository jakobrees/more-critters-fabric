package com.morecritters.fabric.client.module.nightshroom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.client.core.CritterModel;
import com.morecritters.fabric.module.nightshroom.RotPieceEntity;
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

/** A rot piece in flight: a small cube of rot turned along its path. */
final class RotPieceRenderer extends EntityRenderer<RotPieceEntity, RotPieceRenderer.State> {
	static final ModelLayerLocation LAYER = new ModelLayerLocation(MoreCritters.id("rot_piece"), "main");
	private static final Identifier TEXTURE = CritterModel.texture("rot_piece");

	private final Model model;

	RotPieceRenderer(EntityRendererProvider.Context context) {
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
	public void extractRenderState(RotPieceEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.xRot = entity.getXRot(partialTicks);
		state.yRot = entity.getYRot(partialTicks);
	}

	static final class State extends EntityRenderState {
		float yRot;
		float xRot;
	}

	/** One 5x5x5 cube, off-centre as in the original model. */
	static final class Model extends EntityModel<EntityRenderState> {
		Model(ModelPart root) {
			super(root, RenderTypes::entityCutout);
		}

		static LayerDefinition createBodyLayer() {
			MeshDefinition mesh = new MeshDefinition();
			mesh.getRoot().addOrReplaceChild("bb_main",
				CubeListBuilder.create().texOffs(0, 0).addBox(0.5F, -24.5F, -2.5F, 5.0F, 5.0F, 5.0F),
				PartPose.offset(0.0F, 24.0F, 0.0F));
			return LayerDefinition.create(mesh, 32, 32);
		}
	}
}
