package com.morecritters.fabric.module.gravedigger;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A slow, heavy undead lump of bodies that a gravedigger sometimes unearths. It hunts the
 * nearest player and strikes within two blocks. Never despawns.
 */
public class AmalgamEntity extends Monster implements GeoEntity {
	/** Squared reach of its swipe: two blocks. */
	private static final double ATTACK_REACH_SQR = 4.0;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);

	public AmalgamEntity(EntityType<? extends AmalgamEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			.add(Attributes.MAX_HEALTH, 30.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 6.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false) {
			@Override
			protected boolean canPerformAttack(LivingEntity target) {
				return this.isTimeToAttack() && this.mob.distanceToSqr(target) < ATTACK_REACH_SQR && this.mob.getSensing().hasLineOfSight(target);
			}
		});
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
		this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(5, new FloatGoal(this));
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		this.triggerAnim(Animations.ACTIONS, "attack");
		return super.doHurtTarget(level, target);
	}

	@Override protected SoundEvent getAmbientSound() { return GravediggerModule.AMALGAM_IDLE_SOUND; }
	@Override protected SoundEvent getHurtSound(DamageSource source) { return GravediggerModule.AMALGAM_HURT_SOUND; }
	@Override protected SoundEvent getDeathSound() { return GravediggerModule.AMALGAM_DEATH_SOUND; }

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(Animations.idleWalk(this, "idle", "walk"));
		controllers.add(Animations.actions(this, "attack"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
