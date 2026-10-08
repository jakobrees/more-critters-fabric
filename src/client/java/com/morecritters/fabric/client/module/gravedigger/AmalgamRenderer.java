package com.morecritters.fabric.client.module.gravedigger;

import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.gravedigger.AmalgamEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/** The Amalgam; its cluster of heads follows where it is looking. */
public class AmalgamRenderer extends CritterRenderer<AmalgamEntity> {
	public AmalgamRenderer(EntityRendererProvider.Context context) {
		super(context, "amalgam", 0.5F, 1.0F);
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
		LivingEntityRenderState state = renderPassInfo.renderState();
		snapshots.get("heads").ifPresent(heads -> heads
			.setRotX(state.xRot * Mth.DEG_TO_RAD)
			.setRotY(state.yRot * Mth.DEG_TO_RAD));
	}
}
