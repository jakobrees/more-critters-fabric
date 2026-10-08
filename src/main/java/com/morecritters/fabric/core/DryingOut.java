package com.morecritters.fabric.core;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A water creature's own air counter: full in water, running down on land. When it runs out the fish
 * takes one point of dry-out damage, then again every second.
 */
public final class DryingOut {
	private static final int FULL_AIR = 200;
	private static final int AIR_AFTER_GASP = 20;

	private final LivingEntity fish;
	private int air = FULL_AIR;

	public DryingOut(LivingEntity fish) {
		this.fish = fish;
	}

	public void tick(ServerLevel level) {
		if (this.fish.isInWater()) {
			this.air = FULL_AIR;
		} else {
			this.air--;
		}
		if (this.air <= 1) {
			this.fish.hurtServer(level, level.damageSources().source(DamageTypes.DRY_OUT), 1.0F);
			this.air = AIR_AFTER_GASP;
		}
	}

	public void save(ValueOutput output) {
		output.putInt("DryingAir", this.air);
	}

	public void load(ValueInput input) {
		this.air = input.getIntOr("DryingAir", FULL_AIR);
	}
}
