package com.morecritters.fabric.client.module.ship_fittings;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.module.ship_fittings.CannonMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** The cannon's screen: the original's background with its ammunition slot and a "Cannon" label. */
public class CannonScreen extends AbstractContainerScreen<CannonMenu> {
	private static final Identifier BACKGROUND = MoreCritters.id("textures/screens/cannon_gui.png");
	private static final int WIDTH = 176, HEIGHT = 166, LABEL_COLOUR = -12829636;
	private static final Component LABEL = Component.translatable("gui.more_critters.cannon_gui.label_cannon");

	public CannonScreen(CannonMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, WIDTH, HEIGHT);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F, WIDTH, HEIGHT, WIDTH, HEIGHT);
	}

	/** Only the cannon's label; the original drew neither the title nor "Inventory". */
	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(this.font, LABEL, 72, 20, LABEL_COLOUR, false);
	}
}
