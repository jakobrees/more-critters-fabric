package com.morecritters.fabric.module.corpse_crew;

import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.AnimationTest;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Config;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.ids.CorpseGearIds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
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
import org.jspecify.annotations.Nullable;

/**
 * The crew's medic. It bites in melee, and in a fight every {@code quartermaster_heal_cooldown}
 * ticks (150) it pauses to smash a bottle of healing rum at its feet, which mends the crew around
 * it (the rum itself belongs to the corpse_gear module).
 */
public class CorpseQuartermasterEntity extends CorpseCrewMember {
	private int healTimer = healCooldown();

	public CorpseQuartermasterEntity(EntityType<? extends CorpseQuartermasterEntity> type, Level level) {
		super(type, level, 3);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			.add(Attributes.MAX_HEALTH, 20.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 5.0);
	}

	private static int healCooldown() {
		return Config.integer("quartermaster_heal_cooldown", 150);
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
		if (--this.healTimer == 0) {
			this.healTimer = healCooldown();
			if (this.getTarget() != null) throwRum();
		}
	}

	/** Stops a moment and drops a bottle of healing rum just around its feet. */
	private void throwRum() {
		this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10, 29, false, false));
		this.triggerAnim(Animations.ACTIONS, "heal2");
		Sounds.playAt(this, CorpseCrewModule.QUARTERMASTER_THROW, SoundSource.HOSTILE, 1.0F, 1.0F);
		entityType(CorpseGearIds.Entities.HEALING_RUM_PROJECTILE)
			.map(type -> type.create(this.level(), EntitySpawnReason.TRIGGERED))
			.filter(rum -> rum instanceof Projectile)
			.map(rum -> (Projectile) rum)
			.ifPresent(rum -> {
				if (rum instanceof AbstractArrow arrow) arrow.setBaseDamage(0.0);
				rum.setSilent(true);
				rum.setPos(this.position());
				rum.shoot(Mth.nextDouble(this.random, -0.2, 0.2), -0.2, Mth.nextDouble(this.random, -0.2, 0.2), 0.7F, 0.0F);
				this.level().addFreshEntity(rum);
			});
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).scale(1.1F);
	}

	@Override public @Nullable SoundEvent attackSound() { return CorpseCrewModule.QUARTERMASTER_BITE; }
	@Override protected @Nullable String strikeAnimation() { return "bite"; }
	@Override protected @Nullable SoundEvent speechSound() { return CorpseCrewModule.QUARTERMASTER_SPEECH; }
	@Override protected @Nullable SoundEvent songSound() { return CorpseCrewModule.QUARTERMASTER_SING; }
	@Override protected SoundEvent getAmbientSound() { return CorpseCrewModule.QUARTERMASTER_IDLE; }
	@Override protected SoundEvent getHurtSound(DamageSource source) { return CorpseCrewModule.QUARTERMASTER_HURT; }
	@Override protected SoundEvent getDeathSound() { return CorpseCrewModule.QUARTERMASTER_DEATH; }

	@Override
	protected String[] actionAnimations() {
		return new String[] {"bite", "heal2"};
	}

	@Override
	protected RawAnimation movementAnimation(AnimationTest<CorpseCrewMember> test) {
		return Gait.walkOrRun(this, test);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("heal", this.healTimer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.healTimer = input.getIntOr("heal", this.healTimer);
	}
}
