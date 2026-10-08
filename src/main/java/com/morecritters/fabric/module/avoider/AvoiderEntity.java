package com.morecritters.fabric.module.avoider;

import com.morecritters.fabric.ids.CorpseGearIds;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.DryingOut;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.SwimmingFish;
import com.morecritters.fabric.core.TextureVariants;
import java.util.Comparator;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The avoider: an ocean fish that flees from any player it can see, bursting away with dolphin's
 * grace and a scared face. Every so often it leaps out of the water and glides back down. In its
 * panic it tears through kelp, and sometimes stuns itself on it, which is the moment to catch it.
 * Breeds on kelp; the offspring is an {@link AvoiderFryEntity}.
 */
public class AvoiderEntity extends Animal implements GeoEntity, TextureVariants {
	private static final EntityDataAccessor<Boolean> SCARED = SynchedEntityData.defineId(AvoiderEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> LEAPING = SynchedEntityData.defineId(AvoiderEntity.class, EntityDataSerializers.BOOLEAN);

	private static final double SWIM_SPEED = 1.5;
	private static final double WATCH_RANGE = 20.0;
	private static final int FIRST_LEAP_MIN = 150, FIRST_LEAP_MAX = 300;
	private static final int LEAP_INTERVAL_MIN = 200, LEAP_INTERVAL_MAX = 300;
	private static final int LEAP_DURATION = 200;
	private static final double LEAP_FORWARD = 0.4, LEAP_UP = 0.5;
	private static final int STUN_ODDS = 7;
	private static final int STUN_TICKS = 100;
	private static final double BREED_WITNESS_RANGE = 10.0;

	private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("swim");
	private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
	private static final RawAnimation LAND = RawAnimation.begin().thenLoop("land");
	private static final RawAnimation AIR = RawAnimation.begin().thenPlay("air");
	private static final RawAnimation JUMP = RawAnimation.begin().thenPlay("jump_start");

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private final DryingOut air = new DryingOut(this);
	private int ticksUntilLeap = FIRST_LEAP_MAX;
	private int leapTicksLeft;

	public AvoiderEntity(EntityType<? extends AvoiderEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.moveControl = SwimmingFish.setUp(this);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
			.add(Attributes.MOVEMENT_SPEED, 1.5)
			.add(Attributes.MAX_HEALTH, 10.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 16.0)
			.add(Attributes.STEP_HEIGHT, 0.6);
	}

	static boolean canSpawnIn(EntityType<? extends Mob> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return level.getBlockState(pos).is(Blocks.WATER) && level.getBlockState(pos.above()).is(Blocks.WATER);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SCARED, false);
		builder.define(LEAPING, false);
	}

	public boolean isScared() {
		return this.entityData.get(SCARED);
	}

	public boolean isLeaping() {
		return this.entityData.get(LEAPING);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return SwimmingFish.navigation(this, level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new BreedGoal(this, 1.0));
		this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 12.0F, 12.0, 12.0) {
			@Override
			public boolean canUse() {
				return !isStunned() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return !isStunned() && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(3, new RandomSwimmingGoal(this, 2.0, 40) {
			@Override
			public boolean canUse() {
				return !isStunned() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return !isStunned() && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
	}

	/** The stunned effect belongs to the corpse crew module; without it the avoider never stuns. */
	private static Optional<? extends Holder<MobEffect>> stunnedEffect() {
		return BuiltInRegistries.MOB_EFFECT.get(CorpseGearIds.Effects.STUNNED);
	}

	private boolean isStunned() {
		return stunnedEffect().map(this::hasEffect).orElse(false);
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.KELP);
	}

	@Override
	public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return AvoiderModule.AVOIDER.create(level, EntitySpawnReason.BREEDING);
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
		this.ticksUntilLeap = Mth.nextInt(this.getRandom(), FIRST_LEAP_MIN, FIRST_LEAP_MAX);
		this.setDeltaMovement(Vec3.ZERO);
		return super.finalizeSpawn(level, difficulty, reason, data);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!(this.level() instanceof ServerLevel level)) return;
		if (this.isBaby()) {
			hatchIntoFry(level);
			return;
		}
		trailBubbles(level);
		tickLeap();
		if (this.isInWater()) {
			setScared(nearestVisiblePlayerIsAThreat(level));
		} else {
			setScared(false);
			glideDown();
		}
		if (isScared()) {
			tearThroughKelp(level);
		}
		this.air.tick(level);
	}

	/** While swimming, it leaves a bubble behind now and then. */
	private void trailBubbles(ServerLevel level) {
		if (this.isInWater() && this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6 && this.getRandom().nextInt(3) == 0) {
			bubble(level);
		}
	}

	private void bubble(ServerLevel level) {
		level.sendParticles(ParticleTypes.BUBBLE, this.getX(), this.getY() + 0.2, this.getZ(), 1, 0.1, 0.1, 0.1, 0.1);
	}

	/**
	 * Every 10 to 15 seconds in water it starts a leap: it shoots forward and up until it breaks the
	 * surface (or hits a ceiling), then keeps one last push into the air.
	 */
	private void tickLeap() {
		if (--this.ticksUntilLeap < 0) {
			this.ticksUntilLeap = Mth.nextInt(this.getRandom(), LEAP_INTERVAL_MIN, LEAP_INTERVAL_MAX);
			if (this.isInWater()) {
				this.leapTicksLeft = LEAP_DURATION;
				this.entityData.set(LEAPING, true);
			}
		}
		if (!isLeaping()) return;
		if (this.isInWater() && blockedAbove()) {
			endLeap();
			return;
		}
		this.setDeltaMovement(this.getLookAngle().x * LEAP_FORWARD, LEAP_UP, this.getLookAngle().z * LEAP_FORWARD);
		if (!this.isInWater() || --this.leapTicksLeft <= 0) {
			endLeap();
		}
	}

	private boolean blockedAbove() {
		BlockState above = this.level().getBlockState(this.blockPosition().above());
		return !above.isAir() && !above.is(Blocks.WATER) && !above.is(Blocks.BUBBLE_COLUMN);
	}

	private void endLeap() {
		this.leapTicksLeft = 0;
		this.entityData.set(LEAPING, false);
		if (this.isInWater()) {
			this.triggerAnim(Animations.ACTIONS, "run_end");
		}
	}

	/** Out of water and in the air, it falls slowly and keeps gliding the way it faces. */
	private void glideDown() {
		if (this.onGround()) return;
		this.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 1, false, false));
		this.setDeltaMovement(this.getLookAngle().x * LEAP_FORWARD, this.getDeltaMovement().y, this.getLookAngle().z * LEAP_FORWARD);
	}

	/**
	 * The nearest player within ten blocks that it can see scares it, unless they are in creative.
	 * A scared avoider gets dolphin's grace and fizzes with bubbles.
	 */
	private boolean nearestVisiblePlayerIsAThreat(ServerLevel level) {
		Player nearest = level.getEntitiesOfClass(Player.class, AABB.ofSize(this.position(), WATCH_RANGE, WATCH_RANGE, WATCH_RANGE), EntitySelector.NO_SPECTATORS)
			.stream()
			.min(Comparator.comparingDouble(this::distanceToSqr))
			.orElse(null);
		if (nearest == null || !this.hasLineOfSight(nearest) || nearest.hasInfiniteMaterials()) {
			return false;
		}
		this.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 20, 2, false, false));
		bubble(level);
		return true;
	}

	private void setScared(boolean scared) {
		if (scared == isScared()) return;
		this.entityData.set(SCARED, scared);
		this.triggerAnim(Animations.ACTIONS, scared ? "run_start" : "run_end");
	}

	/** A scared avoider rips through the kelp it swims into, and one time in seven knocks itself out. */
	private void tearThroughKelp(ServerLevel level) {
		BlockPos pos = this.blockPosition();
		if (!level.getBlockState(pos).is(Blocks.KELP_PLANT)) return;
		level.destroyBlock(pos, false);
		if (this.getRandom().nextInt(STUN_ODDS) == 0) {
			Sounds.playAt(this, SoundEvents.ITEM_BREAK.value(), SoundSource.NEUTRAL, 1.0F, 1.0F);
			Sounds.playAt(this, AvoiderModule.STUN_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
			stunnedEffect().ifPresent(stunned -> this.addEffect(new MobEffectInstance(stunned, STUN_TICKS, 0, false, false)));
		}
	}

	/** A bred baby avoider is immediately replaced by a fry; players nearby earn the breeding advancement. */
	private void hatchIntoFry(ServerLevel level) {
		this.discard();
		AvoiderFryEntity fry = AvoiderModule.AVOIDER_FRY.spawn(level, this.blockPosition(), EntitySpawnReason.MOB_SUMMONED);
		if (fry != null) {
			fry.setYRot(this.getYRot());
			fry.setYBodyRot(this.getYRot());
			fry.setYHeadRot(this.getYRot());
			fry.setDeltaMovement(Vec3.ZERO);
		}
		for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(BREED_WITNESS_RANGE))) {
			Advancements.award(player, MoreCritters.id("breed_avoider"));
		}
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		InteractionResult result = super.mobInteract(player, hand);
		if (!this.level().isClientSide() && player.getMainHandItem().is(Items.WATER_BUCKET)) {
			player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AvoiderModule.avoiderBucket));
			Sounds.playAt(this, SoundEvents.BUCKET_FILL_FISH, SoundSource.BLOCKS, 1.0F, 1.0F);
			this.discard();
			return InteractionResult.SUCCESS;
		}
		return result.consumesAction() ? result : InteractionResult.SUCCESS;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		// It lives underwater, so vanilla's drowning must not apply; drying out replaces it.
		return !source.is(DamageTypes.DROWN) && super.hurtServer(level, source, amount);
	}

	@Override
	protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
		SwimmingFish.swimFaster(this, SWIM_SPEED, input);
		super.travelInWater(input, baseGravity, isFalling, oldY);
	}

	@Override
	public boolean checkSpawnObstruction(LevelReader level) {
		return level.isUnobstructed(this);
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("TicksUntilLeap", this.ticksUntilLeap);
		output.putInt("LeapTicksLeft", this.leapTicksLeft);
		this.air.save(output);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.ticksUntilLeap = input.getIntOr("TicksUntilLeap", FIRST_LEAP_MAX);
		this.leapTicksLeft = input.getIntOr("LeapTicksLeft", 0);
		this.entityData.set(LEAPING, this.leapTicksLeft > 0);
		this.air.load(input);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return AvoiderModule.HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return AvoiderModule.HURT_SOUND;
	}

	@Override
	public String textureName() {
		return isScared() ? "avoider_scared" : "avoider";
	}

	private PlayState animate(AnimationTest<AvoiderEntity> test) {
		if (!this.isInWater()) return test.setAndContinue(this.onGround() ? LAND : AIR);
		if (isLeaping()) return test.setAndContinue(JUMP);
		return test.setAndContinue(isScared() ? RUN : SWIM);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(Animations.MOVEMENT, 0, this::animate));
		controllers.add(Animations.actions(this, "run_start", "run_end"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
