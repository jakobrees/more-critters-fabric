package com.morecritters.fabric.module.critterlings_c;

import com.morecritters.fabric.module.critterling_system.Critterling;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.level.Level;

/**
 * The gillmunch: a plain critterling that strolls about on dry land, fidgets and dances. Unlike
 * most critterlings it can drown.
 */
public class GillmunchEntity extends Critterling {
	public GillmunchEntity(EntityType<? extends GillmunchEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return CommonTraits.common();
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, CommonTraits.dryWander(this, 0.8));
		this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(3, new FloatGoal(this));
	}

	@Override
	protected boolean drowns() {
		return true;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return CritterlingsCModule.GILLMUNCH_IDLE;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return CritterlingsCModule.GILLMUNCH_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return CritterlingsCModule.GILLMUNCH_DEATH;
	}
}
