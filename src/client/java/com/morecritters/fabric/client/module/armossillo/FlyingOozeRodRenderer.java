package com.morecritters.fabric.client.module.armossillo;

import com.geckolib.renderer.layer.builtin.TextureLayerGeoLayer;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.armossillo.FlyingOozeRodEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** The thrown ooze rod: its model drawn normally, then again full-bright so it glows in the dark. */
public class FlyingOozeRodRenderer extends CritterRenderer<FlyingOozeRodEntity> {
	private static final String ASSET = "ooze_rod";
	private static final Identifier GLOW_TEXTURE = MoreCritters.id("textures/entities/" + ASSET + ".png");

	public FlyingOozeRodRenderer(EntityRendererProvider.Context context) {
		super(context, ASSET, 0.0F, 1.0F);
		withRenderLayer(renderer -> new TextureLayerGeoLayer<>(renderer, GLOW_TEXTURE, RenderTypes::eyes));
	}
}
