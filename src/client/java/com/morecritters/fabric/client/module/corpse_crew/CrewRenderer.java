package com.morecritters.fabric.client.module.corpse_crew;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.morecritters.fabric.client.core.CritterModel;
import com.morecritters.fabric.client.core.CritterRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/** A corpse crew member; its head bone follows where it is looking, as in the original models. */
public class CrewRenderer<T extends LivingEntity & GeoAnimatable> extends CritterRenderer<T> {
	public CrewRenderer(EntityRendererProvider.Context context, CritterModel<T> model, float shadowRadius, float scale) {
		super(context, model, shadowRadius, scale);
	}

	public CrewRenderer(EntityRendererProvider.Context context, String assetName, float shadowRadius, float scale) {
		super(context, assetName, shadowRadius, scale);
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
		LivingEntityRenderState state = renderPassInfo.renderState();
		snapshots.get("head").ifPresent(head -> head
			.setRotX(state.xRot * Mth.DEG_TO_RAD)
			.setRotY(state.yRot * Mth.DEG_TO_RAD));
	}
}
