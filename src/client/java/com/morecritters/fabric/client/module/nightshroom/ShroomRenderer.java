package com.morecritters.fabric.client.module.nightshroom;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.TextureLayerGeoLayer;
import com.morecritters.fabric.client.core.CritterModel;
import com.morecritters.fabric.client.core.CritterRenderer;
import java.util.function.ToDoubleFunction;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/**
 * The renderer the shrooms and their rot share: a {@link CritterRenderer} that can also turn a
 * head bone toward where the creature looks, draw a glowing overlay texture, and scale the model
 * by how far the creature has grown.
 */
final class ShroomRenderer<T extends LivingEntity & GeoAnimatable> extends CritterRenderer<T> {
	private static final DataTicket<Float> GROWTH = DataTicket.create("more_critters_shroom_growth", Float.class);

	private @Nullable String headBone;
	private @Nullable ToDoubleFunction<T> growth;

	ShroomRenderer(EntityRendererProvider.Context context, String name, float shadowRadius, float scale) {
		super(context, name, shadowRadius, scale);
	}

	/** The bone that follows the creature's gaze (pitch and head yaw). */
	ShroomRenderer<T> headBone(String bone) {
		this.headBone = bone;
		return this;
	}

	/** Draws {@code textures/entities/<texture>.png} again over the model, full-bright. */
	ShroomRenderer<T> glow(String texture) {
		withRenderLayer(new TextureLayerGeoLayer<>(this, CritterModel.texture(texture), RenderTypes::eyes));
		return this;
	}

	/** Scales the model by the creature's current size. */
	ShroomRenderer<T> growing(ToDoubleFunction<T> growth) {
		this.growth = growth;
		return this;
	}

	@Override
	public void addRenderData(T animatable, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
		super.addRenderData(animatable, relatedObject, renderState, partialTick);
		if (this.growth != null) renderState.addGeckolibData(GROWTH, (float) this.growth.applyAsDouble(animatable));
	}

	@Override
	public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> pass, float widthScale, float heightScale) {
		float size = this.growth != null ? pass.getOrDefaultGeckolibData(GROWTH, 1.0F) : 1.0F;
		super.scaleModelForRender(pass, widthScale * size, heightScale * size);
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> pass, BoneSnapshots snapshots) {
		super.adjustModelBonesForRender(pass, snapshots);
		if (this.headBone == null) return;
		LivingEntityRenderState state = pass.renderState();
		snapshots.get(this.headBone).ifPresent(head -> head
			.setRotX(state.xRot * Mth.DEG_TO_RAD)
			.setRotY(state.yRot * Mth.DEG_TO_RAD));
	}
}
