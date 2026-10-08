package com.morecritters.fabric.module.critterlings_a;

import com.morecritters.fabric.module.critterling_system.Critterling;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.level.Level;

/** The snek: a critterling that wanders, hisses now and then and turns its head to watch any creature within six blocks. */
public class SnekEntity extends Critterling {
	private static final float WATCH_DISTANCE = 6.0F;

	public SnekEntity(EntityType<? extends SnekEntity> type, Level level) {
		super(type, level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, wanderGoal(0.5));
		this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, LivingEntity.class, WATCH_DISTANCE));
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(4, new FloatGoal(this));
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return CritterlingsAModule.SNEK_IDLE;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return CritterlingsAModule.SNEK_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return CritterlingsAModule.SNEK_HURT;
	}
}
