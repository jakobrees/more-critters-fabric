package com.morecritters.fabric.module.critterlings_a;

import com.morecritters.fabric.module.critterling_system.Critterling;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.level.Level;

/** The dunger: a critterling that wanders slowly, looks around and fidgets. Silent but for its hurt sound. */
public class DungerEntity extends Critterling {
	public DungerEntity(EntityType<? extends DungerEntity> type, Level level) {
		super(type, level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, wanderGoal(0.5));
		this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(3, new FloatGoal(this));
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return CritterlingsAModule.DUNGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return CritterlingsAModule.DUNGER_HURT;
	}
}
