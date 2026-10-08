package com.morecritters.fabric.client.core;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * The renderer every critter uses: a {@link CritterModel}, a shadow size and a uniform scale,
 * drawn translucent as the original mod draws all of its creatures.
 */
public class CritterRenderer<T extends LivingEntity & GeoAnimatable> extends GeoEntityRenderer<T, LivingEntityRenderState> {
	public CritterRenderer(EntityRendererProvider.Context context, GeoModel<T> model, float shadowRadius, float scale) {
		super(context, model);
		this.shadowRadius = shadowRadius;
		withScale(scale);
	}

	public CritterRenderer(EntityRendererProvider.Context context, String assetName, float shadowRadius, float scale) {
		this(context, new CritterModel<>(assetName), shadowRadius, scale);
	}

	@Override
	public RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
		return RenderTypes.entityTranslucent(texture);
	}
}
