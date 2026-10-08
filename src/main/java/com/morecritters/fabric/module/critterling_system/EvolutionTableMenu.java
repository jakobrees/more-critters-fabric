package com.morecritters.fabric.module.critterling_system;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.function.Predicate;

/**
 * The evolution table: put a critterling sack and an evolite on the left, and the table shows (greyed
 * out above the ingredient slots) what it wants for the next form: two items for a normal sack, three
 * for a rare one ({@link EvolutionRecipes}). With the right items in place the evolved sack appears on
 * the right; taking it uses up the sack, the evolite and one of each ingredient, plays the evolve
 * sound and awards "evolve_critterling". Nothing is stored in the block: the items come back on close.
 *
 * <p>Menu slots keep the original's numbering, which the screen relies on.
 */
public class EvolutionTableMenu extends AbstractContainerMenu {
	public static final int SACK_SLOT = 0, RESULT_SLOT = 1, EVOLITE_SLOT = 2;
	/** The three ingredient slots, in recipe order, and the preview slots above them. */
	public static final int[] INGREDIENT_SLOTS = {3, 4, 8}, PREVIEW_SLOTS = {5, 6, 7};
	private static final int TABLE_SLOTS = 9, INVENTORY_START = TABLE_SLOTS, HOTBAR_START = INVENTORY_START + 27, SLOT_END = HOTBAR_START + 9;

	private static final TagKey<Item> EVOLVING_SACKS = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("sack_common_and_rare"));
	private static final TagKey<Item> RARE_SACKS = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("sack_rare"));
	private static final Identifier EVOLVE_ADVANCEMENT = MoreCritters.id("evolve_critterling");

	private final Player player;
	private final ContainerLevelAccess access;
	/** Sack, evolite and the three ingredients; what the player puts in. */
	private final SimpleContainer inputs = new SimpleContainer(5) {
		@Override
		public void setChanged() {
			super.setChanged();
			EvolutionTableMenu.this.slotsChanged(this);
		}
	};
	private final SimpleContainer previews = new SimpleContainer(3);
	private final SimpleContainer result = new SimpleContainer(1);

	/** Client side. */
	public EvolutionTableMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, ContainerLevelAccess.NULL);
	}

	public EvolutionTableMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(CritterlingSystemModule.EVOLUTION_TABLE_MENU, containerId);
		this.player = inventory.player;
		this.access = access;
		this.addSlot(new FilteredSlot(this.inputs, 0, 26, 51, stack -> stack.is(EVOLVING_SACKS)));
		this.addSlot(new ResultSlot(this.result, 0, 134, 43));
		this.addSlot(new FilteredSlot(this.inputs, 1, 26, 33, stack -> stack.is(CritterlingSystemModule.EVOLITE)));
		this.addSlot(new Slot(this.inputs, 2, 62, 51));
		this.addSlot(new Slot(this.inputs, 3, 80, 51));
		this.addSlot(new PreviewSlot(this.previews, 0, 62, 31));
		this.addSlot(new PreviewSlot(this.previews, 1, 80, 31));
		this.addSlot(new PreviewSlot(this.previews, 2, 98, 31));
		this.addSlot(new Slot(this.inputs, 4, 98, 51));
		this.addStandardInventorySlots(inventory, 8, 84);
	}

	static MenuType<EvolutionTableMenu> createType() {
		return new MenuType<>(EvolutionTableMenu::new, FeatureFlags.VANILLA_SET);
	}

	private ItemStack sack() {
		return this.slots.get(SACK_SLOT).getItem();
	}

	/** Shows the next form's ingredients and, if they are all in place, the evolved sack. */
	@Override
	public void slotsChanged(Container container) {
		super.slotsChanged(container);
		if (container != this.inputs || this.player.level().isClientSide()) return;
		EvolutionRecipes.Recipe recipe = this.slots.get(EVOLITE_SLOT).hasItem() ? EvolutionRecipes.forSack(sack()) : null;
		for (int i = 0; i < PREVIEW_SLOTS.length; i++) {
			this.previews.setItem(i, recipe == null ? ItemStack.EMPTY : recipe.ingredientStack(i));
		}
		this.result.setItem(0, recipe != null && allIngredientsIn(recipe) ? recipe.resultStack() : ItemStack.EMPTY);
		this.broadcastChanges();
	}

	private boolean allIngredientsIn(EvolutionRecipes.Recipe recipe) {
		for (int i = 0; i < INGREDIENT_SLOTS.length; i++) {
			if (!recipe.matches(i, this.slots.get(INGREDIENT_SLOTS[i]).getItem())) return false;
		}
		return true;
	}

	/** Taking the evolved sack: spend the inputs, then celebrate. */
	private void evolve(Player player) {
		boolean toEpic = sack().is(RARE_SACKS);
		this.slots.get(EVOLITE_SLOT).remove(1);
		this.slots.get(INGREDIENT_SLOTS[0]).remove(1);
		this.slots.get(INGREDIENT_SLOTS[1]).remove(1);
		if (toEpic) this.slots.get(INGREDIENT_SLOTS[2]).remove(1);
		this.slots.get(SACK_SLOT).remove(1);
		if (player instanceof ServerPlayer) {
			Advancements.award(player, EVOLVE_ADVANCEMENT);
			SoundEvent sound = toEpic ? CritterlingSystemModule.EVOLVE_EPIC_SOUND : CritterlingSystemModule.EVOLVE_RARE_SOUND;
			this.access.execute((level, pos) -> level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F));
		}
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (index < TABLE_SLOTS) {
			if (!this.moveItemStackTo(stack, INVENTORY_START, SLOT_END, true)) return ItemStack.EMPTY;
		} else if (!this.moveItemStackTo(stack, 0, TABLE_SLOTS, false)) {
			// Nowhere on the table for it: shuffle between inventory and hotbar instead.
			boolean moved = index < HOTBAR_START
				? this.moveItemStackTo(stack, HOTBAR_START, SLOT_END, false)
				: this.moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false);
			if (!moved) return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
		slot.onTake(player, stack);
		return original;
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(this.access, player, CritterlingSystemModule.EVOLUTION_TABLE);
	}

	/** The inputs go back to the player; the previews and the not-yet-taken result were never real. */
	@Override
	public void removed(Player player) {
		super.removed(player);
		if (player instanceof ServerPlayer) {
			this.clearContainer(player, this.inputs);
		}
	}

	private static class FilteredSlot extends Slot {
		private final Predicate<ItemStack> filter;

		FilteredSlot(Container container, int index, int x, int y, Predicate<ItemStack> filter) {
			super(container, index, x, y);
			this.filter = filter;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return this.filter.test(stack);
		}
	}

	/** Shows an ingredient the recipe wants; cannot be touched. */
	private static class PreviewSlot extends Slot {
		PreviewSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

		@Override
		public boolean mayPickup(Player player) {
			return false;
		}
	}

	private class ResultSlot extends Slot {
		ResultSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

		@Override
		public void onTake(Player player, ItemStack stack) {
			super.onTake(player, stack);
			evolve(player);
		}
	}

	/** For the screen: the item in a menu slot. */
	public ItemStack itemIn(int menuSlot) {
		return this.slots.get(menuSlot).getItem();
	}
}
