package com.morecritters.fabric.client.module.nightshroom;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterModel;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.client.core.SpriteParticle;
import com.morecritters.fabric.module.nightshroom.NightshroomModule;
import com.morecritters.fabric.module.nightshroom.RotSplashEntity;
import com.morecritters.fabric.module.nightshroom.RotZombieEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/** Rendering for the nightshroom module: the shrooms and their rot, the rot particle and the rot splatters on screen. */
public final class NightshroomClientModule implements ClientModule {
	/** Three splatters (200x200) placed around the middle of the screen while covered in rot. */
	private static final Identifier[] SPLATTERS = {
		MoreCritters.id("textures/screens/rot_splatter1.png"),
		MoreCritters.id("textures/screens/rot_splatter2.png"),
		MoreCritters.id("textures/screens/rot_splatter3.png"),
	};
	private static final int[][] SPLATTER_OFFSETS = {{-221, -129}, {21, -118}, {-86, -74}};
	private static final int SPLATTER_SIZE = 200;

	@Override
	public void registerClient() {
		EntityRendererRegistry.register(NightshroomModule.NIGHTSHROOM, context ->
			new ShroomRenderer<>(context, "nightshroom", 0.8F, 0.8F).headBone("neck_main").glow("nightshroom_glow"));
		EntityRendererRegistry.register(NightshroomModule.FRIGHTSHROOM, context ->
			new ShroomRenderer<>(context, "frightshroom", 0.8F, 0.9F).headBone("neck_center").glow("frightshroom_glow"));
		EntityRendererRegistry.register(NightshroomModule.FUNGAL_ZOMBIE, context ->
			new ShroomRenderer<>(context, "fungal_zombie", 0.5F, 1.0F).headBone("head"));
		EntityRendererRegistry.register(NightshroomModule.ROT_ZOMBIE, context ->
			new ShroomRenderer<RotZombieEntity>(context, "rot_zombie", 0.5F, 1.0F).headBone("head_r").glow("rot_zombie_glow").growing(RotZombieEntity::growth));
		EntityRendererRegistry.register(NightshroomModule.ROT_SPLASH, context ->
			new ShroomRenderer<RotSplashEntity>(context, "rot_splash", 0.0F, 1.0F).growing(RotSplashEntity::growth));
		EntityRendererRegistry.register(NightshroomModule.MORI_ROOTS, context ->
			new CritterRenderer<>(context, new CritterModel<>("mori_roots", "mori_roots", "mori_root"), 0.3F, 1.0F));
		// The skeleton's texture follows the stew poured on it (TextureVariants).
		EntityRendererRegistry.register(NightshroomModule.ANCIENT_SKELETON, context ->
			new CritterRenderer<>(context, new CritterModel<>("ancien_skeleton", "ancien_skeleton", "ancient_skeleton"), 0.9F, 1.0F));
		ModelLayerRegistry.registerModelLayer(RotPieceRenderer.LAYER, RotPieceRenderer.Model::createBodyLayer);
		EntityRendererRegistry.register(NightshroomModule.ROT_PIECE, RotPieceRenderer::new);

		// Six frames, two ticks each, falling fast. The original also spun it slowly (0.02 rad a tick).
		SpriteParticle.register(NightshroomModule.ROT_PARTICLE, SpriteParticle.Settings.of(0.2F, 2.0F, 10, 0.6F, true, 1.0, 6, 2).spin(0.02F, 0.0F));

		HudElementRegistry.attachElementBefore(VanillaHudElements.MISC_OVERLAYS, MoreCritters.id("rot_splatter"),
			(graphics, tickCounter) -> drawSplatters(graphics));
	}

	private static void drawSplatters(GuiGraphicsExtractor graphics) {
		Player player = Minecraft.getInstance().player;
		if (player == null || !player.hasEffect(NightshroomModule.ROT_COVERED)) return;
		int centerX = graphics.guiWidth() / 2, centerY = graphics.guiHeight() / 2;
		for (int i = 0; i < SPLATTERS.length; i++) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, SPLATTERS[i], centerX + SPLATTER_OFFSETS[i][0], centerY + SPLATTER_OFFSETS[i][1],
				0.0F, 0.0F, SPLATTER_SIZE, SPLATTER_SIZE, SPLATTER_SIZE, SPLATTER_SIZE);
		}
	}
}
