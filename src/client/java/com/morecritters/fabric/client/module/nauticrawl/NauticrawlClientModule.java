package com.morecritters.fabric.client.module.nauticrawl;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.client.ClientModule;
import com.morecritters.fabric.client.core.CritterModel;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.nauticrawl.NauticrawlItems;
import com.morecritters.fabric.module.nauticrawl.NauticrawlModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

/** Rendering for the nauticrawls and blood bubbles, the worn nautical helmet, and the view from inside it when tucked in. */
public final class NauticrawlClientModule implements ClientModule {
	private static final Identifier HELMET_BLUR = MoreCritters.id("textures/screens/helmet_blur.png");
	private static final int BLUR_WIDTH = 1000, BLUR_HEIGHT = 512;

	@Override
	public void registerClient() {
		EntityRendererRegistry.register(NauticrawlModule.NAUTICRAWL, context -> new CritterRenderer<>(context, "nauticrawl", 0.6F, 1.2F));
		EntityRendererRegistry.register(NauticrawlModule.ZOMBIE_NAUTICRAWL, context ->
			new CritterRenderer<>(context, new CritterModel<>("nauticrawl_zombie"), 0.6F, 1.2F));
		EntityRendererRegistry.register(NauticrawlModule.BUBBLE, context -> new CritterRenderer<>(context, "bubble_entity", 0.3F, 1.0F));
		NauticalHelmetModel.register();
		HudElementRegistry.attachElementBefore(VanillaHudElements.MISC_OVERLAYS, MoreCritters.id("nautical_helmet_blur"),
			(graphics, tickCounter) -> drawHelmetBlur(graphics));
	}

	/** Sneaking in the nautical helmet, the screen is framed by the inside of the shell. */
	private static void drawHelmetBlur(GuiGraphicsExtractor graphics) {
		Player player = Minecraft.getInstance().player;
		if (player == null || !player.isShiftKeyDown() || !player.getItemBySlot(EquipmentSlot.HEAD).is(NauticrawlItems.NAUTICAL_HELMET)) return;
		int x = graphics.guiWidth() / 2 - 496, y = graphics.guiHeight() / 2 - 271;
		graphics.blit(RenderPipelines.GUI_TEXTURED, HELMET_BLUR, x, y, 0.0F, 0.0F, BLUR_WIDTH, BLUR_HEIGHT, BLUR_WIDTH, BLUR_HEIGHT);
	}
}
