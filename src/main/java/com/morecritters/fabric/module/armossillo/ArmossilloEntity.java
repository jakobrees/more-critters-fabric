package com.morecritters.fabric.module.armossillo;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Drops;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A mossy armadillo of the lush caves. Every twenty seconds it either sits down, if it is
 * standing still, or gets back up; sitting down it bone-meals the ground under it. Fed glow
 * berries, it sneezes a second later, scattering glowing ooze and recoiling backwards. It is
 * tempted and bred with spore blossoms; a bred baby turns straight into a moss clump, which
 * grows into a {@link BabyArmossilloEntity}.
 */
public class ArmossilloEntity extends Animal implements GeoEntity {
	private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(ArmossilloEntity.class, EntityDataSerializers.BOOLEAN);

	/** Ticks between sitting down and getting up again. */
	private static final int REST_INTERVAL = 400;
	/** Ticks after spawning before the first sit-down check. */
	private static final int FIRST_REST = 100;
	private static final int SIT_SOUND_DELAY = 7, RISE_SOUND_DELAY = 5;
	/** The ground is bone-mealed this long after sitting down, then twice more on the following ticks. */
	private static final int FERTILISE_DELAY = 15;
	/** Ticks from eating glow berries to the sneeze, and to the "about to sneeze" sound. */
	private static final int SNEEZE_DELAY = 20, SNEEZE_READY_SOUND_DELAY = 7;
	private static final int OOZE_MIN = 3, OOZE_MAX = 6;
	private static final double STILL_SPEED_SQR = 1.0E-6;
	private static final int BONE_MEAL_PARTICLES = 15;

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
	private static final RawAnimation SIT_DOWN = RawAnimation.begin().thenPlayAndHold("sit_down");
	private static final String SIT_UP = "sit_up", SNEEZE = "sneeze";

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	/** Ticks until it next sits down or gets up. */
	private int restTimer = FIRST_REST;
	/** Ticks until the sneeze after eating glow berries; 0 when none is coming. */
	private int sneezeTimer;

	public ArmossilloEntity(EntityType<? extends ArmossilloEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		// Animals, not Mob: TemptGoal needs the tempt_range attribute.
		return Animal.createAnimalAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			.add(Attributes.MAX_HEALTH, 35.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 16.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SITTING, false);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, whileStanding(new BreedGoal(this, 1.0)));
		this.goalSelector.addGoal(2, whileStanding(new TemptGoal(this, 1.0, this::isFood, false)));
		this.goalSelector.addGoal(3, whileStanding(new RandomStrollGoal(this, 0.7)));
		this.goalSelector.addGoal(4, whileStanding(new PanicGoal(this, 0.7)));
		this.goalSelector.addGoal(5, whileStanding(new LookAtPlayerGoal(this, Player.class, 6.0F)));
		this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(7, new FloatGoal(this));
	}

	/** A sitting armossillo does not walk, breed, follow food, panic or watch players. */
	private Goal whileStanding(Goal goal) {
		return new WrappedGoal(0, goal) {
			@Override
			public boolean canUse() {
				return !isSitting() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return !isSitting() && super.canContinueToUse();
			}
		};
	}

	public boolean isSitting() {
		return this.entityData.get(SITTING);
	}

	// --- ticking ----------------------------------------------------------------------

	@Override
	public void baseTick() {
		super.baseTick();
		if (!(this.level() instanceof ServerLevel level)) return;

		// Bred young do not grow up as armossillos: they become a moss clump at once.
		if (this.isBaby()) {
			becomeMossClump(level);
			return;
		}

		if (--this.restTimer <= 0) {
			this.restTimer = REST_INTERVAL;
			if (isSitting()) standUp();
			else if (this.getDeltaMovement().horizontalDistanceSqr() <= STILL_SPEED_SQR) sitDown(level);
		}

		if (this.sneezeTimer > 0 && --this.sneezeTimer == 0) sneeze();
	}

	private void becomeMossClump(ServerLevel level) {
		BlockPos pos = this.blockPosition();
		this.discard();
		level.setBlock(pos, ArmossilloBlocks.MOSS_CLUMP.defaultBlockState(), 3);
	}

	/** Sits down and, a moment later, bone-meals the block it sits on and the one under it. */
	private void sitDown(ServerLevel level) {
		this.entityData.set(SITTING, true);
		ServerScheduler.runLater(SIT_SOUND_DELAY, () -> Sounds.playAt(this, ArmossilloEntities.SIT_SOUND));

		BlockPos feet = this.blockPosition();
		ServerScheduler.runLater(FERTILISE_DELAY, () -> {
			fertilise(level, feet.below());
			fertilise(level, feet);
		});
		ServerScheduler.runLater(FERTILISE_DELAY + 1, () -> fertilise(level, feet));
		ServerScheduler.runLater(FERTILISE_DELAY + 2, () -> fertilise(level, feet));
	}

	private void standUp() {
		this.entityData.set(SITTING, false);
		ServerScheduler.runLater(RISE_SOUND_DELAY, () -> Sounds.playAt(this, ArmossilloEntities.RISE_SOUND));
		triggerAnim(Animations.ACTIONS, SIT_UP);
	}

	/** One application of bone meal, as if a player had used it, without using up an item. */
	private static void fertilise(ServerLevel level, BlockPos pos) {
		if (BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, pos)
			|| BoneMealItem.growWaterPlant(new ItemStack(Items.BONE_MEAL), level, pos, null)) {
			level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, BONE_MEAL_PARTICLES);
		}
	}

	/** Scatters glowing ooze and is thrown backwards by the force of it. */
	private void sneeze() {
		Sounds.playAt(this, ArmossilloEntities.SNEEZE_SOUND);
		Drops.dropSingles(this, ArmossilloItems.GLOWING_OOZE, Drops.randomCount(this, OOZE_MIN, OOZE_MAX));
		Vec3 look = this.getLookAngle();
		this.push(-look.x, -look.y, -look.z);
	}

	// --- interaction ------------------------------------------------------------------

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getMainHandItem();
		if (hand == InteractionHand.MAIN_HAND && held.is(Items.GLOW_BERRIES) && this.sneezeTimer <= 0) {
			if (this.level() instanceof ServerLevel) feedGlowBerries(player, held);
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	/** Eats the berries; the sneeze follows a second later. */
	private void feedGlowBerries(Player player, ItemStack berries) {
		Sounds.playAt(this, SoundEvents.DOLPHIN_EAT);
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		this.sneezeTimer = SNEEZE_DELAY;
		ServerScheduler.runLater(SNEEZE_READY_SOUND_DELAY, () -> Sounds.playAt(this, ArmossilloEntities.SNEEZE_READY_SOUND));
		if (!player.hasInfiniteMaterials()) berries.shrink(1);
		triggerAnim(Animations.ACTIONS, SNEEZE);
	}

	// --- saving -----------------------------------------------------------------------

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Sitting", isSitting());
		output.putInt("RestTimer", this.restTimer);
		output.putInt("SneezeTimer", this.sneezeTimer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.entityData.set(SITTING, input.getBooleanOr("Sitting", false));
		this.restTimer = input.getIntOr("RestTimer", FIRST_REST);
		this.sneezeTimer = input.getIntOr("SneezeTimer", 0);
	}

	// --- vanilla hooks ----------------------------------------------------------------

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Blocks.SPORE_BLOSSOM.asItem());
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return ArmossilloEntities.ARMOSSILLO.create(level, EntitySpawnReason.BREEDING);
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).scale(1.2F);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ArmossilloEntities.IDLE_SOUND;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ArmossilloEntities.HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ArmossilloEntities.DEATH_SOUND;
	}

	// --- GeckoLib ---------------------------------------------------------------------

	/**
	 * Idle/walk while standing; the sit-down pose held while sitting; getting up and sneezing
	 * as one-shot animations on top.
	 */
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<ArmossilloEntity>(Animations.MOVEMENT, 2,
			test -> isSitting() ? PlayState.STOP : test.setAndContinue(test.isMoving() ? WALK : IDLE)));
		controllers.add(new AnimationController<ArmossilloEntity>(Animations.ACTIONS, 2,
			test -> isSitting() ? test.setAndContinue(SIT_DOWN) : PlayState.STOP)
			.triggerableAnim(SIT_UP, RawAnimation.begin().thenPlay(SIT_UP))
			.triggerableAnim(SNEEZE, RawAnimation.begin().thenPlay(SNEEZE)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
