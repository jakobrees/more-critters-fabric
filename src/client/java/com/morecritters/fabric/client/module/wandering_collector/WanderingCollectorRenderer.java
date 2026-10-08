package com.morecritters.fabric.client.module.wandering_collector;

import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.wandering_collector.WanderingCollectorEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/** The collector at 0.95x in his biome outfit, his head turned where he looks, as the original model did. */
public class WanderingCollectorRenderer extends CritterRenderer<WanderingCollectorEntity> {
	public WanderingCollectorRenderer(EntityRendererProvider.Context context) {
		super(context, "wandering_collector", 0.5F, 0.95F);
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
