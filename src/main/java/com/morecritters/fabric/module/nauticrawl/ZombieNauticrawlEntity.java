package com.morecritters.fabric.module.nauticrawl;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A dead nauticrawl risen again: behaves like the living one but hunts players on sight and
 * never despawns. One in ten has coral growing on its shell.
 */
public class ZombieNauticrawlEntity extends NauticrawlEntity {
	private static final EntityDataAccessor<Boolean> CORAL = SynchedEntityData.defineId(ZombieNauticrawlEntity.class, EntityDataSerializers.BOOLEAN);
	private static final int CORAL_ONE_IN = 10;

	public ZombieNauticrawlEntity(EntityType<? extends ZombieNauticrawlEntity> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CORAL, false);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, false));
	}

	@Override
	protected void onFirstSpawn(ServerLevel level) {
		if (getRandom().nextInt(CORAL_ONE_IN) == 0) entityData.set(CORAL, true);
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public String textureName() {
		String shell = entityData.get(CORAL) ? "nauticrawl_zombie_coral" : "nauticrawl_zombie";
		return isCracked() ? shell + "_cracked" : shell;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Coral", entityData.get(CORAL));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(CORAL, input.getBooleanOr("Coral", false));
	}
}
