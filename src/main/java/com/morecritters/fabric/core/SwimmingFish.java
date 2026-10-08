package com.morecritters.fabric.core;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/** Swimming for the fish-like critters: free water pathing, a 3D move control, buoyancy, and the swim-speed boost. */
public final class SwimmingFish {
	/** Vanilla's base acceleration in water, which NeoForge's swim-speed attribute multiplied. */
	private static final float BASE_WATER_ACCELERATION = 0.02F;
	/** Fish keep drifting slightly upward while in water. */
	private static final double BUOYANCY = 0.005;

	/** Lets the fish path through water freely; returns the move control the fish installs. */
	public static MoveControl setUp(PathfinderMob fish) {
		fish.setPathfindingMalus(PathType.WATER, 0.0F);
		return new FishMoveControl(fish);
	}

	public static PathNavigation navigation(Mob fish, Level level) {
		return new WaterBoundPathNavigation(fish, level);
	}

	/** Adds the extra acceleration the original's swim-speed attribute gave on top of vanilla's. */
	public static void swimFaster(Mob fish, double swimSpeed, Vec3 input) {
		fish.moveRelative((float) (BASE_WATER_ACCELERATION * (swimSpeed - 1.0)), input);
	}

	/** Steers toward the path target in three dimensions in water; barely moves on land. */
	private static final class FishMoveControl extends MoveControl {
		FishMoveControl(Mob fish) {
			super(fish);
		}

		@Override
		public void tick() {
			if (this.mob.isInWater()) {
				this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(0.0, BUOYANCY, 0.0));
			}
			if (this.operation != Operation.MOVE_TO || this.mob.getNavigation().isDone()) {
				this.mob.setSpeed(0.0F);
				this.mob.setYya(0.0F);
				this.mob.setZza(0.0F);
				return;
			}
			double dx = this.wantedX - this.mob.getX();
			double dy = this.wantedY - this.mob.getY();
			double dz = this.wantedZ - this.mob.getZ();
			float movementSpeed = (float) this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
			float speed = (float) (this.speedModifier * movementSpeed);
			float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
			this.mob.setYRot(this.rotlerp(this.mob.getYRot(), yaw, 10.0F));
			this.mob.yBodyRot = this.mob.getYRot();
			this.mob.yHeadRot = this.mob.getYRot();
			if (this.mob.isInWater()) {
				this.mob.setSpeed(movementSpeed);
				float pitch = -(float) (Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG);
				pitch = Mth.clamp(Mth.wrapDegrees(pitch), -85.0F, 85.0F);
				this.mob.setXRot(this.rotlerp(this.mob.getXRot(), pitch, 5.0F));
				this.mob.setZza(Mth.cos(this.mob.getXRot() * Mth.DEG_TO_RAD) * speed);
				this.mob.setYya((float) (speed * dy));
			} else {
				this.mob.setSpeed(speed * 0.05F);
			}
		}
	}

	private SwimmingFish() {}
}
