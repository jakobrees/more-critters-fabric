package com.morecritters.fabric.module.kelpire;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

/**
 * Steering for the kelpire: in water it pitches toward its target and swims in three
 * dimensions with a slight constant lift; on land it barely crawls.
 */
final class KelpireMoveControl extends MoveControl {
	private static final double WATER_LIFT = 0.005;
	private static final float LAND_SPEED_FACTOR = 0.05F;
	private static final float MAX_PITCH = 85.0F;

	KelpireMoveControl(Mob mob) {
		super(mob);
	}

	@Override
	public void tick() {
		if (this.mob.isInWater()) {
			this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(0.0, WATER_LIFT, 0.0));
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
		float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
		float movementSpeed = (float) this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
		float speed = (float) (this.speedModifier * movementSpeed);
		this.mob.setYRot(this.rotlerp(this.mob.getYRot(), yaw, 10.0F));
		this.mob.yBodyRot = this.mob.getYRot();
		this.mob.yHeadRot = this.mob.getYRot();
		if (this.mob.isInWater()) {
			this.mob.setSpeed(movementSpeed);
			float pitch = -(float) (Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG);
			pitch = Mth.clamp(Mth.wrapDegrees(pitch), -MAX_PITCH, MAX_PITCH);
			this.mob.setXRot(this.rotlerp(this.mob.getXRot(), pitch, 5.0F));
			this.mob.setZza(Mth.cos(this.mob.getXRot() * Mth.DEG_TO_RAD) * speed);
			this.mob.setYya((float) (speed * dy));
		} else {
			this.mob.setSpeed(speed * LAND_SPEED_FACTOR);
		}
	}
}
