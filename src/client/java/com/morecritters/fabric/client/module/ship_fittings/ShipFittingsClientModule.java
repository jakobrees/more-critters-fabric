package com.morecritters.fabric.client.module.ship_fittings;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.GeoItemRenderers;
import com.morecritters.fabric.module.ship_fittings.GeoBlockItem;
import com.morecritters.fabric.module.ship_fittings.Infusion;
import com.morecritters.fabric.module.ship_fittings.ShipFittingsModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Cannon balls drawn as their items, the GeckoLib ship wheel and jolly roger (placed and in hand), and both screens. */
public final class ShipFittingsClientModule implements ClientModule {
	@Override
	public void registerClient() {
		var projectiles = ShipFittingsModule.CANNON_BALL_PROJECTILES;
		EntityRendererRegistry.register(projectiles.get(Infusion.NONE), ThrownItemRenderer::new);
		EntityRendererRegistry.register(projectiles.get(Infusion.COLD), ThrownItemRenderer::new);
		EntityRendererRegistry.register(projectiles.get(Infusion.FIRE), ThrownItemRenderer::new);
		EntityRendererRegistry.register(projectiles.get(Infusion.SLIME), ThrownItemRenderer::new);
		EntityRendererRegistry.register(projectiles.get(Infusion.ELECTRIC), ThrownItemRenderer::new);
		EntityRendererRegistry.register(projectiles.get(Infusion.COMBUSTING), ThrownItemRenderer::new);

		BlockEntityRenderers.register(ShipFittingsModule.SHIP_WHEEL_BLOCK_ENTITY, translucent(new ShipFittingsModels.ShipWheel()));
		BlockEntityRenderers.register(ShipFittingsModule.TATTERED_JOLLY_ROGER_BLOCK_ENTITY, translucent(new ShipFittingsModels.JollyRoger()));
		GeoItemRenderers.register((GeoBlockItem) ShipFittingsModule.SHIP_WHEEL_ITEM, "ship_wheel", "block/ship_wheel");
		GeoItemRenderers.register((GeoBlockItem) ShipFittingsModule.TATTERED_JOLLY_ROGER_ITEM, "tattered_jolly_roger", "block/tattered_jolly_rodger_large");

		MenuScreens.register(ShipFittingsModule.CANNON_MENU, CannonScreen::new);
		MenuScreens.register(ShipFittingsModule.TREASURE_CHEST_MENU, TreasureChestScreen::new);
	}

	/** Both blocks were drawn translucent in the original. */
	private static <T extends BlockEntity & GeoAnimatable> BlockEntityRendererProvider<T, BlockEntityRenderState> translucent(GeoModel<T> model) {
		return context -> new GeoBlockRenderer<>(context, model) {
			@Override
			public RenderType getRenderType(BlockEntityRenderState renderState, Identifier texture) {
				return RenderTypes.entityTranslucent(texture);
			}
		};
	}
}
