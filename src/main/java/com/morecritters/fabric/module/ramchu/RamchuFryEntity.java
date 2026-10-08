package com.morecritters.fabric.module.ramchu;

import com.morecritters.fabric.core.DryingOut;
import com.morecritters.fabric.core.SwimmingFish;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Sounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A ramchu fry: hatched from breeding, it swims around for a full day and then grows into a
 * {@link RamchuEntity}. On land it flops about and dries out. Never despawns.
 */
public class RamchuFryEntity extends PathfinderMob implements GeoEntity {
	private static final int GROW_UP_TICKS = 24_000;
	private static final double SWIM_SPEED = 3.0;
	private static final double FLOP_SPREAD = 0.2;
	private static final double FLOP_HEIGHT = 0.3;

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("swim");
	private static final RawAnimation FLOPPING = RawAnimation.begin().thenLoop("land");

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private final DryingOut air = new DryingOut(this);
	private int ticksUntilGrown = GROW_UP_TICKS;

	public RamchuFryEntity(EntityType<? extends RamchuFryEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setPersistenceRequired();
		this.moveControl = SwimmingFish.setUp(this);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 3.0)
			.add(Attributes.MAX_HEALTH, 3.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 16.0)
			.add(Attributes.STEP_HEIGHT, 0.6);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return SwimmingFish.navigation(this, level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new RandomSwimmingGoal(this, 1.0, 40));
		this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level() instanceof ServerLevel level) {
			if (--this.ticksUntilGrown == 0) {
				growUp(level);
			}
			if (!this.isInWater() && this.onGround()) {
				flop();
			}
			this.air.tick(level);
		}
	}

	private void growUp(ServerLevel level) {
		this.discard();
		RamchuEntity adult = RamchuModule.RAMCHU.spawn(level, this.blockPosition(), EntitySpawnReason.MOB_SUMMONED);
		if (adult != null) {
			adult.setYRot(this.getYRot());
			adult.setYBodyRot(this.getYRot());
			adult.setYHeadRot(this.getYRot());
			adult.setDeltaMovement(Vec3.ZERO);
		}
	}

	/** A beached fry hops in a random direction every tick it touches the ground. */
	private void flop() {
		this.setDeltaMovement(new Vec3(
			Mth.nextDouble(this.getRandom(), -FLOP_SPREAD, FLOP_SPREAD),
			FLOP_HEIGHT,
			Mth.nextDouble(this.getRandom(), -FLOP_SPREAD, FLOP_SPREAD)));
		Sounds.playAt(this, SoundEvents.TROPICAL_FISH_FLOP, SoundSource.NEUTRAL, 1.0F, 1.0F);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		super.mobInteract(player, hand);
		if (!this.level().isClientSide() && player.getMainHandItem().is(Items.WATER_BUCKET)) {
			player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(RamchuModule.fryBucket));
			Sounds.playAt(this, SoundEvents.BUCKET_FILL_FISH, SoundSource.BLOCKS, 1.0F, 1.0F);
			this.discard();
		}
		return InteractionResult.SUCCESS;
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
		output.putInt("TicksUntilGrown", this.ticksUntilGrown);
		this.air.save(output);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.ticksUntilGrown = input.getIntOr("TicksUntilGrown", GROW_UP_TICKS);
		this.air.load(input);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.COD_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.COD_DEATH;
	}

	private PlayState animate(AnimationTest<RamchuFryEntity> test) {
		if (!this.isInWater()) return test.setAndContinue(FLOPPING);
		return test.setAndContinue(test.isMoving() ? SWIM : IDLE);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>("body", 4, this::animate));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
