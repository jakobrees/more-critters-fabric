package com.morecritters.fabric.client.module.balloon_rat;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.client.core.SpriteParticle;
import com.morecritters.fabric.client.core.SpriteParticle.Settings;
import com.morecritters.fabric.module.balloon_rat.BalloonRatEffects;
import com.morecritters.fabric.module.balloon_rat.BalloonRatModule;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/** The hallucination and balloon-rat stripe particles, and the pink haze over the screen of a hallucinating player. */
final class BalloonRatEffectsClient {
	private static final Identifier HAZE = MoreCritters.id("textures/screens/hallucinazium_shade_animated.png");
	/** The haze texture is a vertical strip of 512x512 frames, each shown for 8 ticks (its .mcmeta). */
	private static final int HAZE_SIZE = 512, HAZE_FRAMES = 13, HAZE_FRAME_TICKS = 8;
	private static final int HAZE_OFFSET_X = -254, HAZE_OFFSET_Y = -186;

	static void register() {
		SpriteParticle.register(BalloonRatModule.PINK_EYE, Settings.of(0.2F, 3.0F, 18, 0.04F, true, 1.0, 19, 1).fullBright());
		SpriteParticle.register(BalloonRatModule.PINK_SPIRAL, Settings.of(0.2F, 3.0F, 55, 0.0F, true, 1.0, 8, 1).translucentSheet());
		SpriteParticle.register(BalloonRatModule.MEDIC_STRIPE, Settings.of(0.2F, 2.0F, 6, -0.2F, false, 1.0, 7, 1).fullBright());
		SpriteParticle.register(BalloonRatModule.SOLDIER_STRIPE, Settings.of(0.2F, 2.0F, 6, -0.2F, true, 1.0, 7, 1).fullBright());

		HudElementRegistry.attachElementBefore(VanillaHudElements.MISC_OVERLAYS, MoreCritters.id("hallucinazium_haze"),
			(graphics, tickCounter) -> drawHaze(graphics));
	}

	/** Under hallucinazium, a slowly shifting pink haze covers the middle of the screen. */
	private static void drawHaze(GuiGraphicsExtractor graphics) {
		Player player = Minecraft.getInstance().player;
		if (player == null || !player.hasEffect(BalloonRatEffects.HALLUCINAZIUM)) return;
		int frame = player.tickCount / HAZE_FRAME_TICKS % HAZE_FRAMES;
		int x = graphics.guiWidth() / 2 + HAZE_OFFSET_X, y = graphics.guiHeight() / 2 + HAZE_OFFSET_Y;
		graphics.blit(RenderPipelines.GUI_TEXTURED, HAZE, x, y, 0.0F, frame * HAZE_SIZE, HAZE_SIZE, HAZE_SIZE, HAZE_SIZE, HAZE_SIZE * HAZE_FRAMES);
	}

	private BalloonRatEffectsClient() {}
}
