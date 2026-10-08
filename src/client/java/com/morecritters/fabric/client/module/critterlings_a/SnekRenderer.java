package com.morecritters.fabric.client.module.critterlings_a;

import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.critterlings_a.SnekEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/** The snek, its head turned where it looks, as the original model did. */
public class SnekRenderer extends CritterRenderer<SnekEntity> {
	public SnekRenderer(EntityRendererProvider.Context context) {
		super(context, "snek", 0.2F, 1.0F);
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
