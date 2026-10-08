package com.morecritters.fabric.client.core;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.GeoItems;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

/**
 * Registers a GeckoLib renderer for an item that implements {@code GeoItem}, using the original
 * mod's asset layout: model {@code geckolib/models/<name>.geo.json}, animations
 * {@code geckolib/animations/<name>.animation.json}, texture {@code textures/<texture>.png}.
 */
public final class GeoItemRenderers {
	/** @param texture path under {@code textures/}, e.g. {@code "item/tazegun"} or {@code "block/gravedigger_jar"} */
	public static <T extends Item & GeoAnimatable> void register(T item, String modelName, String texture) {
		GeoModel<T> model = new ItemModel<>(modelName, texture);
		GeoItems.registerRenderer(item, new GeoRenderProvider() {
			private @Nullable GeoItemRenderer<T> renderer;

			@Override
			public GeoItemRenderer<T> getGeoItemRenderer() {
				if (renderer == null) renderer = new GeoItemRenderer<>(model);
				return renderer;
			}
		});
	}

	private static final class ItemModel<T extends GeoAnimatable> extends GeoModel<T> {
		private final Identifier model, texture;

		ItemModel(String modelName, String texture) {
			this.model = MoreCritters.id(modelName);
			this.texture = MoreCritters.id("textures/" + texture + ".png");
		}

		@Override
		public Identifier getModelResource(GeoRenderState renderState) {
			return model;
		}

		@Override
		public Identifier getTextureResource(GeoRenderState renderState) {
			return texture;
		}

		@Override
		public Identifier getAnimationResource(T animatable) {
			return model;
		}
	}

	private GeoItemRenderers() {}
}
