package com.morecritters.fabric.module.wandering_collector;

import com.mojang.serialization.Codec;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import com.morecritters.fabric.core.Drops;
import com.morecritters.fabric.core.Sounds;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The collector's trade screen: one slot for what the player sells, one read-only slot showing
 * what the collector pays per item ({@link WanderingCollectorTrades#priceFor}), and a "Trade All"
 * button ({@link #TRADE_BUTTON}, sent as a vanilla menu-button click) that sells the whole stack.
 * Selling five different kinds of item earns the advancement that unlocks carrybugs.
 */
public class WanderingCollectorMenu extends AbstractContainerMenu {
	public static final int TRADE_BUTTON = 0;
	/** Different items a player must sell before the collector trusts them with a carrybug. */
	private static final int KINDS_FOR_CARRYBUG = 5;

	private static final int INPUT_SLOT = 0, INVENTORY_START = 2, HOTBAR_START = 29, SLOT_END = 38;

	/** Ids of the different items a player has sold to any collector (the original's {@code traded1..5}). */
	static AttachmentType<List<String>> ITEMS_SOLD;

	private final Player player;
	/** The collector being traded with; null on the client. */
	private final @Nullable Entity collector;
	private final SimpleContainer input = new SimpleContainer(1) {
		@Override
		public void setChanged() {
			super.setChanged();
			WanderingCollectorMenu.this.slotsChanged(this);
		}
	};
	private final SimpleContainer offer = new SimpleContainer(1);

	static void register() {
		ITEMS_SOLD = AttachmentRegistry.create(MoreCritters.id("collector_items_sold"),
			builder -> builder.persistent(Codec.STRING.listOf()));
	}

	/** Client side. */
	public WanderingCollectorMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, null);
	}

	public WanderingCollectorMenu(int containerId, Inventory inventory, @Nullable Entity collector) {
		super(WanderingCollector.MENU, containerId);
		this.player = inventory.player;
		this.collector = collector;
		this.addSlot(new Slot(this.input, 0, 57, 29));
		this.addSlot(new Slot(this.offer, 0, 103, 29) {
			@Override
			public boolean mayPickup(Player player) {
				return false;
			}

			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		});
		this.addStandardInventorySlots(inventory, 8, 84);
	}

	/** Shows the collector's price for whatever is in the input slot. */
	@Override
	public void slotsChanged(Container container) {
		if (container == this.input && !this.player.level().isClientSide()) {
			this.offer.setItem(0, WanderingCollectorTrades.priceFor(this.input.getItem(0)));
		}
		super.slotsChanged(container);
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		if (buttonId == TRADE_BUTTON && player instanceof ServerPlayer serverPlayer) {
			this.tradeAll(serverPlayer);
		}
		return buttonId == TRADE_BUTTON;
	}

	/** Sells the whole input stack at the offered price per item. */
	private void tradeAll(ServerPlayer player) {
		ItemStack sold = this.input.getItem(0);
		ItemStack price = this.offer.getItem(0);
		if (sold.isEmpty() || price.isEmpty()) {
			return;
		}
		pay(player, price, sold.getCount() * price.getCount());
		Advancements.award(player, MoreCritters.id("trade_with_collector"));
		int kindsSold = recordSale(player, BuiltInRegistries.ITEM.getKey(sold.getItem()).toString());
		this.input.setItem(0, ItemStack.EMPTY);

		Entity at = this.collector != null ? this.collector : player;
		ServerLevel level = player.level();
		for (int orbs = Drops.randomCount(at, 1, 3); orbs > 0; orbs--) {
			level.addFreshEntity(new ExperienceOrb(level, at.getX(), at.getY(), at.getZ(), 1));
		}
		Sounds.playAt(at, WanderingCollectorModule.COLLECTOR_TRADE_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
		if (kindsSold >= KINDS_FOR_CARRYBUG) {
			Advancements.award(player, MoreCritters.id("gain_access_to_carrybug"));
		}
	}

	private static void pay(ServerPlayer player, ItemStack currency, int amount) {
		while (amount > 0) {
			int count = Math.min(amount, currency.getMaxStackSize());
			player.getInventory().placeItemBackInInventory(currency.copyWithCount(count), Prediction.SERVER_ONLY);
			amount -= count;
		}
	}

	/** Remembers a newly sold kind of item; returns how many different kinds the player has sold. */
	private static int recordSale(ServerPlayer player, String itemId) {
		List<String> sold = player.getAttachedOrElse(ITEMS_SOLD, List.of());
		if (sold.size() < KINDS_FOR_CARRYBUG && !sold.contains(itemId)) {
			sold = new ArrayList<>(sold);
			sold.add(itemId);
			player.setAttached(ITEMS_SOLD, List.copyOf(sold));
		}
		return sold.size();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (index < INVENTORY_START) {
			if (!this.moveItemStackTo(stack, INVENTORY_START, SLOT_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
			// The input is taken: shuffle between inventory and hotbar instead.
			boolean moved = index < HOTBAR_START
				? this.moveItemStackTo(stack, HOTBAR_START, SLOT_END, false)
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

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	/** Gives back whatever is left in the input slot; the price slot is only a display. */
	@Override
	public void removed(Player player) {
		super.removed(player);
		if (player instanceof ServerPlayer) {
			this.clearContainer(player, this.input);
		}
	}

	static MenuType<WanderingCollectorMenu> createType() {
		return new MenuType<>(WanderingCollectorMenu::new, FeatureFlags.VANILLA_SET);
	}
}
