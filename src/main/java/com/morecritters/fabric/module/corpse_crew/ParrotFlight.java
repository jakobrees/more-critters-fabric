package com.morecritters.fabric.module.corpse_crew;

import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** A parrot's wander: anywhere within 16 blocks in every direction, airborne or not, when allowed. */
final class ParrotFlight extends RandomStrollGoal {
	private static final float WANDER = 16.0F;
	private final BooleanSupplier allowed;

	ParrotFlight(PathfinderMob parrot, double speed, BooleanSupplier allowed) {
		super(parrot, speed, 20);
		this.allowed = allowed;
	}

	@Override
	protected @Nullable Vec3 getPosition() {
		var random = this.mob.getRandom();
		return new Vec3(
			this.mob.getX() + (random.nextFloat() * 2.0F - 1.0F) * WANDER,
			this.mob.getY() + (random.nextFloat() * 2.0F - 1.0F) * WANDER,
			this.mob.getZ() + (random.nextFloat() * 2.0F - 1.0F) * WANDER);
	}

	@Override
	public boolean canUse() {
		return super.canUse() && this.allowed.getAsBoolean();
	}

	@Override
	public boolean canContinueToUse() {
		return super.canContinueToUse() && this.allowed.getAsBoolean();
	}
}
