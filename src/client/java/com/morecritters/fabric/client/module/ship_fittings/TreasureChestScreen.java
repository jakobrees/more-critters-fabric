package com.morecritters.fabric.client.module.ship_fittings;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.module.ship_fittings.TreasureChestMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** The treasure chest's screen: the original's background, a "Treasure Chest" and an "Inventory" label. */
public class TreasureChestScreen extends AbstractContainerScreen<TreasureChestMenu> {
	private static final Identifier BACKGROUND = MoreCritters.id("textures/screens/treasure_chest_gui.png");
	private static final int WIDTH = 176, HEIGHT = 166, LABEL_COLOUR = -12829636;
	private static final Component CHEST_LABEL = Component.translatable("gui.more_critters.treasure_chest_gui.label_treasure_chest");
	private static final Component INVENTORY_LABEL = Component.translatable("gui.more_critters.treasure_chest_gui.label_inventory");

	public TreasureChestScreen(TreasureChestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, WIDTH, HEIGHT);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F, WIDTH, HEIGHT, WIDTH, HEIGHT);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(this.font, INVENTORY_LABEL, 7, 73, LABEL_COLOUR, false);
		graphics.text(this.font, CHEST_LABEL, 52, 6, LABEL_COLOUR, false);
	}
}
