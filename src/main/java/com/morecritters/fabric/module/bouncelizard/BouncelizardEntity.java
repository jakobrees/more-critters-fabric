package com.morecritters.fabric.module.bouncelizard;

import com.morecritters.fabric.ids.NightshroomIds;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Config;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.TextureVariants;
import com.morecritters.fabric.ids.SnowflakeSpiderIds;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A jungle lizard you can bounce on: anything that lands on an adult is flung upward, twice as high
 * while it sleeps. Feeding it a bounceberry puts it to sleep, a milk bucket wakes it. Bred pairs
 * do not get a baby but lay a clutch of one to four eggs, which hatch after about a day.
 * Some names give it a special skin.
 */
public class BouncelizardEntity extends Animal implements GeoEntity, TextureVariants {
	private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(BouncelizardEntity.class, EntityDataSerializers.BOOLEAN);
	private static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("sleep");
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
	private static final float BABY_HITBOX_SCALE = 0.6F;
	private static final double BOUNCE_REACH = 0.65;
	/** Names of the people behind each skin; "bouncelizard<N>" is the texture. */
	private static final Map<String, Integer> NAMED_SKINS = Map.ofEntries(
		Map.entry("Skippy", 1), Map.entry("Skippybuttersz", 1), Map.entry("Skipster43", 1), Map.entry("Skipster", 1),
		Map.entry("Dusty", 2), Map.entry("athe7revx", 2), Map.entry("Cutie", 3), Map.entry("noname_thing", 3),
		Map.entry("Waffle", 4), Map.entry("BelgianCatWaffle", 4), Map.entry("Rubber", 5), Map.entry("Caticorny", 5),
		Map.entry("Offcolor", 6), Map.entry("Lexpresstart", 6), Map.entry("Coralee", 7), Map.entry("ProfessorScottie", 7),
		Map.entry("Breakfast", 8), Map.entry("VictorGraves", 8), Map.entry("Wild", 9), Map.entry("Yarilis20", 9));

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	/** Hatched from an egg block; other babies (from breeding) turn into a clutch of eggs at once. */
	private boolean fromEgg;
	private int snoreTimer;
	private int idleSoundTimer;

	public BouncelizardEntity(EntityType<? extends BouncelizardEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			.add(Attributes.MAX_HEALTH, 10.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 64.0);
	}

	public static boolean canSpawnAt(EntityType<BouncelizardEntity> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON) && level.getRawBrightness(pos, 0) > 8;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SLEEPING, false);
	}

	public boolean sleeping() {
		return this.entityData.get(SLEEPING);
	}

	void markHatched() {
		this.fromEgg = true;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, whileAwake(new BreedGoal(this, 1.0)));
		this.goalSelector.addGoal(2, whileAwake(new TemptGoal(this, 1.0, this::isFood, false)));
		this.goalSelector.addGoal(3, whileAwake(new RandomStrollGoal(this, 1.0)));
		this.goalSelector.addGoal(4, whileAwake(new RandomLookAroundGoal(this)));
		this.goalSelector.addGoal(5, new FloatGoal(this));
	}

	/** A sleeping bouncelizard stays put: the goal neither starts nor continues while it sleeps. */
	private Goal whileAwake(Goal goal) {
		return new Goal() {
			{ setFlags(goal.getFlags()); }
			@Override public boolean canUse() { return !sleeping() && goal.canUse(); }
			@Override public boolean canContinueToUse() { return !sleeping() && goal.canContinueToUse(); }
			@Override public void start() { goal.start(); }
			@Override public void stop() { goal.stop(); }
			@Override public boolean requiresUpdateEveryTick() { return goal.requiresUpdateEveryTick(); }
			@Override public void tick() { goal.tick(); }
		};
	}

	@Override
	public String textureName() {
		String skin = "bouncelizard" + NAMED_SKINS.getOrDefault(getCustomName() == null ? "" : getCustomName().getString(), 0);
		return sleeping() ? skin + "_sleep" : skin;
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		this.snoreTimer = Mth.nextInt(this.random, 60, 120);
		this.idleSoundTimer = Mth.nextInt(this.random, 100, 300);
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (isBaby()) {
			if (!this.fromEgg && level() instanceof ServerLevel) layEggs();
		} else {
			bounceLandingEntities();
		}
		if (level() instanceof ServerLevel level) {
			if (sleeping() && this.random.nextInt(6) == 0) {
				ParticleOptions zzz = BouncelizardModule.sleepParticle();
				if (zzz != null) level.sendParticles(zzz, getX(), getY() + 0.5, getZ(), 2, 0.5, 0.04, 0.5, 0.01);
			}
			tickSounds();
		}
	}

	/** A baby that did not hatch (a bred one) becomes a clutch of one to four eggs where it stands. */
	private void layEggs() {
		int[] clutchStates = {0, 2, 3, 4};
		BlockPos pos = blockPosition();
		level().setBlock(pos, BouncelizardModule.EGG.defaultBlockState()
			.setValue(BouncelizardEggBlock.EGGS, clutchStates[this.random.nextInt(clutchStates.length)]), 3);
		discard();
	}

	/** Anything airborne that touches an adult is flung upward; a sleeping one throws it higher. */
	private void bounceLandingEntities() {
		Vec3 center = position();
		for (Entity other : level().getEntities(this, new AABB(center, center).inflate(BOUNCE_REACH), this::isBounceable)) {
			boolean big = sleeping();
			double speed = big
				? Config.number("bouncelizard_sleeping_jump_height", 1.2)
				: Config.number("bouncelizard_regular_jump_height", 0.6);
			Vec3 motion = other.getDeltaMovement();
			other.setDeltaMovement(motion.x, speed, motion.z);
			if (level() instanceof ServerLevel level) {
				other.syncVelocity = true;
				// Holds still for a moment while it squashes.
				addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 15, 30, false, false));
				triggerAnim(Animations.ACTIONS, big ? "bounce_big" : "bounce");
				if (big) {
					Sounds.playAt(this, BouncelizardModule.BIG_BOUNCE_SOUND, SoundSource.BLOCKS, 1.0F, 1.0F);
					boostTrail(level, center, 0);
				} else {
					Sounds.playAt(this, BouncelizardModule.BOUNCE_SOUND, SoundSource.BLOCKS, 1.0F, 1.0F);
				}
			}
		}
	}

	private boolean isBounceable(Entity other) {
		return !(other instanceof BouncelizardEntity) && !(other instanceof ExperienceOrb) && !other.onGround()
			&& !isOtherModuleType(other, SnowflakeSpiderIds.Entities.WEB_ENTITY) && !isOtherModuleType(other, NightshroomIds.Entities.MORI_ROOTS);
	}

	private static boolean isOtherModuleType(Entity entity, net.minecraft.resources.Identifier id) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(id);
	}

	/** Boost particles rising one block every two ticks, four puffs in all. */
	private static void boostTrail(ServerLevel level, Vec3 origin, int step) {
		double[] heights = {0.7, 1.5, 2.5, 3.5};
		ParticleOptions boost = BouncelizardModule.boostParticle();
		if (boost != null) level.sendParticles(boost, origin.x, origin.y + heights[step], origin.z, 2, 0.4, 0.2, 0.4, 0.0);
		if (step + 1 < heights.length) ServerScheduler.runLater(2, () -> boostTrail(level, origin, step + 1));
	}

	private void tickSounds() {
		if (--this.snoreTimer < 0) {
			this.snoreTimer = Mth.nextInt(this.random, 60, 120);
			if (sleeping()) {
				SoundEvent snore = this.random.nextInt(100) == 0 ? BouncelizardModule.SNORE_MIMIMI_SOUND : BouncelizardModule.SNORE_SOUND;
				Sounds.playAt(this, snore, SoundSource.VOICE, 1.0F, 1.0F);
			}
		}
		if (--this.idleSoundTimer < 0) {
			this.idleSoundTimer = Mth.nextInt(this.random, 100, 300);
			if (!sleeping()) Sounds.playAt(this, BouncelizardModule.IDLE_SOUND, SoundSource.VOICE, 1.0F, 1.0F);
		}
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		InteractionResult result = super.mobInteract(player, hand);
		if (hand != InteractionHand.MAIN_HAND || isBaby()) return result;
		ItemStack held = player.getMainHandItem();
		if (!sleeping() && BouncelizardItems.isBounceberry(held)) {
			held.shrink(1);
			player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
			if (level() instanceof ServerLevel level) {
				level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, BouncelizardItems.bounceberry), getX(), getY() + 0.3, getZ(), 5, 0.1, 0.02, 0.01, 0.02);
			}
			Sounds.playAt(this, SoundEvents.STRIDER_EAT, SoundSource.VOICE, 1.0F, 1.0F);
			this.entityData.set(SLEEPING, true);
			return InteractionResult.SUCCESS;
		}
		if (sleeping() && held.is(Items.MILK_BUCKET)) {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
			player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
			Sounds.playAt(this, SoundEvents.GENERIC_DRINK.value(), SoundSource.VOICE, 1.0F, 1.0F);
			this.entityData.set(SLEEPING, false);
			return InteractionResult.SUCCESS;
		}
		return result;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return !source.is(DamageTypes.FALL) && super.hurtServer(level, source, amount);
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.SPIDER_EYE);
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return BouncelizardModule.BOUNCELIZARD.create(level, EntitySpawnReason.BREEDING);
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).scale(isBaby() ? BABY_HITBOX_SCALE : 1.0F);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Sleeping", sleeping());
		output.putBoolean("FromEgg", this.fromEgg);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.entityData.set(SLEEPING, input.getBooleanOr("Sleeping", false));
		this.fromEgg = input.getBooleanOr("FromEgg", false);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return BouncelizardModule.HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return BouncelizardModule.DEATH_SOUND;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<BouncelizardEntity>(Animations.MOVEMENT, 2,
			test -> test.setAndContinue(sleeping() ? SLEEP : test.isMoving() ? WALK : IDLE)));
		controllers.add(Animations.actions(this, "bounce", "bounce_big"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
