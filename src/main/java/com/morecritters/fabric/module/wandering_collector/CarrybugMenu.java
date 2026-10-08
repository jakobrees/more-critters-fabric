package com.morecritters.fabric.module.wandering_collector;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A carrybug's chest: three 3x4 blocks of slots side by side above the player's inventory.
 * The client builds it over an empty container that the server fills slot by slot, like a chest.
 */
public class CarrybugMenu extends AbstractContainerMenu {
	private static final int SIZE = CarrybugEntity.INVENTORY_SIZE;
	/** Left edge of each 3-wide column block, in the original GUI's layout. */
	private static final int[] BLOCK_LEFT = {15, 75, 134};

	private final Container cargo;
	/** Set on the server only; the menu closes when it dies. */
	private final @Nullable CarrybugEntity carrybug;

	/** Client side. */
	public CarrybugMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, new SimpleContainer(SIZE), null);
	}

	public CarrybugMenu(int containerId, Inventory playerInventory, Container cargo, @Nullable CarrybugEntity carrybug) {
		super(CarrybugRegistry.MENU, containerId);
		this.cargo = cargo;
		this.carrybug = carrybug;
		checkContainerSize(cargo, SIZE);
		for (int slot = 0; slot < SIZE; slot++) {
			int block = slot / 12, row = slot % 12 / 3, column = slot % 3;
			this.addSlot(new Slot(cargo, slot, BLOCK_LEFT[block] + column * 18, 21 + row * 18));
		}
		this.addStandardInventorySlots(playerInventory, 21, 101);
	}

	@Override
	public boolean stillValid(Player player) {
		return this.carrybug == null || this.carrybug.isAlive();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = this.slots.get(slotIndex);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack moved = stack.copy();
		boolean fromCargo = slotIndex < SIZE;
		if (!this.moveItemStackTo(stack, fromCargo ? SIZE : 0, fromCargo ? this.slots.size() : SIZE, fromCargo)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return moved;
	}
}
