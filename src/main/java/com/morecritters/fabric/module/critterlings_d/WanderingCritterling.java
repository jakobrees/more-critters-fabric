package com.morecritters.fabric.module.critterlings_d;

import com.morecritters.fabric.module.critterling_system.Critterling;
import com.morecritters.fabric.module.critterling_system.CritterlingRarity;
import java.util.Locale;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.level.Level;

/**
 * What the four critterlings of this module share, as the original wrote them: a 3-health
 * critterling that strolls at 0.6, looks around, floats, and uses its {@code idle} sound
 * as ambient and its {@code hurt} sound for both hurting and dying.
 */
abstract class WanderingCritterling extends Critterling {
	private static final double STROLL_SPEED = 0.6;

	protected WanderingCritterling(EntityType<? extends WanderingCritterling> type, Level level) {
		super(type, level);
	}

	static AttributeSupplier.Builder attributes(double movementSpeed) {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, movementSpeed)
			.add(Attributes.MAX_HEALTH, 3.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	/**
	 * Texture names with the rarity as a suffix ({@code flarg}, {@code flarg_rare}, {@code flarg_epic}),
	 * which is how the original named three of these critterlings' textures (the stalk's use the
	 * usual prefix).
	 */
	static String suffixedTexture(String base, CritterlingRarity rarity) {
		return rarity == CritterlingRarity.NORMAL ? base : base + "_" + rarity.name().toLowerCase(Locale.ROOT);
	}

	protected abstract SoundEvent idleSound();

	protected abstract SoundEvent hurtSound();

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, wanderGoal(STROLL_SPEED));
		this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(3, new FloatGoal(this));
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return idleSound();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return hurtSound();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return hurtSound();
	}
}
