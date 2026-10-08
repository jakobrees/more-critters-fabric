package com.morecritters.fabric.module.critterlings_c;

import com.morecritters.fabric.module.critterling_system.Critterling;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The olmer: a quiet critterling (no idle sound) that wanders, fidgets and dances. Its rarer
 * looks are named {@code olmer_rare} and {@code olmer_epic}.
 */
public class OlmerEntity extends Critterling {
	public OlmerEntity(EntityType<? extends OlmerEntity> type, Level level) {
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
		return CommonTraits.suffixedTexture("olmer", rarity());
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return null;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return CritterlingsCModule.OLMER_HURT;
	}

	/** The original reused the hurt sound on death. */
	@Override
	protected SoundEvent getDeathSound() {
		return CritterlingsCModule.OLMER_HURT;
	}
}
