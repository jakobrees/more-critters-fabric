package com.morecritters.fabric.module.ship_fittings;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The cannon's screen: one ammunition slot that takes only cannon balls, above the player's inventory. */
public class CannonMenu extends AbstractContainerMenu {
	/** The original's tag, in the {@code minecraft} namespace. */
	private static final TagKey<Item> CANNON_BALLS = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("cannon_balls"));
	private static final int AMMO_SLOT = 0, INVENTORY_START = 1, HOTBAR_START = 28, SLOT_END = 37;

	private final Container cannon;

	/** Client side. */
	public CannonMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(1) {
			@Override
			public int getMaxStackSize() {
				return CannonBlockEntity.MAGAZINE_SIZE;
			}
		});
	}

	public CannonMenu(int containerId, Inventory inventory, Container cannon) {
		super(ShipFittingsModule.CANNON_MENU, containerId);
		this.cannon = cannon;
		checkContainerSize(cannon, 1);
		this.addSlot(new Slot(cannon, 0, 80, 39) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(CANNON_BALLS);
			}
		});
		this.addStandardInventorySlots(inventory, 8, 84);
	}

	@Override
	public boolean stillValid(Player player) {
		return this.cannon.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (index == AMMO_SLOT) {
			if (!this.moveItemStackTo(stack, INVENTORY_START, SLOT_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.moveItemStackTo(stack, AMMO_SLOT, AMMO_SLOT + 1, false)) {
			// Not ammunition, or the cannon is full: shuffle between inventory and hotbar instead.
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
