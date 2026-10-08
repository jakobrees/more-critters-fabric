package com.morecritters.fabric.client.module.warptrap;

import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.warptrap.WarptrapEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/** The warptrap at 1.2x, with its head turned towards where it is looking, as the original model did. */
public class WarptrapRenderer extends CritterRenderer<WarptrapEntity> {
	public WarptrapRenderer(EntityRendererProvider.Context context) {
		super(context, "warptrap", 0.5F, 1.2F);
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
		super.adjustModelBonesForRender(renderPassInfo, snapshots);
		LivingEntityRenderState state = renderPassInfo.renderState();
		snapshots.ifPresent("head", head -> head
			.setRotX(state.xRot * Mth.DEG_TO_RAD)
			.setRotY(state.yRot * Mth.DEG_TO_RAD));
	}
}
