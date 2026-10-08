package com.morecritters.fabric.module.corpse_crew;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A corpse parrot of one's own, from the corpse parrot item. It follows its owner, fights what
 * the owner fights or what attacks the owner, and pelts targets with pebbles from the air like its
 * wild kin. Right-clicking it tells it to sit or get up; an ownerless one is claimed by the first
 * player to right-click it. It eats nothing and never breeds. No fall or explosion damage.
 */
public class TamedCorpseParrotEntity extends TamableAnimal implements GeoEntity {
	private static final double PECK_REACH_SQR = 1.69;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private final ParrotHabits<TamedCorpseParrotEntity> habits = new ParrotHabits<>(this, SoundSource.NEUTRAL);

	public TamedCorpseParrotEntity(EntityType<? extends TamedCorpseParrotEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setPersistenceRequired();
		this.moveControl = new FlyingMoveControl<>(this, 10, true);
		this.setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 25.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 16.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FLYING_SPEED, 0.3);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new FlyingPathNavigation(this, level);
	}

	/** Each movement goal comes in an airborne and a grounded copy; none runs while it sits. */
	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, peck(2.0, false));
		this.goalSelector.addGoal(2, peck(1.0, true));
		this.targetSelector.addGoal(3, new OwnerHurtTargetGoal(this) {
			@Override public boolean canUse() { return super.canUse() && !isOrderedToSit(); }
			@Override public boolean canContinueToUse() { return super.canContinueToUse() && !isOrderedToSit(); }
		});
		this.targetSelector.addGoal(4, new OwnerHurtByTargetGoal(this) {
			@Override public boolean canUse() { return super.canUse() && !isOrderedToSit(); }
			@Override public boolean canContinueToUse() { return super.canContinueToUse() && !isOrderedToSit(); }
		});
		this.goalSelector.addGoal(5, followOwner(2.0, false));
		this.goalSelector.addGoal(6, followOwner(1.0, true));
		this.goalSelector.addGoal(7, new ParrotFlight(this, 2.0, () -> mayMove(false)));
		this.goalSelector.addGoal(8, new ParrotFlight(this, 2.0, () -> mayMove(true)));
		this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(10, new FloatGoal(this));
	}

	private boolean mayMove(boolean grounded) {
		return !this.isOrderedToSit() && this.onGround() == grounded;
	}

	private MeleeAttackGoal peck(double speed, boolean grounded) {
		return new MeleeAttackGoal(this, speed, false) {
			@Override
			protected boolean canPerformAttack(LivingEntity target) {
				return this.isTimeToAttack() && this.mob.distanceToSqr(target) < PECK_REACH_SQR && this.mob.getSensing().hasLineOfSight(target);
			}

			@Override public boolean canUse() { return super.canUse() && mayMove(grounded); }
			@Override public boolean canContinueToUse() { return super.canContinueToUse() && mayMove(grounded); }
		};
	}

	private FollowOwnerGoal followOwner(double speed, boolean grounded) {
		return new FollowOwnerGoal(this, speed, 5.0F, 2.0F) {
			@Override public boolean canUse() { return super.canUse() && mayMove(grounded); }
			@Override public boolean canContinueToUse() { return super.canContinueToUse() && mayMove(grounded); }
		};
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		level.getLevel().sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 1.0, this.getZ(), 5, 0.5, 0.5, 0.5, 1.0);
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level() instanceof ServerLevel) {
			this.habits.tick();
			// A sitting parrot sinks to the ground and stays there.
			if (this.isOrderedToSit() && !this.onGround()) {
				this.setDeltaMovement(0.0, -0.2, 0.0);
			}
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		this.setNoGravity(true);
	}

	// --- Owner ------------------------------------------------------------------------

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.getItem() instanceof SpawnEggItem) {
			return super.mobInteract(player, hand);
		}
		if (this.level().isClientSide()) {
			return this.isTame() && this.isOwnedBy(player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
		}
		InteractionResult result = super.mobInteract(player, hand);
		if (this.isTame() && this.isOwnedBy(player)) {
			toggleSitting();
			return InteractionResult.SUCCESS;
		}
		if (!this.isTame()) {
			this.tame(player);
			return InteractionResult.SUCCESS;
		}
		return result;
	}

	private void toggleSitting() {
		boolean sit = !this.isOrderedToSit();
		this.setOrderedToSit(sit);
		this.setInSittingPose(sit);
		if (!sit) {
			this.triggerAnim(Animations.ACTIONS, "sitting_end");
		}
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return false;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return CorpseCrewModule.TAMED_CORPSE_PARROT.create(level, EntitySpawnReason.BREEDING);
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	// --- Combat -----------------------------------------------------------------------

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		this.triggerAnim(Animations.ACTIONS, "attack");
		return super.doHurtTarget(level, target);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return !source.is(DamageTypes.FALL) && !source.is(DamageTypes.EXPLOSION) && super.hurtServer(level, source, damage);
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
		return false;
	}

	@Override protected SoundEvent getAmbientSound() { return CorpseCrewModule.TAMED_PARROT_IDLE; }
	@Override protected SoundEvent getHurtSound(DamageSource source) { return CorpseCrewModule.TAMED_PARROT_HURT; }
	@Override protected SoundEvent getDeathSound() { return CorpseCrewModule.TAMED_PARROT_DEATH; }

	// --- Saving and animation ---------------------------------------------------------

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		this.habits.save(output);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.habits.load(input);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<TamedCorpseParrotEntity>(Animations.MOVEMENT, 2,
			test -> test.setAndContinue(ParrotPoses.of(this, test.isMoving(), this.isInSittingPose()))));
		controllers.add(Animations.actions(this, "attack", "throw", "ready", "sitting_end"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
