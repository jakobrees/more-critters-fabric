package com.morecritters.fabric.module.corpse_crew;

import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.AnimationTest;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Config;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.ids.CorpseGearIds;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The crew's heavy: a slow, tough brute with a pearl cannon. Every {@code tank_shoot_cooldown}
 * ticks (200), if it can see its target, it braces (held to the deck), and 18 ticks later fires a
 * flying pearl at it from shoulder height. Killing one before it ever fires earns an advancement.
 */
public class CorpseTankEntity extends CorpseCrewMember {
	private static final int AIM_TICKS = 18;
	private static final int BRACE_TICKS = 30;
	private static final double PEARL_DAMAGE = 5.0;
	private static final float PEARL_SPEED = 2.0F;

	private int shotTimer = shotCooldown();
	private boolean bracing;
	private boolean hasShot;

	public CorpseTankEntity(EntityType<? extends CorpseTankEntity> type, Level level) {
		super(type, level, 3);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			.add(Attributes.MAX_HEALTH, 50.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 5.0)
			.add(Attributes.FOLLOW_RANGE, 5.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
	}

	private static int shotCooldown() {
		return Config.integer("tank_shoot_cooldown", 200);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, meleeGoal(1.0, 4.0));
		this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(4, new FloatGoal(this));
	}

	@Override
	protected void crewTick(ServerLevel level) {
		if (--this.shotTimer == 0) {
			this.shotTimer = shotCooldown();
			LivingEntity target = this.getTarget();
			if (target != null && this.hasLineOfSight(target)) {
				takeAim();
			}
		}
		if (this.bracing && this.onGround()) {
			this.setDeltaMovement(0.0, -3.0, 0.0);
		}
	}

	/** Braces, and fires from where it stood when it began to aim. */
	private void takeAim() {
		Vec3 muzzle = this.position().add(0.0, 2.0, 0.0);
		this.bracing = true;
		this.triggerAnim(Animations.ACTIONS, "shoot");
		Sounds.playAt(this, CorpseCrewModule.TANK_READY, SoundSource.HOSTILE, 1.0F, 1.0F);
		ServerScheduler.runLater(BRACE_TICKS, () -> this.bracing = false);
		ServerScheduler.runLater(AIM_TICKS, () -> {
			if (this.isAlive()) fire(muzzle);
		});
	}

	private void fire(Vec3 muzzle) {
		this.hasShot = true;
		Sounds.playAt(this, CorpseCrewModule.TANK_SHOOT, SoundSource.HOSTILE, 1.0F, 1.0F);
		LivingEntity target = this.getTarget();
		if (target != null) {
			this.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(target.getX(), target.getY() + target.getBbHeight(), target.getZ()));
		}
		entityType(CorpseGearIds.Entities.FLYING_PEARL)
			.map(type -> type.create(this.level(), EntitySpawnReason.TRIGGERED))
			.filter(pearl -> pearl instanceof Projectile)
			.map(pearl -> (Projectile) pearl)
			.ifPresent(pearl -> {
				pearl.setOwner(this);
				if (pearl instanceof AbstractArrow arrow) arrow.setBaseDamage(PEARL_DAMAGE);
				pearl.setSilent(true);
				pearl.setPos(muzzle);
				Vec3 look = this.getLookAngle();
				pearl.shoot(look.x, look.y, look.z, PEARL_SPEED, 0.0F);
				this.level().addFreshEntity(pearl);
			});
	}

	@Override
	protected void awardKill(ServerPlayer player) {
		if (!this.hasShot) {
			Advancements.award(player, MoreCritters.id("kill_tank_before_shooting"));
		}
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).scale(1.1F);
	}

	@Override public @Nullable SoundEvent attackSound() { return CorpseCrewModule.TANK_ATTACK; }
	@Override protected @Nullable String strikeAnimation() { return "attack"; }
	@Override protected @Nullable SoundEvent speechSound() { return CorpseCrewModule.TANK_SPEECH; }
	@Override protected @Nullable SoundEvent songSound() { return CorpseCrewModule.TANK_SING; }
	@Override protected SoundEvent getAmbientSound() { return CorpseCrewModule.TANK_IDLE; }
	@Override protected SoundEvent getHurtSound(DamageSource source) { return CorpseCrewModule.TANK_HURT; }
	@Override protected SoundEvent getDeathSound() { return CorpseCrewModule.TANK_DEATH; }

	@Override
	protected String[] actionAnimations() {
		return new String[] {"attack", "shoot"};
	}

	@Override
	protected RawAnimation movementAnimation(AnimationTest<CorpseCrewMember> test) {
		return Gait.walkOnly(test);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("fire", this.shotTimer);
		output.putBoolean("DataHasShot", this.hasShot);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.shotTimer = input.getIntOr("fire", this.shotTimer);
		this.hasShot = input.getBooleanOr("DataHasShot", false);
	}
}
