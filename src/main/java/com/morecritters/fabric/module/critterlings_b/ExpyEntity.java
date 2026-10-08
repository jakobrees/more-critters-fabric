package com.morecritters.fabric.module.critterlings_b;

import com.morecritters.fabric.ids.MiscIds;
import com.morecritters.fabric.module.critterling_system.Critterling;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A floating orb of experience. It ignores gravity, drifts to random spots up to 16 blocks away in
 * any direction, bobs up off the ground when it touches it, and trails an experience particle in
 * its rarity's colour every tick. It has no fidgets and no walk animation, and drops 1 xp.
 */
public class ExpyEntity extends Critterling {
	private static final int STROLL_INTERVAL = 20;
	private static final float STROLL_RANGE = 16.0F;
	private static final double LIFT_OFF_GROUND = 0.1;

	public ExpyEntity(EntityType<? extends ExpyEntity> type, Level level) {
		super(type, level);
		this.xpReward = 1;
		this.moveControl = new FlyingMoveControl<>(this, 10, true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 3.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 16.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FLYING_SPEED, 0.3);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new FlyingPathNavigation(this, level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new DriftGoal());
		this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(3, new FloatGoal(this));
	}

	@Override
	protected List<String> fidgetAnimations() {
		return List.of();
	}

	@Override
	protected @Nullable String walkAnimation() {
		return null;
	}

	@Override
	public boolean isNoGravity() {
		return true;
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!(this.level() instanceof ServerLevel level)) return;
		if (this.onGround()) {
			this.setDeltaMovement(0.0, LIFT_OFF_GROUND, 0.0);
		}
		trail(level);
	}

	/** One {@code xp}, {@code xprare} or {@code xpepic} particle (the misc module's), as the original's forced {@code /particle}. */
	private void trail(ServerLevel level) {
		Identifier id = switch (rarity()) {
			case NORMAL -> MiscIds.Particles.XP;
			case RARE -> MiscIds.Particles.XPRARE;
			case EPIC -> MiscIds.Particles.XPEPIC;
		};
		double lift = switch (rarity()) {
			case EPIC -> 0.0;
			default -> 0.2;
		};
		if (BuiltInRegistries.PARTICLE_TYPE.getValue(id) instanceof ParticleOptions particle) {
			level.sendParticles(particle, true, false, getX(), getY() + lift, getZ(), 1, 0.0, 0.0, 0.0, 1.0);
		}
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return CritterlingsBModule.EXPY_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return CritterlingsBModule.EXPY_DEATH;
	}

	/** Every second or so picks a point up to 16 blocks away on every axis and floats there; pauses while dancing. */
	private class DriftGoal extends RandomStrollGoal {
		DriftGoal() {
			super(ExpyEntity.this, 0.7, STROLL_INTERVAL);
		}

		@Override
		protected Vec3 getPosition() {
			return new Vec3(
				getX() + (getRandom().nextFloat() * 2.0F - 1.0F) * STROLL_RANGE,
				getY() + (getRandom().nextFloat() * 2.0F - 1.0F) * STROLL_RANGE,
				getZ() + (getRandom().nextFloat() * 2.0F - 1.0F) * STROLL_RANGE);
		}

		@Override
		public boolean canUse() {
			return !isDancing() && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return !isDancing() && super.canContinueToUse();
		}
	}
}
