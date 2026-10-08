package com.morecritters.fabric.core;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Spawning item entities in the world, the way the original mod does it. */
public final class Drops {
	/** Drops {@code count} single items at the position, each as its own entity with a short pickup delay. */
	public static void dropSingles(ServerLevel level, Vec3 pos, ItemLike item, int count) {
		for (int i = 0; i < count; i++) {
			ItemEntity drop = new ItemEntity(level, pos.x, pos.y, pos.z, new ItemStack(item));
			drop.setPickUpDelay(10);
			level.addFreshEntity(drop);
		}
	}

	public static void dropSingles(Entity at, ItemLike item, int count) {
		if (at.level() instanceof ServerLevel level) dropSingles(level, at.position(), item, count);
	}

	/** A random count in {@code [min, max)}, as MCreator's "random between" produced it (the upper bound is never reached). */
	public static int randomCount(Entity at, double min, double max) {
		return (int) Mth.nextDouble(at.getRandom(), min, max);
	}

	private Drops() {}
}
