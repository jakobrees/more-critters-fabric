package com.morecritters.fabric.client.module.bouncelizard;

import com.geckolib.renderer.base.RenderPassInfo;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.bouncelizard.BouncelizardEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** The bouncelizard, with hatchlings drawn at a third of an adult's size as in the original. */
public class BouncelizardRenderer extends CritterRenderer<BouncelizardEntity> {
	private static final float BABY_SCALE = 0.3F;

	public BouncelizardRenderer(EntityRendererProvider.Context context) {
		super(context, "bouncelizard", 0.5F, 1.0F);
	}

	@Override
	public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, float widthScale, float heightScale) {
		float age = renderPassInfo.renderState().isBaby ? BABY_SCALE : 1.0F;
		super.scaleModelForRender(renderPassInfo, widthScale * age, heightScale * age);
	}
}
