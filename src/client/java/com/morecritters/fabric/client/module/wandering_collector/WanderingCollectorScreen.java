package com.morecritters.fabric.client.module.wandering_collector;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.module.wandering_collector.WanderingCollectorMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** The collector's trade screen: item slot, arrow, price slot and a "Trade All" button, laid out as in the original. */
public class WanderingCollectorScreen extends AbstractContainerScreen<WanderingCollectorMenu> {
	private static final Identifier BACKGROUND = MoreCritters.id("textures/screens/wandering_trader_gui.png");
	private static final Identifier ARROW = MoreCritters.id("textures/screens/trade_arrow.png");
	private static final int LABEL_COLOUR = -12829636;

	public WanderingCollectorScreen(WanderingCollectorMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 166);
	}

	@Override
	protected void init() {
		super.init();
		this.addRenderableWidget(Button.builder(Component.translatable("gui.more_critters.wandering_trader_gui.button_trade"),
				button -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, WanderingCollectorMenu.TRADE_BUTTON))
			.bounds(this.leftPos + 62, this.topPos + 55, 51, 20).build());
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F,
			this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
		graphics.blit(RenderPipelines.GUI_TEXTURED, ARROW, this.leftPos + 77, this.topPos + 29, 0.0F, 0.0F, 22, 15, 22, 15);
	}

	/** Only the collector's name; the original drew neither the title nor "Inventory". */
	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(this.font, Component.translatable("gui.more_critters.wandering_trader_gui.label_wandering_collector"), 41, 10, LABEL_COLOUR, false);
	}
}
