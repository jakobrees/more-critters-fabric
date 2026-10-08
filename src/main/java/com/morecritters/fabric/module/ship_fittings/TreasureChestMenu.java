package com.morecritters.fabric.module.ship_fittings;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The treasure chest's screen (creative players only): its first nine slots as a 3x3 grid above the player's inventory. */
public class TreasureChestMenu extends AbstractContainerMenu {
	private static final int TREASURE = TreasureChestBlockEntity.TREASURE_SLOTS;
	private static final int INVENTORY_START = TREASURE, HOTBAR_START = TREASURE + 27, SLOT_END = TREASURE + 36;

	private final Container chest;

	/** Client side. */
	public TreasureChestMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(TREASURE));
	}

	public TreasureChestMenu(int containerId, Inventory inventory, Container chest) {
		super(ShipFittingsModule.TREASURE_CHEST_MENU, containerId);
		this.chest = chest;
		for (int slot = 0; slot < TREASURE; slot++) {
			this.addSlot(new Slot(chest, slot, 62 + slot % 3 * 18, 18 + slot / 3 * 18));
		}
		this.addStandardInventorySlots(inventory, 8, 84);
	}

	@Override
	public boolean stillValid(Player player) {
		return this.chest.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (index < TREASURE) {
			if (!this.moveItemStackTo(stack, INVENTORY_START, SLOT_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.moveItemStackTo(stack, 0, TREASURE, false)) {
			// The chest is full: shuffle between inventory and hotbar instead.
			boolean moved = index < HOTBAR_START
				? this.moveItemStackTo(stack, HOTBAR_START, SLOT_END, true)
				: this.moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false);
			if (!moved) {
				return ItemStack.EMPTY;
			}
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, stack);
		return original;
	}
}
