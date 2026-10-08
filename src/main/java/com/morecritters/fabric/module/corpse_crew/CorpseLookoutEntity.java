package com.morecritters.fabric.module.corpse_crew;

import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.AnimationTest;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Sounds;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A towering corpse in the crow's nest. It spots any player (survival or adventure, not in pirate
 * gear) within 60 blocks, with or without a line of sight, and spits at them: usually one gob
 * straight at the target, but one time in five a spray of six to nine lobbed high into the air.
 * It spits faster the closer the target is, and backs away from anyone within four blocks. It
 * never strikes in melee and has nothing to say.
 */
public class CorpseLookoutEntity extends CorpseCrewMember {
	private static final double SPOTTING_RANGE = 60.0;
	private static final int VOLLEY_ODDS = 5;
	private static final double SPIT_DAMAGE = 1.0;
	private static final int SPIT_KNOCKBACK = 5;
	private static final float SPIT_SPEED = 3.0F;
	private static final float SPIT_VOLUME = 5.0F;
	private static final int IDLE_SPIT_DELAY = 20;

	private int spitTimer = IDLE_SPIT_DELAY;

	public CorpseLookoutEntity(EntityType<? extends CorpseLookoutEntity> type, Level level) {
		super(type, level, 0);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 30.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 120.0);
	}

	/** It walks toward its target but its reach is zero, so it never lands a blow. */
	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, meleeGoal(0.7, 0.0));
		this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(3, new FloatGoal(this));
	}

	@Override
	protected double lookoutRange() {
		return SPOTTING_RANGE;
	}

	@Override
	protected boolean wouldAttack(LivingEntity candidate) {
		return candidate instanceof Player && !isCreativeOrSpectator(candidate) && !wearsPirateGear(candidate);
	}

	@Override
	protected void crewTick(ServerLevel level) {
		LivingEntity target = this.getTarget();
		if (--this.spitTimer == 0) {
			if (target != null) {
				if (Mth.nextInt(this.random, 1, VOLLEY_ODDS) == 1) {
					spitVolley(target);
				} else {
					spitAt(target);
				}
				this.spitTimer = reloadTicks(this.distanceTo(target));
			} else {
				this.spitTimer = IDLE_SPIT_DELAY;
			}
		}
		if (target != null && this.onGround()) {
			float distance = this.distanceTo(target);
			if (distance > 0.0F && distance <= 4.0F) {
				Vec3 look = this.getLookAngle();
				this.push(-0.2 * look.x, -0.2, -0.2 * look.z);
			}
		}
	}

	/** 20 ticks within three blocks, 40 within ten, 100 beyond. */
	private int reloadTicks(float distance) {
		if (distance > 10.0F) return 100;
		if (distance > 3.0F) return 40;
		return distance > 0.0F ? 20 : 0;
	}

	private boolean isWalking() {
		return this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
	}

	private void spitAt(LivingEntity target) {
		this.triggerAnim(Animations.ACTIONS, isWalking() ? "spit3" : "spit2");
		Sounds.playAt(this, CorpseCrewModule.LOOKOUT_SPIT_SOUND, SoundSource.HOSTILE, SPIT_VOLUME, 1.0F);
		this.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(target.getX(), target.getY() + target.getBbHeight(), target.getZ()));
		LookoutSpitEntity spit = newSpit();
		spit.setPos(this.getX(), this.getEyeY() - 0.1, this.getZ());
		Vec3 look = this.getLookAngle();
		spit.shoot(look.x, look.y, look.z, SPIT_SPEED, 0.0F);
		this.level().addFreshEntity(spit);
	}

	/** Six to nine gobs lobbed upward from its feet, to rain down around the target. */
	private void spitVolley(LivingEntity target) {
		this.triggerAnim(Animations.ACTIONS, isWalking() ? "ultra_spit2" : "ultra_spit1");
		Sounds.playAt(this, CorpseCrewModule.LOOKOUT_SPIT_SOUND, SoundSource.HOSTILE, SPIT_VOLUME, 1.0F);
		this.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(target.getX(), target.getY() + target.getBbHeight() + 50.0, target.getZ()));
		int count = Mth.nextInt(this.random, 6, 9);
		for (int i = 0; i < count; i++) {
			LookoutSpitEntity spit = newSpit();
			spit.setPos(this.position());
			spit.shoot(Mth.nextDouble(this.random, -0.2, 0.2), 0.4, Mth.nextDouble(this.random, -0.2, 0.2), SPIT_SPEED, 0.0F);
			this.level().addFreshEntity(spit);
		}
	}

	private LookoutSpitEntity newSpit() {
		LookoutSpitEntity spit = new LookoutSpitEntity(CorpseCrewModule.LOOKOUT_SPIT, this.level());
		spit.setOwner(this);
		spit.setBaseDamage(SPIT_DAMAGE);
		spit.setKnockback(SPIT_KNOCKBACK);
		spit.setSilent(true);
		return spit;
	}

	@Override
	protected void awardKill(ServerPlayer player) {
		Advancements.award(player, MoreCritters.id("kill_corpse_lookout"));
	}

	@Override protected SoundEvent getAmbientSound() { return CorpseCrewModule.LOOKOUT_IDLE; }
	@Override protected SoundEvent getHurtSound(DamageSource source) { return CorpseCrewModule.LOOKOUT_HURT; }
	@Override protected SoundEvent getDeathSound() { return CorpseCrewModule.LOOKOUT_DEATH; }

	@Override
	protected String[] actionAnimations() {
		return new String[] {"spit2", "spit3", "ultra_spit1", "ultra_spit2"};
	}

	@Override
	protected RawAnimation movementAnimation(AnimationTest<CorpseCrewMember> test) {
		return Gait.walkOnly(test);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("attack", this.spitTimer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.spitTimer = input.getIntOr("attack", this.spitTimer);
	}
}
