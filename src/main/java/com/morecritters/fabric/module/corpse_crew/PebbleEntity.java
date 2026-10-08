package com.morecritters.fabric.module.corpse_crew;

import com.morecritters.fabric.ids.MiscIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A pebble a corpse parrot drops on its target. Drawn as the misc module's pebble icon. */
public class PebbleEntity extends CrewProjectile {
	public PebbleEntity(EntityType<? extends PebbleEntity> type, Level level) {
		super(type, level);
	}

	private static ItemStack pebble() {
		return new ItemStack(BuiltInRegistries.ITEM.getValue(MiscIds.Items.PEBBLE_ICON));
	}

	@Override
	public ItemStack getItem() {
		return pebble();
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		return pebble();
	}
}
