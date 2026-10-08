package com.morecritters.fabric.module.iropod;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/** The rare black iropod: behaves like an iropod, never despawns, and goes into its own bucket. */
public class BlackIropodEntity extends IropodEntity {
	public BlackIropodEntity(EntityType<? extends BlackIropodEntity> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	@Override
	protected Item bucketItem() {
		return IropodModule.blackIropodBucket;
	}

	/** Already black. */
	@Override
	protected void maybeTurnBlack(ServerLevelAccessor level) {
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}
}
