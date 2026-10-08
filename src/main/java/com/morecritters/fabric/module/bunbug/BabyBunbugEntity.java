package com.morecritters.fabric.module.bunbug;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** A bunbug grub. It wanders for half a day and then turns into a {@link BunbugEntity}. */
public class BabyBunbugEntity extends PathfinderMob implements GeoEntity {
	private static final int GROW_UP_TICKS = 12_000;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private int ticksUntilGrown = GROW_UP_TICKS;

	public BabyBunbugEntity(EntityType<? extends BabyBunbugEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 3.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new RandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(2, new PanicGoal(this, 1.2));
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(4, new FloatGoal(this));
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level() instanceof ServerLevel level && --this.ticksUntilGrown <= 0) {
			growUp(level);
		}
	}

	private void growUp(ServerLevel level) {
		this.discard();
		BunbugEntity adult = BunbugModule.BUNBUG.spawn(level, this.blockPosition(), EntitySpawnReason.MOB_SUMMONED);
		if (adult != null) {
			adult.setYRot(this.getYRot());
			adult.setYBodyRot(this.getYRot());
			adult.setYHeadRot(this.getYRot());
			adult.setXRot(this.getXRot());
			adult.setDeltaMovement(Vec3.ZERO);
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("TicksUntilGrown", this.ticksUntilGrown);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.ticksUntilGrown = input.getIntOr("TicksUntilGrown", GROW_UP_TICKS);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return BunbugModule.HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return BunbugModule.DEATH_SOUND;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(Animations.idleWalk(this, "idle", "walk"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
