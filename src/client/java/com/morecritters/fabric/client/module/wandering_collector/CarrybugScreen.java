package com.morecritters.fabric.client.module.wandering_collector;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.module.wandering_collector.CarrybugMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** The carrybug's chest screen: the original's 201x183 background and a "Carrybug" label. */
public class CarrybugScreen extends AbstractContainerScreen<CarrybugMenu> {
	private static final Identifier BACKGROUND = MoreCritters.id("textures/screens/carrybug_gui.png");
	private static final int WIDTH = 201, HEIGHT = 183;
	private static final Component LABEL = Component.translatable("gui.more_critters.carrybug_gui.label_carrybug");

	public CarrybugScreen(CarrybugMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, WIDTH, HEIGHT);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F, WIDTH, HEIGHT, WIDTH, HEIGHT);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		// The original shows only its own label, no "Inventory" caption.
		graphics.text(this.font, LABEL, 14, 8, -12829636, false);
	}
}
