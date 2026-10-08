package com.morecritters.fabric.module.snowflake_spider;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.ClimbOnTopOfPowderSnowGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The snowflake spider: a small frosty spider of snowy biomes. It hunts players only where it is
 * dim (light level 11 or less), and its bite gives frostbite unless the server turns that off.
 */
public class SnowflakeSpiderEntity extends Monster implements GeoEntity {
	private static final int MAX_HUNTING_LIGHT = 11;
	private static final int FROSTBITE_TICKS = 200;

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
	private static final RawAnimation CHASE = RawAnimation.begin().thenLoop("chase");

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);

	public SnowflakeSpiderEntity(EntityType<? extends SnowflakeSpiderEntity> type, Level level) {
		super(type, level);
		this.xpReward = 3;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 15.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new ClimbOnTopOfPowderSnowGoal(this, this.level()));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false, false) {
			@Override
			public boolean canUse() {
				return isDimHere() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return isDimHere() && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, false));
		this.goalSelector.addGoal(4, new RandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(5, new LeapAtTargetGoal(this, 0.5F));
		this.targetSelector.addGoal(6, new HurtByTargetGoal(this));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(8, new FloatGoal(this));
	}

	/** It only hunts players where the light is low, like a vanilla spider in daylight. */
	private boolean isDimHere() {
		return level().getMaxLocalRawBrightness(blockPosition()) <= MAX_HUNTING_LIGHT;
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && target instanceof LivingEntity victim && Config.flag("snowflake_spider_inflict_frostbite", true)) {
			victim.addEffect(new MobEffectInstance(SnowflakeSpiderModule.FROSTBITE, FROSTBITE_TICKS, 0, false, true), this);
		}
		return hit;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() instanceof ServerPlayer player) {
			Advancements.award(player, MoreCritters.id("encounter_snowflake_spider"));
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SnowflakeSpiderModule.IDLE_SOUND;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SnowflakeSpiderModule.HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SnowflakeSpiderModule.DEATH_SOUND;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SnowflakeSpiderModule.STEP_SOUND, 0.15F, 1.0F);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// Walks while wandering, scuttles ("chase") while hunting, idles when still.
		controllers.add(new AnimationController<SnowflakeSpiderEntity>(Animations.MOVEMENT, 4, test -> {
			if (!test.isMoving()) {
				return test.setAndContinue(IDLE);
			}
			return test.setAndContinue(isAggressive() ? CHASE : WALK);
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animations;
	}
}
