package com.morecritters.fabric.module.bomb_jelly;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * The {@code bomb_jelly} that spawns naturally and from the spawn egg. On its first tick it turns
 * into a small, medium or large bomb jelly, each equally likely.
 */
public class UnsizedBombJellyEntity extends PathfinderMob {
	public UnsizedBombJellyEntity(EntityType<? extends UnsizedBombJellyEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 10.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 16.0)
			.add(Attributes.STEP_HEIGHT, 0.6);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level() instanceof ServerLevel level && !this.isRemoved()) {
			BombJellySize size = BombJellySize.values()[this.getRandom().nextInt(BombJellySize.values().length)];
			this.discard();
			BombJellyEntity jelly = BombJellyModule.typeFor(size).spawn(level, this.blockPosition(), EntitySpawnReason.MOB_SUMMONED);
			if (jelly != null) {
				jelly.setYRot(this.getYRot());
				jelly.setYBodyRot(this.getYRot());
				jelly.setYHeadRot(this.getYRot());
			}
		}
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}
}
