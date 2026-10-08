package com.morecritters.fabric.module.blubberfish;

import com.morecritters.fabric.core.DryingOut;
import com.morecritters.fabric.core.SwimmingFish;
import java.util.Comparator;
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
import com.morecritters.fabric.core.Drops;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.TextureVariants;
import com.morecritters.fabric.ids.KelpireIds;
import com.morecritters.fabric.ids.MiscIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The blubberfish: a round ocean fish that breeds on gravel and flops around helplessly on land.
 * Out of water it slowly dries out. Feed it sprinkles while it is beached and it swells up and
 * bursts into a shower of sprinkles. A water bucket scoops it up; killed, it drops raw blubberfish
 * and sometimes blubber. Its young hatch as {@link BlubberfishFryEntity}.
 */
public class BlubberfishEntity extends Animal implements GeoEntity, TextureVariants {
	private static final EntityDataAccessor<Boolean> EXPLODING = SynchedEntityData.defineId(BlubberfishEntity.class, EntityDataSerializers.BOOLEAN);

	/** How long it takes from eating sprinkles to bursting. */
	private static final int FUSE_TICKS = 25;
	/** The tick of the fuse when the sprinkles fly; the fish vanishes one tick later. */
	private static final int BURST_TICK = 2;
	private static final double SWIM_SPEED = 2.0;
	private static final double EXPLOSION_WITNESS_RANGE = 10.0;

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("swim");
	private static final RawAnimation FALL = RawAnimation.begin().thenPlayAndHold("fall");
	private static final RawAnimation BEACHED = RawAnimation.begin().thenLoop("land");
	private static final RawAnimation SPLASH_DOWN = RawAnimation.begin().thenPlay("landing").thenLoop("idle");
	private static final RawAnimation EXPLODE = RawAnimation.begin().thenPlayAndHold("explode");
	/** Length of the "landing" animation, after which the fish swims normally again. */
	private static final int SPLASH_DOWN_TICKS = 5;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private final DryingOut air = new DryingOut(this);
	private int fuseTicks;
	// Client-side animation state: set when the fish falls back into water from the air.
	private boolean falling;
	private int splashDownTicks;

	public BlubberfishEntity(EntityType<? extends BlubberfishEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.moveControl = SwimmingFish.setUp(this);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
			.add(Attributes.MOVEMENT_SPEED, 2.0)
			.add(Attributes.MAX_HEALTH, 5.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 16.0)
			.add(Attributes.STEP_HEIGHT, 0.6);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(EXPLODING, false);
	}

	public boolean isExploding() {
		return this.entityData.get(EXPLODING);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return SwimmingFish.navigation(this, level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new BreedGoal(this, 1.0));
		this.goalSelector.addGoal(2, new TemptGoal(this, 1.0, this::isFood, false));
		this.goalSelector.addGoal(3, new RandomSwimmingGoal(this, 1.0, 40));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this) {
			@Override
			public boolean canUse() {
				return super.canUse() && BlubberfishEntity.this.isInWater();
			}

			@Override
			public boolean canContinueToUse() {
				return super.canContinueToUse() && BlubberfishEntity.this.isInWater();
			}
		});
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.GRAVEL);
	}

	@Override
	public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return BlubberfishModule.BLUBBERFISH.create(level, EntitySpawnReason.BREEDING);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level() instanceof ServerLevel level) {
			this.air.tick(level);
			if (this.isBaby()) {
				hatchIntoFry(level);
			}
			tickFuse(level);
		} else {
			updateFallState();
		}
	}

	/** A bred baby blubberfish is immediately replaced by a fry, which grows back into a blubberfish. */
	private void hatchIntoFry(ServerLevel level) {
		this.discard();
		BlubberfishFryEntity fry = BlubberfishModule.BLUBBERFISH_FRY.spawn(level, this.blockPosition(), EntitySpawnReason.MOB_SUMMONED);
		if (fry != null) {
			fry.setYRot(this.getYRot());
			fry.setYBodyRot(this.getYRot());
			fry.setYHeadRot(this.getYRot());
			fry.setDeltaMovement(Vec3.ZERO);
		}
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		super.mobInteract(player, hand);
		if (this.level() instanceof ServerLevel level && !isExploding()) {
			ItemStack held = player.getMainHandItem();
			if (held.is(Items.WATER_BUCKET)) {
				scoopUp(player, BlubberfishModule.blubberfishBucket);
			} else if (held.is(BlubberfishModule.sprinkles) && !this.isInWater()) {
				eatSprinkles(level, player, held);
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Swaps the player's water bucket for a bucket holding this fish. */
	void scoopUp(Player player, Item bucket) {
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(bucket));
		Sounds.playAt(this, SoundEvents.BUCKET_FILL_FISH, SoundSource.BLOCKS, 1.0F, 1.0F);
		this.discard();
	}

	/** A beached blubberfish fed sprinkles swells up and bursts a moment later. */
	private void eatSprinkles(ServerLevel level, Player player, ItemStack sprinkles) {
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		this.entityData.set(EXPLODING, true);
		if (!player.hasInfiniteMaterials()) sprinkles.shrink(1);
		this.fuseTicks = FUSE_TICKS;
		sendForcedParticles(level, new ItemParticleOption(ParticleTypes.ITEM, BlubberfishModule.sprinkles), 0.2, 30);
		Sounds.playAt(this, BlubberfishModule.EXPLODE_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
		Sounds.playAt(this, SoundEvents.STRIDER_EAT, SoundSource.NEUTRAL, 1.0F, 1.0F);
	}

	private void tickFuse(ServerLevel level) {
		if (this.fuseTicks <= 0) return;
		this.fuseTicks--;
		if (this.fuseTicks == BURST_TICK) {
			burstIntoSprinkles(level);
		} else if (this.fuseTicks == 1) {
			this.discard();
			awardNearestPlayer(level);
		}
	}

	private void burstIntoSprinkles(ServerLevel level) {
		sendForcedParticles(level, miscParticle(MiscIds.Particles.SPRINKLE), 0.5, 30);
		sendForcedParticles(level, miscParticle(MiscIds.Particles.SPRINKLE), 1.0, 30);
		sendForcedParticles(level, miscParticle(MiscIds.Particles.REMAINS), 1.0, 15);
		if (this.getRandom().nextInt(10) == 0) {
			sendForcedParticles(level, miscParticle(MiscIds.Particles.WHITE_SPRINKLE), 0.5, 1);
		}
	}

	/** The player closest to the burst, within a 10-block box, earns "explode_blubberfish". */
	private void awardNearestPlayer(ServerLevel level) {
		Vec3 centre = this.position();
		level.getEntitiesOfClass(Player.class, AABB.ofSize(centre, EXPLOSION_WITNESS_RANGE, EXPLOSION_WITNESS_RANGE, EXPLOSION_WITNESS_RANGE))
			.stream()
			.min(Comparator.comparingDouble(player -> player.distanceToSqr(centre)))
			.ifPresent(player -> Advancements.award(player, MoreCritters.id("explode_blubberfish")));
	}

	/** Particles from the misc module, looked up by name; skipped while that module is a stub. */
	private static ParticleOptions miscParticle(Identifier id) {
		return BuiltInRegistries.PARTICLE_TYPE.getValue(id) instanceof SimpleParticleType type ? type : null;
	}

	/** Like the original's {@code /particle ... 0 0 0 0.1 <count> force}: visible from afar. */
	private void sendForcedParticles(ServerLevel level, ParticleOptions particle, double heightOffset, int count) {
		if (particle == null) return;
		level.sendParticles(particle, true, false, this.getX(), this.getY() + heightOffset, this.getZ(), count, 0.0, 0.0, 0.0, 0.1,
			ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() instanceof Player player) {
			Advancements.award(player, MoreCritters.id("encounter_blubberfish"));
		}
		// It lives underwater, so vanilla's drowning must not apply; drying out replaces it.
		return !source.is(DamageTypes.DROWN) && super.hurtServer(level, source, amount);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (this.level() instanceof ServerLevel && wasKilledByNonKelpire(source)) {
			Drops.dropSingles(this, BlubberfishModule.rawBlubberfish, 1);
			if (this.getRandom().nextBoolean()) {
				Drops.dropSingles(this, BlubberfishModule.BLUBBER, Drops.randomCount(this, 1, 4));
			}
		}
	}

	/** Only kills by some entity drop meat, and the kelpire eats its catch whole. */
	private static boolean wasKilledByNonKelpire(DamageSource source) {
		Entity killer = source.getEntity();
		return killer != null && killer.getType() != BuiltInRegistries.ENTITY_TYPE.getValue(KelpireIds.Entities.KELPIRE);
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

	/** Spawn rule: water at the spawn position and the block above it. */
	static boolean canSpawnIn(EntityType<? extends Mob> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return level.getBlockState(pos).is(Blocks.WATER) && level.getBlockState(pos.above()).is(Blocks.WATER);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Exploding", isExploding());
		output.putInt("FuseTicks", this.fuseTicks);
		this.air.save(output);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.entityData.set(EXPLODING, input.getBooleanOr("Exploding", false));
		this.fuseTicks = input.getIntOr("FuseTicks", 0);
		this.air.load(input);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return BlubberfishModule.HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return BlubberfishModule.HURT_SOUND;
	}

	@Override
	public String textureName() {
		if (isExploding()) return "blubberfish_explode";
		return this.isInWater() ? "blubberfish" : "blubberfish_land";
	}

	/** Remembers a fall so the fish plays its "landing" splash when it drops back into water. */
	private void updateFallState() {
		if (this.splashDownTicks > 0) this.splashDownTicks--;
		if (this.isInWater()) {
			if (this.falling) this.splashDownTicks = SPLASH_DOWN_TICKS;
			this.falling = false;
		} else {
			this.falling = !this.onGround();
		}
	}

	/** Swims or idles in water, holds its falling pose in the air, flops when beached, swells when exploding. */
	private PlayState animate(AnimationTest<BlubberfishEntity> test) {
		if (isExploding()) return test.setAndContinue(EXPLODE);
		if (this.falling) return test.setAndContinue(FALL);
		if (!this.isInWater()) return test.setAndContinue(BEACHED);
		if (this.splashDownTicks > 0) return test.setAndContinue(SPLASH_DOWN);
		return test.setAndContinue(test.isMoving() ? SWIM : IDLE);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>("body", 1, this::animate));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
