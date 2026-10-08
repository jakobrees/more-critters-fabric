package com.morecritters.fabric.client.module.critterling_system;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.module.critterling_system.ConfettiPopperBlockEntity;
import net.minecraft.resources.Identifier;

/** The popper's GeckoLib model; its texture lives with the block textures. */
public class ConfettiPopperModel extends GeoModel<ConfettiPopperBlockEntity> {
	private static final Identifier NAME = MoreCritters.id("confetti_popper");
	private static final Identifier TEXTURE = MoreCritters.id("textures/block/confetti_popper.png");

	@Override
	public Identifier getModelResource(GeoRenderState renderState) {
		return NAME;
	}

	@Override
	public Identifier getTextureResource(GeoRenderState renderState) {
		return TEXTURE;
	}

	@Override
	public Identifier getAnimationResource(ConfettiPopperBlockEntity animatable) {
		return NAME;
	}
}
