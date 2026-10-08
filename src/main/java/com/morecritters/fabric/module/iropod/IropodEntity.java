package com.morecritters.fabric.module.iropod;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Config;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.SwimmingFish;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * An armoured sea-floor crustacean. It sinks to the bottom and wanders there; out of the water, or when
 * hit in the water, it rolls up into its shell (and stops moving) until it is back in the water and a
 * moment has passed. Every hour or so it hops and sheds a molded shell. One in {@code black_iropod_chance}
 * spawns is a black iropod instead. A water bucket scoops it up.
 */
public class IropodEntity extends PathfinderMob implements GeoEntity {
	private static final double SWIM_SPEED = 2.5;
	private static final int FIRST_SHED = 12_000;
	private static final int SHED_INTERVAL = 72_000;
	private static final int HIDE_AFTER_HIT = 100;
	private static final int LOCK_SOUND_DELAY = 5;
	private static final int AIR_IN_WATER = 20;
	private static final double SINK_SPEED = -0.3;
	private static final double SHED_HOP = 0.2;
	private static final int MAX_SPAWN_Y = 45;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private boolean locked;
	private int hideTimer;
	private int shedTimer;

	public IropodEntity(EntityType<? extends IropodEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.moveControl = SwimmingFish.setUp(this);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 2.5)
			.add(Attributes.MAX_HEALTH, 35.0)
			.add(Attributes.ARMOR, 5.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 16.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
			.add(Attributes.STEP_HEIGHT, 0.6);
	}

	/** Natural spawns only below y 45. */
	public static boolean canSpawnAt(EntityType<IropodEntity> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
			RandomSource random) {
		return pos.getY() <= MAX_SPAWN_Y;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new RandomSwimmingGoal(this, 25.0, 40) {
			@Override
			public boolean canUse() {
				return !locked && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return !locked && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(2, new RandomLookAroundGoal(this) {
			@Override
			public boolean canUse() {
				return !locked && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return !locked && super.canContinueToUse();
			}
		});
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return SwimmingFish.navigation(this, level);
	}

	@Override
	protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
		SwimmingFish.swimFaster(this, SWIM_SPEED, input);
		super.travelInWater(input, baseGravity, isFalling, oldY);
	}

	/** The bucket a water bucket turns into when it scoops this iropod up. */
	protected Item bucketItem() {
		return IropodModule.iropodBucket;
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData data) {
		SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
		shedTimer = FIRST_SHED;
		if (reason == EntitySpawnReason.BUCKET) setPersistenceRequired();
		maybeTurnBlack(level);
		return result;
	}

	/** One in N iropods is replaced by a black iropod as it spawns. */
	protected void maybeTurnBlack(ServerLevelAccessor level) {
		int chance = Math.max(1, (int) Config.number("black_iropod_chance", 100.0));
		if (Mth.nextInt(getRandom(), 1, chance) != 1) return;
		discard();
		IropodEntity black = IropodModule.BLACK_IROPOD.spawn(level.getLevel(), blockPosition(), EntitySpawnReason.MOB_SUMMONED);
		if (black != null) {
			black.setYRot(getYRot());
			black.setYBodyRot(getYRot());
			black.setYHeadRot(getYRot());
			black.setDeltaMovement(Vec3.ZERO);
		}
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!(level() instanceof ServerLevel)) return;
		if (isInWater()) {
			tickInWater();
		} else {
			lock();
		}
		hideTimer--;
		tickShedding();
	}

	/** In the water it sinks to the floor and breathes; it unrolls once it is no longer hiding from a hit. */
	private void tickInWater() {
		if (locked && hideTimer <= 0) unlock();
		setAirSupply(AIR_IN_WATER);
		if (!onGround()) setDeltaMovement(0.0, SINK_SPEED, 0.0);
	}

	/** Counts down to the next moult; at the end it hops and leaves a molded shell behind. */
	private void tickShedding() {
		shedTimer--;
		if (shedTimer == 1) {
			setDeltaMovement(0.0, SHED_HOP, 0.0);
			Sounds.playAt(this, IropodModule.SHED_SOUND);
			spawnAtLocation((ServerLevel) level(), new ItemStack(IropodModule.moldedShell));
		}
		if (shedTimer <= 0) shedTimer = SHED_INTERVAL;
	}

	private void lock() {
		if (locked) return;
		locked = true;
		triggerAnim(Animations.ACTIONS, "lock");
		ServerScheduler.runLater(LOCK_SOUND_DELAY, () -> Sounds.playAt(this, IropodModule.LOCK_SOUND));
	}

	private void unlock() {
		locked = false;
		triggerAnim(Animations.ACTIONS, "unlock");
		Sounds.playAt(this, IropodModule.UNLOCK_SOUND);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypes.DROWN)) return false;
		boolean hurt = super.hurtServer(level, source, amount);
		if (isInWater()) {
			lock();
			hideTimer = HIDE_AFTER_HIT;
		}
		return hurt;
	}

	/** A water bucket in the main hand scoops the iropod up. */
	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		super.mobInteract(player, hand);
		if (player.getMainHandItem().is(Items.WATER_BUCKET)) {
			player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(bucketItem()));
			if (!level().isClientSide()) {
				Sounds.playAt(this, SoundEvents.BUCKET_FILL_FISH, SoundSource.BLOCKS, 1.0F, 1.0F);
				discard();
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Locked", locked);
		output.putInt("HideTimer", hideTimer);
		output.putInt("ShedTimer", shedTimer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		locked = input.getBooleanOr("Locked", false);
		hideTimer = input.getIntOr("HideTimer", 0);
		shedTimer = input.getIntOr("ShedTimer", 0);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return IropodModule.IDLE_SOUND;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return IropodModule.HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return IropodModule.DEATH_SOUND;
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
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(Animations.idleWalk(this, "idle", "walk"));
		controllers.add(Animations.actions(this, "lock", "unlock"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animations;
	}
}
