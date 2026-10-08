package com.morecritters.fabric.client.module.critterling_system;

import com.geckolib.renderer.GeoBlockRenderer;
import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.client.core.GeoItemRenderers;
import com.morecritters.fabric.client.core.SpriteParticle;
import com.morecritters.fabric.module.critterling_system.ConfettiPopperItem;
import com.morecritters.fabric.module.critterling_system.CritterlingSystemModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

/**
 * Renderers for the critter eater, evolutioner and evolite maw, the confetti popper (block and
 * item), the evolution table screen and the four particles. Critterlings need nothing from here:
 * {@code new CritterRenderer<>(context, "cubefrog", shadow, scale)} picks up their rarity texture
 * through {@code TextureVariants}.
 */
public final class CritterlingSystemClientModule implements ClientModule {
	@Override
	public void registerClient() {
		EntityRendererRegistry.register(CritterlingSystemModule.CRITTER_EATER, context -> new CritterRenderer<>(context, "critter_eater", 0.6F, 1.0F));
		EntityRendererRegistry.register(CritterlingSystemModule.EVOLITE_MAW, context -> new CritterRenderer<>(context, "evolite_maw", 0.0F, 1.0F));
		EntityRendererRegistry.register(CritterlingSystemModule.EVOLUTIONER, EvolutionerRenderer::new);

		BlockEntityRenderers.register(CritterlingSystemModule.CONFETTI_POPPER_BLOCK_ENTITY,
			context -> new GeoBlockRenderer<>(context, new ConfettiPopperModel()));
		GeoItemRenderers.register((ConfettiPopperItem) CritterlingSystemModule.CONFETTI_POPPER_ITEM, "confetti_popper", "block/confetti_popper");

		MenuScreens.register(CritterlingSystemModule.EVOLUTION_TABLE_MENU, EvolutionTableScreen::new);

		// Sprite sheets cycling one frame per tick: boost 10 frames, evolightened 19, zzz 14.
		SpriteParticle.register(CritterlingSystemModule.BOOST_PARTICLE,
			SpriteParticle.Settings.of(0.2F, 1.5F, 9, -0.02F, true, 2.0, 10, 1).translucentSheet());
		SpriteParticle.register(CritterlingSystemModule.EVOLIGHTENED_PARTICLE,
			SpriteParticle.Settings.of(0.2F, 0.2F, 18, -0.01F, true, 1.0, 19, 1).translucentSheet());
		SpriteParticle.register(CritterlingSystemModule.ZZZ_PARTICLE,
			SpriteParticle.Settings.of(0.2F, 2.0F, 13, -0.02F, true, 1.0, 14, 1).translucentSheet());
		ConfettiParticle.register(CritterlingSystemModule.CONFETTI_PARTICLE);
	}
}
