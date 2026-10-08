package com.morecritters.fabric.client.module.critterlings_e;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.morecritters.fabric.client.core.CritterRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/** A critter renderer that turns the whole model ({@code root} bone) where the creature looks, as the cobble's and lightfly's models did. */
class LookingRenderer<T extends LivingEntity & GeoAnimatable> extends CritterRenderer<T> {
	LookingRenderer(EntityRendererProvider.Context context, String assetName, float shadowRadius, float scale) {
		super(context, assetName, shadowRadius, scale);
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
		super.adjustModelBonesForRender(renderPassInfo, snapshots);
		LivingEntityRenderState state = renderPassInfo.renderState();
		snapshots.ifPresent("root", root -> root
			.setRotX(state.xRot * Mth.DEG_TO_RAD)
			.setRotY(state.yRot * Mth.DEG_TO_RAD));
	}
}
