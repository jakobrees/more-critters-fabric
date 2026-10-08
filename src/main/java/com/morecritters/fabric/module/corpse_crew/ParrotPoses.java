package com.morecritters.fabric.module.corpse_crew;

import com.geckolib.animation.RawAnimation;
import net.minecraft.world.entity.Entity;

/** The looping animations both corpse parrots share. */
final class ParrotPoses {
	static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
	static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");
	static final RawAnimation SITTING = RawAnimation.begin().thenLoop("sitting");

	/** Flapping in the air; on the ground sitting, walking or idle. */
	static RawAnimation of(Entity parrot, boolean moving, boolean sitting) {
		if (!parrot.onGround()) return FLY;
		if (sitting) return SITTING;
		return moving ? WALK : IDLE;
	}

	private ParrotPoses() {}
}
