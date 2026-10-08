package com.morecritters.fabric.client.module.ship_fittings;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.module.ship_fittings.JollyRogerBlockEntity;
import com.morecritters.fabric.module.ship_fittings.ShipWheelBlockEntity;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** The GeckoLib models of the placed ship wheel and jolly roger; their textures live with the block textures. */
final class ShipFittingsModels {
	static final class ShipWheel extends GeoModel<ShipWheelBlockEntity> {
		private static final Identifier NAME = MoreCritters.id("ship_wheel");
		private static final Identifier TEXTURE = MoreCritters.id("textures/block/ship_wheel.png");

		@Override
		public Identifier getModelResource(GeoRenderState renderState) {
			return NAME;
		}

		@Override
		public Identifier getTextureResource(GeoRenderState renderState) {
			return TEXTURE;
		}

		@Override
		public Identifier getAnimationResource(ShipWheelBlockEntity animatable) {
			return NAME;
		}
	}

	/** The small flag, or the large one ("_2") when hung on a pole; both share one texture. */
	static final class JollyRoger extends GeoModel<JollyRogerBlockEntity> {
		private static final DataTicket<Boolean> LARGE = DataTicket.create("more_critters_jolly_roger_large", Boolean.class);
		private static final Identifier SMALL_NAME = MoreCritters.id("tattered_jolly_roger");
		private static final Identifier LARGE_NAME = MoreCritters.id("tattered_jolly_roger_2");
		private static final Identifier TEXTURE = MoreCritters.id("textures/block/tattered_jolly_rodger_large.png");

		@Override
		public void addAdditionalStateData(JollyRogerBlockEntity animatable, @Nullable Object relatedObject, GeoRenderState renderState) {
			renderState.addGeckolibData(LARGE, JollyRogerBlockEntity.isLarge(animatable.getBlockState()));
		}

		@Override
		public Identifier getModelResource(GeoRenderState renderState) {
			return Boolean.TRUE.equals(renderState.getGeckolibData(LARGE)) ? LARGE_NAME : SMALL_NAME;
		}

		@Override
		public Identifier getTextureResource(GeoRenderState renderState) {
			return TEXTURE;
		}

		@Override
		public Identifier getAnimationResource(JollyRogerBlockEntity animatable) {
			return JollyRogerBlockEntity.isLarge(animatable.getBlockState()) ? LARGE_NAME : SMALL_NAME;
		}
	}

	private ShipFittingsModels() {}
}
