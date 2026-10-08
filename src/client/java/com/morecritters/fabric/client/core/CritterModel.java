package com.morecritters.fabric.client.core;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.TextureVariants;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * A GeckoLib model using the original mod's asset layout: model {@code geckolib/models/<name>.geo.json},
 * animations {@code geckolib/animations/<name>.animation.json}, texture {@code textures/entities/<texture>.png}.
 * Entities implementing {@link TextureVariants} choose their texture per frame.
 */
public class CritterModel<T extends Entity & GeoAnimatable> extends GeoModel<T> {
	private static final DataTicket<String> TEXTURE_NAME = DataTicket.create("more_critters_texture_name", String.class);

	private final Identifier model;
	private final Identifier animations;
	private final Identifier defaultTexture;

	/** Model, animations and texture all share {@code name}. */
	public CritterModel(String name) {
		this(name, name, name);
	}

	public CritterModel(String modelName, String animationsName, String textureName) {
		this.model = MoreCritters.id(modelName);
		this.animations = MoreCritters.id(animationsName);
		this.defaultTexture = texture(textureName);
	}

	public static Identifier texture(String name) {
		return MoreCritters.id("textures/entities/" + name + ".png");
	}

	@Override
	public Identifier getModelResource(GeoRenderState renderState) {
		return this.model;
	}

	@Override
	public Identifier getAnimationResource(T animatable) {
		return this.animations;
	}

	@Override
	public Identifier getTextureResource(GeoRenderState renderState) {
		String name = renderState.getGeckolibData(TEXTURE_NAME);
		return name == null ? this.defaultTexture : texture(name);
	}

	@Override
	public void addAdditionalStateData(T animatable, @Nullable Object relatedObject, GeoRenderState renderState) {
		if (animatable instanceof TextureVariants variants) {
			renderState.addGeckolibData(TEXTURE_NAME, variants.textureName());
		}
	}
}
