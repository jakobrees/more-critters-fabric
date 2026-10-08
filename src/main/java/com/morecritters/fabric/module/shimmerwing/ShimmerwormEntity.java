package com.morecritters.fabric.module.shimmerwing;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The shimmerworm: a little End grub that hatches from a shimmering chrysalis. It never
 * despawns, blinks away like an enderman when wet or stuck in a wall, can be picked up
 * with an empty hand, and after ten minutes turns into a shimmerwing.
 */
public class ShimmerwormEntity extends PathfinderMob implements GeoEntity {
	private static final int METAMORPHOSIS_TICKS = 12_000;
	private static final double BLINK_MAX_RISE = 2.0;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private int ticksUntilWings = METAMORPHOSIS_TICKS;

	public ShimmerwormEntity(EntityType<? extends ShimmerwormEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			.add(Attributes.MAX_HEALTH, 3.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new PanicGoal(this, 1.2));
		this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, LivingEntity.class, 6.0F));
		this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(5, new FloatGoal(this));
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level() instanceof ServerLevel level) {
			if (--this.ticksUntilWings == 0) {
				growWings(level);
				return;
			}
			if (this.isInWaterOrRain()) {
				this.hurtServer(level, this.damageSources().generic(), 1.0F);
				EnderBlink.blinkAway(this, BLINK_MAX_RISE);
			}
			if (this.isInWall()) {
				EnderBlink.blinkAway(this, BLINK_MAX_RISE);
			}
		}
	}

	/** Replaces the worm with a shimmerwing facing the same way. */
	private void growWings(ServerLevel level) {
		this.discard();
		ShimmerwingEntity wing = ShimmerwingModule.SHIMMERWING.spawn(level, this.blockPosition(), EntitySpawnReason.MOB_SUMMONED);
		if (wing != null) {
			wing.setYRot(this.getYRot());
			wing.setYBodyRot(this.getYRot());
			wing.setYHeadRot(this.getYRot());
			wing.setXRot(this.getXRot());
			wing.setDeltaMovement(Vec3.ZERO);
		}
	}

	/** An empty main hand scoops the worm up; it replaces whatever was there (nothing). */
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty()) {
			return super.mobInteract(player, hand);
		}
		if (!this.level().isClientSide()) {
			this.discard();
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ShimmerwingModule.shimmerwormItem));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		this.triggerAnim(Animations.ACTIONS, "hurt");
		return !source.is(DamageTypeTags.IS_FALL) && super.hurtServer(level, source, amount);
	}

	@Override
	protected void tickDeath() {
		if (++this.deathTime == 20 && this.level() instanceof ServerLevel level) {
			this.remove(RemovalReason.KILLED);
			this.dropExperience(level, null);
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("TicksUntilWings", this.ticksUntilWings);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.ticksUntilWings = input.getIntOr("TicksUntilWings", METAMORPHOSIS_TICKS);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ShimmerwingModule.WORM_HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ShimmerwingModule.WORM_HURT_SOUND;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(Animations.idleWalk(this, "idle", "walk"));
		controllers.add(Animations.actions(this, "hurt"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
