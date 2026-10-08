package com.morecritters.fabric.core;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Granting the mod's own advancements directly, since they have no vanilla trigger. */
public final class Advancements {
	public static void award(Entity entity, Identifier advancementId) {
		if (!(entity instanceof ServerPlayer player)) return;
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(advancementId);
		if (advancement == null) return;
		AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
		if (progress.isDone()) return;
		for (String criterion : progress.getRemainingCriteria()) {
			player.getAdvancements().award(advancement, criterion);
		}
	}

	private Advancements() {}
}
