package com.morecritters.fabric.module.nightshroom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import org.jspecify.annotations.Nullable;

/** One creature turning into another where it stands. */
final class Transformations {
	/** Removes {@code original} and spawns {@code into} at its block, facing the way it faced. */
	static <T extends Entity> @Nullable T replace(ServerLevel level, Entity original, EntityType<T> into) {
		original.discard();
		T replacement = into.spawn(level, original.blockPosition(), EntitySpawnReason.MOB_SUMMONED);
		if (replacement != null) {
			float yaw = original.getYRot();
			replacement.setYRot(yaw);
			replacement.setYBodyRot(yaw);
			replacement.setYHeadRot(yaw);
			replacement.setXRot(original.getXRot());
			replacement.setDeltaMovement(0.0, 0.0, 0.0);
		}
		return replacement;
	}

	private Transformations() {}
}
