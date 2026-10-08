package com.morecritters.fabric.client.module.mightshroom;

import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.TextureLayerGeoLayer;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.mightshroom.MightshroomEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/** The mightshroom with its full-bright glow layer; its neck follows where it looks, like the original. */
public class MightshroomRenderer extends CritterRenderer<MightshroomEntity> {
	public MightshroomRenderer(EntityRendererProvider.Context context) {
		super(context, "mightshroom", 0.8F, 1.0F);
		withRenderLayer(new TextureLayerGeoLayer<>(this, MoreCritters.id("textures/entities/mightshroom_glow.png"), RenderTypes::eyes));
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
		LivingEntityRenderState state = renderPassInfo.renderState();
		snapshots.get("neck_main").ifPresent(neck -> neck
			.setRotX(state.xRot * Mth.DEG_TO_RAD)
			.setRotY(state.yRot * Mth.DEG_TO_RAD));
	}
}
