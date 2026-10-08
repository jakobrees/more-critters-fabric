package com.morecritters.fabric.module.ramchu;

import com.morecritters.fabric.ids.CorpseGearIds;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.DryingOut;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.SwimmingFish;
import com.morecritters.fabric.core.TextureVariants;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Bucketable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The ramchu: a stone-shelled fish of underground aquifers. Every few seconds it picks a player
 * swimming in sight and rams them at full tilt. Ramming into a wall busts its shell and stuns it.
 * A shell-less ramchu can be milked with a glass bottle for ramchu oil; the oil grows back after a
 * while. Breeds with glow lichen into {@link RamchuFryEntity fry}; dries out on land.
 */
public class RamchuEntity extends Animal implements GeoEntity, TextureVariants, Bucketable {
	/** The shell state, saved under the original's key and carried in its bucket. */
	static final String SHELL_STATE_KEY = "Datastate";

	private static final double SWIM_SPEED = 2.0;
	private static final double RAM_SPEED = 100.0;
	private static final int FIRST_RAM_DELAY = 100;
	private static final int RAM_DELAY_MIN = 70, RAM_DELAY_MAX = 200;
	private static final double RAM_SIGHT_RANGE = 10.0;
	private static final int OIL_REGROW_MIN = 400, OIL_REGROW_MAX = 10_000;
	private static final int STUN_TICKS = 40;
	private static final double RECOIL = 0.4;

	private static final EntityDataAccessor<Integer> SHELL_STATE = SynchedEntityData.defineId(RamchuEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> RAMMING = SynchedEntityData.defineId(RamchuEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> FROM_BUCKET = SynchedEntityData.defineId(RamchuEntity.class, EntityDataSerializers.BOOLEAN);

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("swim");
	private static final RawAnimation RAM = RawAnimation.begin().thenLoop("ram");

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private final DryingOut air = new DryingOut(this);
	private int ramCooldown = FIRST_RAM_DELAY;
	/** Counts down after milking; the oil is back when it reaches zero. Zero means not regrowing. */
	private int oilRegrowTicks;

	public RamchuEntity(EntityType<? extends RamchuEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.moveControl = SwimmingFish.setUp(this);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
			.add(Attributes.MOVEMENT_SPEED, 2.0)
			.add(Attributes.MAX_HEALTH, 20.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 32.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
			.add(Attributes.STEP_HEIGHT, 0.6);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SHELL_STATE, ShellState.SHELLED.ordinal());
		builder.define(RAMMING, false);
		builder.define(FROM_BUCKET, false);
	}

	public ShellState shellState() {
		return ShellState.byId(this.entityData.get(SHELL_STATE));
	}

	void setShellState(ShellState state) {
		this.entityData.set(SHELL_STATE, state.ordinal());
	}

	private boolean isRamming() {
		return this.entityData.get(RAMMING);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return SwimmingFish.navigation(this, level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new BreedGoal(this, 1.0));
		this.goalSelector.addGoal(2, new TemptGoal(this, 1.0, this::isFood, false));
		this.goalSelector.addGoal(4, new MeleeAttackGoal(this, RAM_SPEED, false) {
			@Override
			protected boolean canPerformAttack(LivingEntity target) {
				return this.isTimeToAttack()
					&& this.mob.distanceToSqr(target) < this.mob.getBbWidth() * this.mob.getBbWidth() + target.getBbWidth()
					&& this.mob.getSensing().hasLineOfSight(target);
			}

			@Override
			public boolean canUse() {
				return isRamming() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return isRamming() && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(5, new RandomSwimmingGoal(this, 8.0, 40));
		this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Blocks.GLOW_LICHEN.asItem());
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!(this.level() instanceof ServerLevel level)) return;
		if (this.isBaby()) {
			hatchIntoFry(level);
			return;
		}
		this.air.tick(level);
		updateRamming(level);
		if (--this.ramCooldown < 0) {
			this.ramCooldown = Mth.nextInt(this.getRandom(), RAM_DELAY_MIN, RAM_DELAY_MAX - 1);
			pickRamTarget();
		}
		if (this.oilRegrowTicks > 0 && --this.oilRegrowTicks == 0) {
			setShellState(ShellState.NO_SHELL);
		}
	}

	/** Bred young come out as fry rather than small ramchus. */
	private void hatchIntoFry(ServerLevel level) {
		this.discard();
		RamchuFryEntity fry = RamchuModule.RAMCHU_FRY.spawn(level, this.blockPosition(), EntitySpawnReason.MOB_SUMMONED);
		if (fry != null) {
			float yaw = this.getRandom().nextFloat();
			fry.setYRot(yaw);
			fry.setYBodyRot(yaw);
			fry.setYHeadRot(yaw);
			fry.setDeltaMovement(Vec3.ZERO);
		}
	}

	/** A ramchu with a target charges while in water, sped up by dolphin's grace; out of water it gives up. */
	private void updateRamming(ServerLevel level) {
		boolean ramming = this.getTarget() != null && this.isInWater();
		this.entityData.set(RAMMING, ramming);
		if (!ramming) return;
		this.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 10, 2, false, false));
		if (this.shellState() == ShellState.SHELLED && touchesWall(level)) {
			bustShell(level);
		}
	}

	private boolean touchesWall(ServerLevel level) {
		double x = this.getX(), y = this.getY(), z = this.getZ();
		return level.getBlockState(BlockPos.containing(x + 0.5, y, z)).canOcclude()
			|| level.getBlockState(BlockPos.containing(x - 0.5, y, z)).canOcclude()
			|| level.getBlockState(BlockPos.containing(x, y, z + 0.5)).canOcclude()
			|| level.getBlockState(BlockPos.containing(x, y, z - 0.5)).canOcclude();
	}

	/** Cracks the stone shell off in a burst of deepslate, leaving the ramchu stunned. */
	private void bustShell(ServerLevel level) {
		setShellState(ShellState.NO_SHELL);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState()), true, false,
			this.getX(), this.getY() + 0.3, this.getZ(), 12, 0.2, 0.2, 0.2, 1.0);
		Sounds.playAt(this, RamchuModule.BUST_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
		BuiltInRegistries.MOB_EFFECT.get(CorpseGearIds.Effects.STUNNED).ifPresent(stunned ->
			this.addEffect(new MobEffectInstance(stunned, STUN_TICKS, 0, false, false)));
	}

	/** Only a shelled ramchu without a target picks the nearest visible swimming player in survival. */
	private void pickRamTarget() {
		if (this.getTarget() != null || this.shellState() != ShellState.SHELLED || !this.isInWater()) return;
		Vec3 centre = this.position();
		this.level().getEntitiesOfClass(Player.class, new AABB(centre, centre).inflate(RAM_SIGHT_RANGE)).stream()
			.filter(player -> !player.isCreative() && !player.isSpectator())
			.filter(player -> player.isInWater() && this.hasLineOfSight(player))
			.min(Comparator.comparingDouble(player -> player.distanceToSqr(centre)))
			.ifPresent(this::setTarget);
	}

	/** After landing a ram it bounces back off its target. */
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		Vec3 look = this.getLookAngle();
		this.setDeltaMovement(-RECOIL * look.x, 0.0, -RECOIL * look.z);
		return hit;
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		super.mobInteract(player, hand);
		if (this.level().isClientSide()) return InteractionResult.SUCCESS;
		ItemStack held = player.getMainHandItem();
		if (this.shellState() == ShellState.NO_SHELL && held.is(Items.GLASS_BOTTLE)) {
			milkOil(player, held);
		}
		if (player.getMainHandItem().is(Items.WATER_BUCKET)) {
			scoopUp(player);
		}
		return InteractionResult.SUCCESS;
	}

	/** Bottles the oil off a shell-less ramchu's forehead. */
	private void milkOil(Player player, ItemStack bottle) {
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		bottle.shrink(1);
		ItemStack oil = new ItemStack(RamchuModule.oilBottle);
		player.getInventory().placeItemBackInInventory(oil, Prediction.SERVER_ONLY);
		setShellState(ShellState.NO_OIL);
		this.oilRegrowTicks = Mth.nextInt(this.getRandom(), OIL_REGROW_MIN, OIL_REGROW_MAX - 1);
		Sounds.playAt(this, RamchuModule.OIL_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
	}

	/** Any water bucket in the main hand scoops the ramchu up, into the bucket matching its shell. */
	private void scoopUp(Player player) {
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		ItemStack bucket = this.getBucketItemStack();
		this.saveToBucketTag(bucket);
		player.setItemInHand(InteractionHand.MAIN_HAND, bucket);
		Sounds.playAt(this, SoundEvents.BUCKET_FILL_FISH, SoundSource.BLOCKS, 1.0F, 1.0F);
		this.discard();
	}

	@Override
	public boolean fromBucket() {
		return this.entityData.get(FROM_BUCKET);
	}

	@Override
	public void setFromBucket(boolean fromBucket) {
		this.entityData.set(FROM_BUCKET, fromBucket);
	}

	@Override
	public void saveToBucketTag(ItemStack bucket) {
		Bucketable.saveDefaultDataToBucketTag(this, bucket);
		CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket, tag -> {
			tag.putInt(SHELL_STATE_KEY, this.shellState().ordinal());
			tag.putInt("OilRegrowTicks", this.oilRegrowTicks);
		});
	}

	@Override
	@SuppressWarnings("deprecation")
	public void loadFromBucketTag(CompoundTag tag) {
		Bucketable.loadDefaultDataFromBucketTag(this, tag);
		setShellState(ShellState.byId(tag.getIntOr(SHELL_STATE_KEY, ShellState.SHELLED.ordinal())));
		this.oilRegrowTicks = tag.getIntOr("OilRegrowTicks", 0);
	}

	@Override
	public ItemStack getBucketItemStack() {
		return new ItemStack(RamchuModule.bucketFor(this.shellState()));
	}

	@Override
	public SoundEvent getPickupSound() {
		return SoundEvents.BUCKET_FILL_FISH;
	}

	/** Released from a bucket it stays, like the original's PersistenceRequired summon. */
	@Override
	public boolean requiresCustomPersistence() {
		return super.requiresCustomPersistence() || this.fromBucket();
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return RamchuModule.RAMCHU.create(level, EntitySpawnReason.BREEDING);
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
		output.putInt(SHELL_STATE_KEY, this.shellState().ordinal());
		output.putBoolean("FromBucket", this.fromBucket());
		output.putInt("RamCooldown", this.ramCooldown);
		output.putInt("OilRegrowTicks", this.oilRegrowTicks);
		this.air.save(output);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setShellState(ShellState.byId(input.getIntOr(SHELL_STATE_KEY, ShellState.SHELLED.ordinal())));
		setFromBucket(input.getBooleanOr("FromBucket", false));
		this.ramCooldown = input.getIntOr("RamCooldown", FIRST_RAM_DELAY);
		this.oilRegrowTicks = input.getIntOr("OilRegrowTicks", 0);
		this.air.load(input);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return RamchuModule.IDLE_SOUND;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return RamchuModule.HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return RamchuModule.HURT_SOUND;
	}

	@Override
	public String textureName() {
		return this.shellState().texture;
	}

	private PlayState animate(AnimationTest<RamchuEntity> test) {
		if (isRamming()) return test.setAndContinue(RAM);
		return test.setAndContinue(test.isMoving() ? SWIM : IDLE);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>("movement", 2, this::animate));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}

	/** Shelled, shell busted off, or shell-less and milked of its oil. Ids are the original's values. */
	public enum ShellState {
		SHELLED("ramchu"),
		NO_SHELL("ramchu_noshell"),
		NO_OIL("ramchu_noslime");

		final String texture;

		ShellState(String texture) {
			this.texture = texture;
		}

		static ShellState byId(int id) {
			ShellState[] states = values();
			return id >= 0 && id < states.length ? states[id] : SHELLED;
		}
	}
}
