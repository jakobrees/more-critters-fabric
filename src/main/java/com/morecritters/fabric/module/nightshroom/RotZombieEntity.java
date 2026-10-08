package com.morecritters.fabric.module.nightshroom;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.ServerScheduler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

/**
 * A frail zombie of rot that a landing rot piece raises. It swells up out of the ground over
 * half a second, then hunts players from far away. Arrows pass through it, it bleeds rot when
 * hurt, and it crumbles when the frightshroom that made it dies. Never despawns.
 */
public class RotZombieEntity extends Monster implements GeoEntity {
	private static final float GROWTH_PER_TICK = 0.1F, FULL_SIZE = 1.0F;
	private static final double ATTACK_REACH_SQR = 2.25;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	/** Its size, from nothing to full; grows on both sides so the client sees it swell. */
	private float growth;

	public RotZombieEntity(EntityType<? extends RotZombieEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			.add(Attributes.MAX_HEALTH, 5.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 64.0);
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
		this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
		this.goalSelector.addGoal(4, new RandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(6, new FloatGoal(this));
	}

	/** Rises with its spawn animation and, a moment later, a gush of rot. */
	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, groupData);
		// A tick later, once clients know about it.
		ServerScheduler.runLater(1, () -> this.triggerAnim(Animations.ACTIONS, "spawn"));
		ServerLevel serverLevel = level.getLevel();
		ServerScheduler.runLater(3, () -> Bursts.rot(serverLevel, this.position(), 12, 0.3, 0.7, 0.3, 0.02));
		this.growth = 0.0F;
		return result;
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.growth < FULL_SIZE) {
			this.growth += GROWTH_PER_TICK;
			this.refreshDimensions();
		}
	}

	public float growth() {
		return this.growth;
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).scale(this.growth);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		Bursts.rot(level, this.position().add(0.0, 1.0, 0.0), 14, 0.3, 0.4, 0.3, 0.06);
		if (source.getDirectEntity() instanceof AbstractArrow || source.is(DamageTypes.DROWN)) return false;
		return super.hurtServer(level, source, damage);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		this.triggerAnim(Animations.ACTIONS, "attack");
		return super.doHurtTarget(level, target);
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override protected SoundEvent getAmbientSound() { return NightshroomModule.ROT_ZOMBIE_IDLE_SOUND; }
	@Override protected SoundEvent getHurtSound(DamageSource source) { return NightshroomModule.ROT_ZOMBIE_HURT_SOUND; }
	@Override protected SoundEvent getDeathSound() { return NightshroomModule.ROT_ZOMBIE_DEATH_SOUND; }

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(Animations.idleWalk(this, "idle", "walk"));
		controllers.add(Animations.actions(this, "attack", "spawn"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
