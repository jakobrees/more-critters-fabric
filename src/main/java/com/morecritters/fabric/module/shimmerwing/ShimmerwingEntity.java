package com.morecritters.fabric.module.shimmerwing;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Config;
import com.morecritters.fabric.core.Drops;
import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import java.util.Comparator;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The shimmerwing: an End moth that drifts about shedding shimmer. Brushing it sheds End dust;
 * feeding it chorus fruit grants the feeder End's Blessing, after which it sits chewing for two
 * seconds and blinks away. It follows blessed creatures, hops now and then when grounded, and
 * blinks away when wet or stuck in a wall.
 */
public class ShimmerwingEntity extends PathfinderMob implements GeoEntity {
	private static final int HOP_INTERVAL_MIN = 100, HOP_INTERVAL_MAX = 300;
	private static final int DIGEST_TICKS = 40;
	private static final int CHEW_SOUND_INTERVAL = 8;
	private static final int BLESSING_TICKS = 2400;
	private static final int BRUSH_WEAR = 8;
	private static final double FOLLOW_RADIUS = 10.0;
	private static final double BLINK_MAX_RISE = 3.0;

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
	private static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private int ticksUntilHop = HOP_INTERVAL_MAX;
	private int ticksUntilChew = 5;
	/** Ticks left chewing a chorus fruit; it blinks away when this reaches one. */
	private int digestTicks;
	/** The last one-shot animation the server played; "ready" and "land" let it lift off the ground. */
	private String lastAction = "";

	public ShimmerwingEntity(EntityType<? extends ShimmerwingEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.moveControl = new FlyingMoveControl(this, 10, true);
		this.setNoGravity(true);
		this.ticksUntilHop = Mth.nextInt(this.getRandom(), HOP_INTERVAL_MIN, HOP_INTERVAL_MAX);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 7.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 16.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.TEMPT_RANGE, 10.0)
			.add(Attributes.FLYING_SPEED, 0.3);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new FlyingPathNavigation(this, level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(2, new TemptGoal(this, 1.0, stack -> stack.is(Items.CHORUS_FRUIT), false) {
			@Override
			public boolean canUse() {
				return !isDigesting() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return !isDigesting() && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.8, 20) {
			/** Wanders anywhere in a 16-block cube, up and down as well. */
			@Override
			protected @Nullable Vec3 getPosition() {
				return ShimmerwingEntity.this.position().add(spread(), spread(), spread());
			}

			private double spread() {
				return (ShimmerwingEntity.this.getRandom().nextFloat() * 2.0F - 1.0F) * 16.0F;
			}

			@Override
			public boolean canUse() {
				return !isDigesting() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return !isDigesting() && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(5, new FloatGoal(this));
	}

	private boolean isDigesting() {
		return this.digestTicks > 1;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		this.setNoGravity(true);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}
		followBlessedCreatures(level);
		this.digestTicks--;
		hopNowAndThen();
		chewWhileDigesting();
		shedShimmer();
		if (this.onGround() && !this.lastAction.equals("ready") && !this.lastAction.equals("land")) {
			this.setDeltaMovement(0.0, -0.1, 0.0);
		}
		if (this.isInWaterOrRain()) {
			this.hurtServer(level, this.damageSources().generic(), 1.0F);
			EnderBlink.blinkAway(this, BLINK_MAX_RISE);
		}
		if (this.isInWall()) {
			EnderBlink.blinkAway(this, BLINK_MAX_RISE);
		}
		if (isDigesting()) {
			Particles.spawnAt(this, new ItemParticleOption(ParticleTypes.ITEM, Items.CHORUS_FRUIT), 1, 0.1, 0.1, 0.1, 0.02);
			this.setDeltaMovement(0.0, -0.1, 0.0);
		} else if (this.digestTicks == 1) {
			EnderBlink.blinkAway(this, BLINK_MAX_RISE);
		}
	}

	/** Flies to two blocks above a nearby creature that has End's Blessing. */
	private void followBlessedCreatures(ServerLevel level) {
		if (isDigesting() || !Config.flag("shimmerwing_follow_blessed_player", true)) {
			return;
		}
		level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(FOLLOW_RADIUS),
				creature -> !(creature instanceof ShimmerwingEntity) && creature.hasEffect(ShimmerwingModule.ENDS_BLESSING))
			.stream()
			.min(Comparator.comparingDouble(this::distanceToSqr))
			.ifPresent(blessed -> this.getNavigation().moveTo(blessed.getX(), blessed.getY() + 2.0, blessed.getZ(), 1.0));
	}

	/** Every five to fifteen seconds, if grounded, crouches for a moment and then hops forward. */
	private void hopNowAndThen() {
		if (--this.ticksUntilHop != 1) {
			return;
		}
		this.ticksUntilHop = Mth.nextInt(this.getRandom(), HOP_INTERVAL_MIN, HOP_INTERVAL_MAX);
		if (this.onGround()) {
			playAction("ready");
			this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 5, 29, false, false));
			ServerScheduler.runLater(7, () -> {
				Vec3 look = this.getLookAngle();
				this.setDeltaMovement(look.x * 0.2, 0.2, look.z * 0.2);
			});
		}
	}

	private void chewWhileDigesting() {
		if (--this.ticksUntilChew > -1) {
			return;
		}
		this.ticksUntilChew = CHEW_SOUND_INTERVAL - 1;
		if (isDigesting()) {
			Sounds.playAt(this, SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 1.0F, 1.0F);
		}
	}

	/** One tick in five, leaves one or two motes of shimmer behind. */
	private void shedShimmer() {
		if (this.getRandom().nextInt(5) != 0) {
			return;
		}
		int motes = Mth.nextInt(this.getRandom(), 1, 2);
		for (int i = 0; i < motes; i++) {
			Particles.spawnAt(this, ShimmerwingModule.SHIMMER, 1, 0.3, 0.03, 0.03, 0.02);
		}
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getMainHandItem();
		if (hand == InteractionHand.MAIN_HAND && held.is(Items.BRUSH)) {
			brushOffDust(player, held);
			return InteractionResult.SUCCESS;
		}
		if (hand == InteractionHand.MAIN_HAND && held.is(Items.CHORUS_FRUIT) && !isDigesting()) {
			eatChorusFrom(player, held);
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	/** Brushing sheds a pinch of End dust and wears the brush. */
	private void brushOffDust(Player player, ItemStack brush) {
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}
		Sounds.playAt(this, ShimmerwingModule.SHED_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
		Drops.dropSingles(level, this.position().add(0.5, 0.0, 0.5), ShimmerwingModule.endDust, 1);
		if (!player.hasInfiniteMaterials()) {
			brush.hurtAndBreak(BRUSH_WEAR, level, null, item -> {});
		}
	}

	/** Takes the chorus fruit, blesses the feeder, then chews for two seconds before blinking away. */
	private void eatChorusFrom(Player player, ItemStack fruit) {
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		if (this.level().isClientSide()) {
			return;
		}
		Sounds.playAt(this, ShimmerwingModule.GIFT_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
		playAction("eat");
		player.addEffect(new MobEffectInstance(ShimmerwingModule.ENDS_BLESSING, BLESSING_TICKS, 0, false, true));
		if (!player.hasInfiniteMaterials()) {
			fruit.shrink(1);
		}
		this.digestTicks = DIGEST_TICKS;
	}

	/** Knocked off the ground by a hit, it flutters back up. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (this.onGround()) {
			playAction("land");
			this.setDeltaMovement(0.0, 0.2, 0.0);
		}
		return !source.is(DamageTypeTags.IS_FALL) && super.hurtServer(level, source, amount);
	}

	private void playAction(String name) {
		this.lastAction = name;
		this.triggerAnim(Animations.ACTIONS, name);
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
		return false;
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
		output.putInt("TicksUntilHop", this.ticksUntilHop);
		output.putInt("DigestTicks", this.digestTicks);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.ticksUntilHop = input.getIntOr("TicksUntilHop", HOP_INTERVAL_MAX);
		this.digestTicks = input.getIntOr("DigestTicks", 0);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ShimmerwingModule.IDLE_SOUND;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ShimmerwingModule.HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ShimmerwingModule.DEATH_SOUND;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<ShimmerwingEntity>(Animations.MOVEMENT, 2, test -> {
			if (!this.onGround()) {
				return test.setAndContinue(FLY);
			}
			return test.setAndContinue(test.isMoving() ? WALK : IDLE);
		}));
		controllers.add(Animations.actions(this, "land", "ready", "eat"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
