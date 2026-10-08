package com.morecritters.fabric.client.module.dripper;

import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.morecritters.fabric.client.core.CritterModel;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.dripper.SlashEffectEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/** Draws the slash at twice its model size, tilted and turned with the entity's head like the original. */
public class SlashEffectRenderer extends CritterRenderer<SlashEffectEntity> {
	public SlashEffectRenderer(EntityRendererProvider.Context context) {
		// Its texture changes as it fades (slash_effect1..5); the model's default is the first.
		super(context, new CritterModel<>("slash_effect", "slash_effect", "slash_effect1"), 0.5F, 2.0F);
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
		LivingEntityRenderState state = renderPassInfo.renderState();
		snapshots.get("root1").ifPresent(root -> root
			.setRotX(state.xRot * Mth.DEG_TO_RAD)
			.setRotY(state.yRot * Mth.DEG_TO_RAD));
	}
}
