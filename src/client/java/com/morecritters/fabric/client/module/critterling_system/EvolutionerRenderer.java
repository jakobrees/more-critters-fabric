package com.morecritters.fabric.client.module.critterling_system;

import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.critterling_system.EvolutionerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/** The evolutioner at 0.95x, arm raised while casting (texture), head turned where he looks, as the original model did. */
public class EvolutionerRenderer extends CritterRenderer<EvolutionerEntity> {
	public EvolutionerRenderer(EntityRendererProvider.Context context) {
		super(context, "evolutioner", 0.5F, 0.95F);
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
		super.adjustModelBonesForRender(renderPassInfo, snapshots);
		LivingEntityRenderState state = renderPassInfo.renderState();
		snapshots.ifPresent("head_root", head -> head
			.setRotX(state.xRot * Mth.DEG_TO_RAD)
			.setRotY(state.yRot * Mth.DEG_TO_RAD));
	}
}
