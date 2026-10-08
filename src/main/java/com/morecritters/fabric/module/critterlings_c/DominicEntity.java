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
 * Dominic: a plain critterling that wanders, fidgets and dances. Its rarer looks are named
 * {@code dominic_rare} and {@code dominic_epic}, the other way round from most critterlings.
 */
public class DominicEntity extends Critterling {
	public DominicEntity(EntityType<? extends DominicEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return CommonTraits.common();
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, wanderGoal(0.6));
		this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(3, new FloatGoal(this));
	}

	@Override
	public String textureName() {
		return CommonTraits.suffixedTexture("dominic", rarity());
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return CritterlingsCModule.DOMINIC_IDLE;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return CritterlingsCModule.DOMINIC_HURT;
	}

	/** The original reused the hurt sound on death. */
	@Override
	protected SoundEvent getDeathSound() {
		return CritterlingsCModule.DOMINIC_HURT;
	}
}
