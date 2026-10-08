package com.morecritters.fabric.module.balloon_rat;

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
import com.morecritters.fabric.core.Drops;
import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.TextureVariants;
import com.morecritters.fabric.ids.BalloonRatIds;
import com.morecritters.fabric.ids.SnowflakeSpiderIds;
import com.morecritters.fabric.ids.StincarpIds;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The balloon rat, a badlands pet tamed with spider eyes. A wild one puffs up into a balloon and
 * drifts whenever a non-creative player comes near, and bursts a poison cloud when hit while inflated.
 * Taming makes it a medic that regularly heals its owner and the owner's pets; sneak-right-click
 * swaps it between medic and soldier. A soldier fights for its owner and, fed a poisoned cupcake,
 * spreads that poison (else wither) around its target. Plain cupcakes heal it; owner right-click sits.
 */
public class BalloonRatEntity extends TamableAnimal implements GeoEntity, TextureVariants {
	private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(BalloonRatEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(BalloonRatEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> PUFFED = SynchedEntityData.defineId(BalloonRatEntity.class, EntityDataSerializers.BOOLEAN);
	private static final int WILD = 0, MEDIC = 1, SOLDIER = 2;
	private static final int HEAL_INTERVAL = 1200, ATTACK_INTERVAL = 100, INFLATE_TICKS = 17, DEFLATE_TICKS = 16;
	private static final int PUFF_START = 10, PUFF_END = 20, POISON_TICKS = 100, POISON_AMPLIFIER = 2, CUPCAKE_POISON_TICKS = 3600;
	private static final float CUPCAKE_HEAL = 3.0F;
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle"), WALK = RawAnimation.begin().thenLoop("walk");
	private static final RawAnimation SIT = RawAnimation.begin().thenLoop("sit");
	/** The five poisons in the order a soldier checks them; each poisoned cupcake gives one and clears the rest. */
	private static final List<Identifier> POISONS = List.of(BalloonRatIds.Effects.STAGNATION, SnowflakeSpiderIds.Effects.BRITTLENESS,
			StincarpIds.Effects.ASPHYXIATION, BalloonRatIds.Effects.HALLUCINAZIUM, BalloonRatIds.Effects.MUSCLE_ACHE);
	private static final Map<Identifier, Identifier> CUPCAKE_POISONS = Map.of(
			BalloonRatIds.Items.CUPCAKE_STAGNATION, BalloonRatIds.Effects.STAGNATION,
			BalloonRatIds.Items.CUPCAKE_MUSCLE_ACHE, BalloonRatIds.Effects.MUSCLE_ACHE,
			SnowflakeSpiderIds.Items.CUPCAKE_BRITTLENESS, SnowflakeSpiderIds.Effects.BRITTLENESS,
			BalloonRatIds.Items.CUPCAKE_HALLUCINAZIUM, BalloonRatIds.Effects.HALLUCINAZIUM,
			StincarpIds.Items.CUPCAKE_ASPHYXIATION, StincarpIds.Effects.ASPHYXIATION);
	private static final List<Identifier> BLADDERS = List.of(BalloonRatIds.Items.TOXIN_BLADDER_STAGNATION, BalloonRatIds.Items.TOXIN_BLADDER_MUSCLE_ACHE,
			SnowflakeSpiderIds.Items.TOXIN_BLADDER_BRITTLENESS, BalloonRatIds.Items.TOXIN_BLADDER_HALLUCINAZIUM, StincarpIds.Items.TOXIN_BLADDER_ASPHYXIATION);

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private int healTimer = HEAL_INTERVAL, attackTimer = ATTACK_INTERVAL;
	private boolean changingShape;

	public BalloonRatEntity(EntityType<? extends BalloonRatEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes().add(Attributes.MOVEMENT_SPEED, 0.4).add(Attributes.MAX_HEALTH, 25.0)
				.add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.STEP_HEIGHT, 0.6).add(Attributes.FOLLOW_RANGE, 64.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SITTING, false).define(VARIANT, WILD).define(PUFFED, false);
	}

	private boolean sitting() { return this.entityData.get(SITTING); }
	private int variant() { return this.entityData.get(VARIANT); }
	private boolean puffed() { return this.entityData.get(PUFFED); }
	private void setPuffed(boolean puffed) { this.entityData.set(PUFFED, puffed); }
	private boolean soldierReady() { return !sitting() && variant() == SOLDIER; }

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false) {
			@Override public boolean canUse() { return super.canUse() && soldierReady(); }
			@Override public boolean canContinueToUse() { return super.canContinueToUse() && soldierReady(); }
		});
		this.goalSelector.addGoal(2, new BreedGoal(this, 1.0) {
			@Override public boolean canUse() { return super.canUse() && !sitting(); }
		});
		this.targetSelector.addGoal(3, new OwnerHurtTargetGoal(this) {
			@Override public boolean canUse() { return super.canUse() && soldierReady(); }
		});
		// The original put this target goal in the goal selector; it still picks the owner's attacker.
		this.goalSelector.addGoal(4, new OwnerHurtByTargetGoal(this) {
			@Override public boolean canUse() { return super.canUse() && soldierReady(); }
		});
		this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0, 5.0F, 2.0F) {
			@Override public boolean canUse() { return super.canUse() && !sitting() && getTarget() == null; }
			@Override public boolean canContinueToUse() { return super.canContinueToUse() && !sitting() && getTarget() == null; }
		});
		this.goalSelector.addGoal(6, new PanicGoal(this, 1.6) {
			@Override public boolean canUse() { return super.canUse() && !sitting(); }
		});
		this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0) {
			@Override public boolean canUse() { return super.canUse() && !sitting(); }
			@Override public boolean canContinueToUse() { return super.canContinueToUse() && !sitting(); }
		});
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(9, new FloatGoal(this));
	}

	// --- Ticking ---

	@Override
	public void baseTick() {
		super.baseTick();
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}
		this.removeEffect(MobEffects.POISON);
		if (variant() == WILD) {
			tickWildBalloon();
		}
		if (--this.healTimer <= -1) {
			this.healTimer = HEAL_INTERVAL;
			if (variant() == MEDIC && !sitting()) {
				medicPulse();
			}
		}
		if (--this.attackTimer <= -1) {
			if (soldierReady() && getTarget() != null && distanceTo(getTarget()) <= 5.0F && onGround()) {
				soldierPulse();
			}
			this.attackTimer = ATTACK_INTERVAL;
		}
	}

	/** Inflates and drifts while a non-creative player is near, deflates otherwise; turns medic once tamed. */
	private void tickWildBalloon() {
		Player player = nearestPlayer(12.0);
		boolean inflate = !this.level().getEntitiesOfClass(Player.class, AABB.ofSize(position(), 8, 8, 8)).isEmpty()
				&& player != null && !player.hasInfiniteMaterials();
		if (inflate) {
			this.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 1, false, false));
			Vec3 look = getLookAngle();
			setDeltaMovement(look.x * -0.05, -0.2, look.z * -0.05);
			if (!puffed()) {
				this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10, 30, false, false));
				setDeltaMovement(0.0, 0.01, 0.0);
				lookAt(EntityAnchorArgument.Anchor.EYES, player.position().add(0.0, 1.0, 0.0));
				changeShape(BalloonRatModule.INFLATE_SOUND, "inflate", INFLATE_TICKS, true, 0);
			}
		} else if (puffed()) {
			changeShape(BalloonRatModule.DEFLATE_SOUND, "deflate", DEFLATE_TICKS, false, 20);
		}
		if (isTame()) {
			Particles.spawnAt(this, BalloonRatModule.MEDIC_STRIPE, 6, 0.5, 0.0, 0.5, 0.0);
			this.entityData.set(VARIANT, MEDIC);
			setPuffed(false);
		}
	}

	private void changeShape(SoundEvent sound, String animation, int ticks, boolean puffed, int slowTicks) {
		if (slowTicks > 0) {
			this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, slowTicks, 30, false, false));
		}
		if (this.changingShape) {
			return;
		}
		this.changingShape = true;
		Sounds.playAt(this, sound, SoundSource.NEUTRAL, 1.0F, 1.0F);
		triggerAnim(Animations.ACTIONS, animation);
		ServerScheduler.runLater(ticks, () -> { setPuffed(puffed); this.changingShape = false; });
	}

	/** Puffs once and, a second later, gives regeneration to itself, its owner and the owner's pets within 5 blocks. */
	private void medicPulse() {
		puffBriefly();
		LivingEntity owner = getOwner();
		for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(5.0))) {
			if (target == this || (owner != null && (target == owner || target instanceof TamableAnimal pet && pet.isOwnedBy(owner)))) {
				ServerScheduler.runLater(PUFF_END, () -> target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 2, false, true)));
			}
		}
	}

	/** Leaps at its target and, a second later, poisons everything within 5 blocks except its own side. */
	private void soldierPulse() {
		Vec3 look = getLookAngle();
		setDeltaMovement(look.x * 0.6, 0.5, look.z * 0.6);
		puffBriefly();
		LivingEntity owner = getOwner();
		for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(5.0))) {
			boolean friend = target == this || target == owner || (owner != null && target instanceof TamableAnimal pet && pet.isOwnedBy(owner));
			if (!friend) {
				ServerScheduler.runLater(PUFF_END, () -> target.addEffect(new MobEffectInstance(carriedPoison(), POISON_TICKS, POISON_AMPLIFIER, false, true)));
			}
		}
	}

	/** The first poison the rat itself carries, else wither. */
	private Holder<MobEffect> carriedPoison() {
		for (Identifier id : POISONS) {
			Holder<MobEffect> poison = effect(id);
			if (poison != null && hasEffect(poison)) {
				return poison;
			}
		}
		return MobEffects.WITHER;
	}

	private void puffBriefly() {
		triggerAnim(Animations.ACTIONS, "inflate");
		ServerScheduler.runLater(PUFF_START, () -> setPuffed(true));
		ServerScheduler.runLater(PUFF_END, () -> setPuffed(false));
	}

	private @Nullable Player nearestPlayer(double size) {
		return this.level().getEntitiesOfClass(Player.class, AABB.ofSize(position(), size, size, size)).stream()
				.min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
	}

	private static @Nullable Holder<MobEffect> effect(Identifier id) {
		return BuiltInRegistries.MOB_EFFECT.get(id).<Holder<MobEffect>>map(h -> h).orElse(null);
	}

	// --- Interaction ---

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.SPIDER_EYE);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.getItem() instanceof SpawnEggItem) {
			return super.mobInteract(player, hand);
		}
		if (this.level().isClientSide()) {
			return (isTame() && isOwnedBy(player)) || isFood(stack) ? InteractionResult.SUCCESS : InteractionResult.PASS;
		}
		if (hand == InteractionHand.MAIN_HAND) {
			feedCupcake(player, player.getMainHandItem());
		}
		InteractionResult result = interactOnServer(player, hand, stack);
		ownerClick(player);
		return result;
	}

	/** Cupcakes from the owner: a plain one heals 3, a poisoned one (soldiers only) swaps the rat's poison. */
	private void feedCupcake(Player player, ItemStack stack) {
		if (!isTame() || !isOwnedBy(player)) {
			return;
		}
		Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		Identifier poison = CUPCAKE_POISONS.get(itemId);
		if (poison != null && variant() == SOLDIER) {
			Holder<MobEffect> given = effect(poison);
			if (given != null) {
				addEffect(new MobEffectInstance(given, CUPCAKE_POISON_TICKS, 0, false, true));
			}
			POISONS.stream().filter(id -> !id.equals(poison)).map(BalloonRatEntity::effect).filter(h -> h != null).forEach(this::removeEffect);
		} else if (itemId.equals(BalloonRatIds.Items.CUPCAKE) && getHealth() != getMaxHealth()) {
			setHealth(getHealth() + CUPCAKE_HEAL);
		} else {
			return;
		}
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		Sounds.playAt(this, SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
		((ServerLevel) this.level()).sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack.getItem()),
				getX() + 0.5, getY() + 0.5, getZ() + 0.5, 5, 0.1, 0.1, 0.1, 0.1);
		if (!player.hasInfiniteMaterials()) {
			stack.shrink(1);
		}
	}

	private InteractionResult interactOnServer(Player player, InteractionHand hand, ItemStack stack) {
		if (isTame()) {
			if (!isOwnedBy(player)) {
				return InteractionResult.SUCCESS;
			}
			if (isFood(stack) && getHealth() < getMaxHealth()) {
				usePlayerItem(player, hand, stack);
				heal(4.0F);
				return InteractionResult.SUCCESS;
			}
			return super.mobInteract(player, hand);
		}
		if (isFood(stack)) {
			usePlayerItem(player, hand, stack);
			if (this.random.nextInt(3) == 0) {
				tame(player);
				this.level().broadcastEntityEvent(this, (byte) 7);
			} else {
				this.level().broadcastEntityEvent(this, (byte) 6);
			}
			setPersistenceRequired();
			return InteractionResult.SUCCESS;
		}
		InteractionResult result = super.mobInteract(player, hand);
		if (result.consumesAction()) {
			setPersistenceRequired();
		}
		return result;
	}

	/** The owner sneaking swaps medic and soldier; otherwise any owner click toggles sitting (even feeding). */
	private void ownerClick(Player player) {
		if (!isTame() || !isOwnedBy(player)) {
			return;
		}
		if (player.isShiftKeyDown()) {
			if (variant() == MEDIC || variant() == SOLDIER) {
				boolean toSoldier = variant() == MEDIC;
				this.entityData.set(VARIANT, toSoldier ? SOLDIER : MEDIC);
				Particles.spawnAt(this, toSoldier ? BalloonRatModule.SOLDIER_STRIPE : BalloonRatModule.MEDIC_STRIPE, 6, 0.5, 0.0, 0.5, 0.0);
				Sounds.playAt(this, BalloonRatModule.TRANSFORM_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
			}
		} else if (sitting()) {
			this.entityData.set(SITTING, false);
			triggerAnim(Animations.ACTIONS, "sit_rise");
		} else {
			this.entityData.set(SITTING, true);
		}
	}

	@Override
	public void tame(Player player) {
		super.tame(player);
		Advancements.award(player, MoreCritters.id("tame_balloon_rat"));
	}

	// --- Damage and death ---

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() != null) {
			if (onGround()) {
				setDeltaMovement(0.0, 0.3, 0.0);
			}
			if (source.getEntity() instanceof Player player) {
				Advancements.award(player, MoreCritters.id("encounter_balloon_rat"));
			}
			if (variant() == WILD && puffed()) {
				burst(level);
			}
		}
		return super.hurtServer(level, source, amount);
	}

	/** Popped while inflated: a green cloud that (if configured) poisons everything within 2 blocks. */
	private void burst(ServerLevel level) {
		Sounds.playAt(this, BalloonRatModule.DEFLATE_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
		triggerAnim(Animations.ACTIONS, "deflate");
		DustParticleOptions dust = new DustParticleOptions(0x4DB31A, 2.0F);
		for (int i = 0, n = Mth.nextInt(this.random, 10, 14); i < n; i++) {
			level.sendParticles(dust, true, true, getX(), getY(), getZ(), 5, 1.0, 0.0, 1.0, 0.05);
		}
		if (Config.flag("balloon_rat_poisons_target", true)) {
			for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(2.0))) {
				target.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS, POISON_AMPLIFIER, true, true));
			}
		}
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		Identifier bladder = BLADDERS.get(this.random.nextInt(BLADDERS.size()));
		Item item = BuiltInRegistries.ITEM.getValue(bladder);
		if (item != Items.AIR) {
			Drops.dropSingles(this, item, 1);
		}
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return BalloonRatEntities.BALLOON_RAT.create(level, EntitySpawnReason.BREEDING);
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).scale(isBaby() ? 0.7F : 1.2F);
	}

	@Override protected SoundEvent getAmbientSound() { return BalloonRatModule.IDLE_SOUND; }
	@Override protected SoundEvent getHurtSound(DamageSource source) { return BalloonRatModule.HURT_SOUND; }
	@Override protected SoundEvent getDeathSound() { return BalloonRatModule.DEATH_SOUND; }

	@Override
	public String textureName() {
		String kind = switch (variant()) { case MEDIC -> "balloon_rat_medic"; case SOLDIER -> "balloon_rat_soldier"; default -> "balloon_rat"; };
		return puffed() ? kind + "_inflated" : kind;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("sit", sitting());
		output.putInt("variant", variant());
		output.putBoolean("inflated", puffed());
		output.putInt("heal", this.healTimer);
		output.putInt("attack", this.attackTimer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.entityData.set(SITTING, input.getBooleanOr("sit", false));
		this.entityData.set(VARIANT, input.getIntOr("variant", WILD));
		setPuffed(input.getBooleanOr("inflated", false));
		this.healTimer = input.getIntOr("heal", HEAL_INTERVAL);
		this.attackTimer = input.getIntOr("attack", ATTACK_INTERVAL);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<BalloonRatEntity>(Animations.MOVEMENT, 4,
				test -> test.setAndContinue(sitting() ? SIT : test.isMoving() ? WALK : IDLE)));
		controllers.add(Animations.actions(this, "inflate", "deflate", "sit_rise"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}
}
