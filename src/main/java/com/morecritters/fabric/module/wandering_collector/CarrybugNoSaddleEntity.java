package com.morecritters.fabric.module.wandering_collector;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * A carrybug whose saddle and chests were sheared off: it keeps its biome colour but can no
 * longer be ridden or carry anything.
 */
public class CarrybugNoSaddleEntity extends CarrybugBaseEntity {
	public CarrybugNoSaddleEntity(EntityType<? extends CarrybugNoSaddleEntity> type, Level level) {
		super(type, level);
	}

	@Override
	public String textureName() {
		return "carrybug_" + this.variant() + "_nosaddle";
	}
}
