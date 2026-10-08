package com.morecritters.fabric.client.module.gravedigger;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.module.gravedigger.GravediggerJarBlockEntity;
import net.minecraft.resources.Identifier;

/** The jar's GeckoLib model; its texture lives with the block textures. */
public class GravediggerJarModel extends GeoModel<GravediggerJarBlockEntity> {
	private static final Identifier NAME = MoreCritters.id("gravedigger_jar");
	private static final Identifier TEXTURE = MoreCritters.id("textures/block/gravedigger_jar.png");

	@Override
	public Identifier getModelResource(GeoRenderState renderState) {
		return NAME;
	}

	@Override
	public Identifier getTextureResource(GeoRenderState renderState) {
		return TEXTURE;
	}

	@Override
	public Identifier getAnimationResource(GravediggerJarBlockEntity animatable) {
		return NAME;
	}
}
