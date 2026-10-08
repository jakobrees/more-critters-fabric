package com.morecritters.fabric.module.critterlings_b;

import com.morecritters.fabric.module.critterling_system.Critterling;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.level.Level;

/**
 * A small crab critterling. Wanders and fidgets like the others; it does not float, but walks
 * along the bottom, and in water it scuttles faster (Speed II).
 */
public class OpalcrabEntity extends Critterling {
	private static final int WATER_SPEED_TICKS = 10, WATER_SPEED_AMPLIFIER = 1;

	public OpalcrabEntity(EntityType<? extends OpalcrabEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.4)
			.add(Attributes.MAX_HEALTH, 3.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, wanderGoal(0.8));
		this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!this.level().isClientSide() && this.isInWater()) {
			this.addEffect(new MobEffectInstance(MobEffects.SPEED, WATER_SPEED_TICKS, WATER_SPEED_AMPLIFIER, false, false));
		}
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return CritterlingsBModule.OPALCRAB_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return CritterlingsBModule.OPALCRAB_HURT;
	}
}
