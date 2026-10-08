package com.morecritters.fabric.client.module.critterling_system;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.module.critterling_system.EvolutionTableMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The evolution table screen. The arrow from the sack to the result fills in stages as the wanted
 * ingredients go in: the first part once the first ingredient matches, the second once the second
 * does too, the whole arrow when every ingredient is in place (the original's
 * {@code ArrowDisplay1..3Procedure}).
 */
public class EvolutionTableScreen extends AbstractContainerScreen<EvolutionTableMenu> {
	private static final Identifier BACKGROUND = MoreCritters.id("textures/screens/evolution_table_gui.png");
	private static final Identifier[] ARROWS = {
		MoreCritters.id("textures/screens/evolution_arrow_1.png"),
		MoreCritters.id("textures/screens/evolution_arrow_2.png"),
		MoreCritters.id("textures/screens/evolution_arrow_3.png")};
	private static final int ARROW_X = 45, ARROW_Y = 20, ARROW_WIDTH = 86, ARROW_HEIGHT = 59;
	private static final int LABEL_COLOUR = -12829636;

	public EvolutionTableScreen(EvolutionTableMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 166);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F,
			this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
		// Drawn in the original's order: full arrow, two thirds, one third.
		if (allIngredientsIn()) drawArrow(graphics, ARROWS[0]);
		if (ingredientsIn(2)) drawArrow(graphics, ARROWS[1]);
		if (ingredientsIn(1)) drawArrow(graphics, ARROWS[2]);
	}

	private void drawArrow(GuiGraphicsExtractor graphics, Identifier arrow) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, arrow, this.leftPos + ARROW_X, this.topPos + ARROW_Y, 0.0F, 0.0F,
			ARROW_WIDTH, ARROW_HEIGHT, ARROW_WIDTH, ARROW_HEIGHT);
	}

	private boolean tableReady() {
		return !item(EvolutionTableMenu.SACK_SLOT).isEmpty() && !item(EvolutionTableMenu.EVOLITE_SLOT).isEmpty();
	}

	/** The first {@code count} wanted ingredients are shown and each lies in its slot. */
	private boolean ingredientsIn(int count) {
		if (!tableReady()) return false;
		for (int i = 0; i < count; i++) {
			ItemStack wanted = item(EvolutionTableMenu.PREVIEW_SLOTS[i]);
			if (wanted.isEmpty() || !item(EvolutionTableMenu.INGREDIENT_SLOTS[i]).is(wanted.getItem())) return false;
		}
		return true;
	}

	/** Every ingredient slot holds what its preview shows (an unused third slot stays empty). */
	private boolean allIngredientsIn() {
		if (!tableReady() || item(EvolutionTableMenu.PREVIEW_SLOTS[0]).isEmpty() && item(EvolutionTableMenu.PREVIEW_SLOTS[1]).isEmpty()) return false;
		for (int i = 0; i < EvolutionTableMenu.INGREDIENT_SLOTS.length; i++) {
			if (item(EvolutionTableMenu.INGREDIENT_SLOTS[i]).getItem() != item(EvolutionTableMenu.PREVIEW_SLOTS[i]).getItem()) return false;
		}
		return true;
	}

	private ItemStack item(int menuSlot) {
		return this.menu.itemIn(menuSlot);
	}

	/** Only the table's name; the original drew neither the title nor "Inventory". */
	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(this.font, Component.translatable("gui.more_critters.evolution_table_gui.label_evolution_table"), 7, 6, LABEL_COLOUR, false);
	}
}
