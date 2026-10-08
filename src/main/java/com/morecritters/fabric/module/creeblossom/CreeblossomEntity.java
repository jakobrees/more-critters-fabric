package com.morecritters.fabric.module.creeblossom;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Drops;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.TextureVariants;
import com.morecritters.fabric.ids.ShockCubeIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A walking flower that lives twenty seconds and then blows up. Wild ones (from blossoming monsters) hunt players;
 * friendly and electric ones (grown from a blossombush) hunt monsters. Three hits and it primes early; an electric
 * one fizzles and leaves a small shock cube behind. Hitting a creeper deals ten extra damage.
 */
public class CreeblossomEntity extends Animal implements GeoEntity, TextureVariants {
	/** Where it came from decides its look and its side. */
	public enum Kind {
		WILD("creeblossom"), FRIENDLY("creeblossom_friendly"), ELECTRIC("creeblossom_electric");

		final String texture;

		Kind(String texture) {
			this.texture = texture;
		}
	}

	private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(CreeblossomEntity.class, EntityDataSerializers.INT);
	private static final int LIFESPAN = 400;
	private static final int HITS_BEFORE_FUSE = 3;
	private static final int FUSE_TICKS = 20;
	private static final float EXPLOSION_POWER = 1.5F;
	private static final float CREEPER_BONUS_DAMAGE = 10.0F;
	private static final int ZAP_CHANCE = 30;
	private static final DustParticleOptions SMOKE = new DustParticleOptions(0x1A1A1A, 1.5F);

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private int lifeLeft = LIFESPAN;
	private int hitsLeft = HITS_BEFORE_FUSE;
	private int fuseTicks;

	public CreeblossomEntity(EntityType<? extends CreeblossomEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 5.0)
			.add(Attributes.ATTACK_DAMAGE, 5.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(KIND, Kind.WILD.ordinal());
	}

	public Kind kind() {
		return Kind.values()[this.entityData.get(KIND)];
	}

	private void setKind(Kind kind) {
		this.entityData.set(KIND, kind.ordinal());
	}

	private boolean isRecruited() {
		return kind() != Kind.WILD;
	}

	private boolean isElectric() {
		return kind() == Kind.ELECTRIC;
	}

	@Override
	public String textureName() {
		return kind().texture;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false, (target, level) -> !isRecruited()));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, false, (target, level) -> isRecruited()));
		this.goalSelector.addGoal(4, new RandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(6, new FloatGoal(this));
	}

	/** Grown inside a blossombush it is recruited: friendly, or electric from an electric bush. */
	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
		BlockState home = level.getBlockState(this.blockPosition());
		if (home.getBlock() instanceof BlossombushBlock bush) {
			setKind(bush.isElectric() ? Kind.ELECTRIC : Kind.FRIENDLY);
		} else {
			setKind(Kind.WILD);
		}
		return super.finalizeSpawn(level, difficulty, reason, data);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level() instanceof ServerLevel level) {
			tickLife(level);
			tickFuse(level);
			if (this.isBaby()) {
				growUpInstantly(level);
			}
			if (isElectric() && this.random.nextInt(ZAP_CHANCE) == 0) {
				spawnZaps(level, 2);
			}
		}
	}

	/** Twenty seconds after spawning it primes no matter what. */
	private void tickLife(ServerLevel level) {
		if (--lifeLeft > 0) {
			return;
		}
		lifeLeft = LIFESPAN;
		startFusing();
		ServerScheduler.runLater(FUSE_TICKS, () -> {
			Sounds.playAt(this, primedSound(), SoundSource.HOSTILE, 1.0F, 1.0F);
			if (isElectric()) {
				Sounds.playAt(this, CreeblossomModule.ELECTRIC_EXPLODE_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
				level.explode(null, getX(), getY(), getZ(), 0.0F, Level.ExplosionInteraction.NONE);
			} else {
				burst(level);
			}
			this.discard();
		});
	}

	/** After three landed hits it primes early. */
	private void tickFuse(ServerLevel level) {
		if (hitsLeft <= 0) {
			hitsLeft = 1000;
			Sounds.playAt(this, primedSound(), SoundSource.HOSTILE, 1.0F, 1.0F);
			startFusing();
			if (isElectric()) {
				ServerScheduler.runLater(FUSE_TICKS, () -> fizzle(level));
			} else {
				Sounds.playAt(this, primedSound(), SoundSource.HOSTILE, 1.0F, 1.0F);
				ServerScheduler.runLater(FUSE_TICKS, () -> {
					burst(level);
					this.discard();
				});
			}
		}
		if (fuseTicks > 0) {
			fuseTicks--;
			int puffs = Drops.randomCount(this, 10, 15);
			if (isElectric()) {
				spawnZaps(level, puffs);
			} else {
				level.sendParticles(SMOKE, true, true, getX(), getY() + 0.5, getZ(), puffs, 0.2, 0.4, 0.2, 1.0);
			}
		}
	}

	private void startFusing() {
		fuseTicks = FUSE_TICKS;
		this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, FUSE_TICKS, 30, false, false));
		this.triggerAnim(Animations.ACTIONS, "blow");
	}

	/** A flower-shaped blast that hurts but leaves the ground alone. */
	private void burst(ServerLevel level) {
		level.explode(null, getX(), getY(), getZ(), EXPLOSION_POWER, Level.ExplosionInteraction.NONE);
		level.sendParticles(CreeblossomModule.BLOSSOM_EXPLOSION, getX(), getY(), getZ(), 1, 0.0, 0.0, 0.0, 0.0);
	}

	/** An electric creeblossom pops harmlessly and leaves a small shock cube. */
	private void fizzle(ServerLevel level) {
		level.explode(null, getX(), getY(), getZ(), 0.0F, Level.ExplosionInteraction.NONE);
		spawnZaps(level, 5);
		Sounds.playAt(this, CreeblossomModule.ELECTRIC_EXPLODE_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
		if (BuiltInRegistries.ENTITY_TYPE.containsKey(ShockCubeIds.Entities.SHOCK_CUBE_SMALL)) {
			BuiltInRegistries.ENTITY_TYPE.getValue(ShockCubeIds.Entities.SHOCK_CUBE_SMALL).spawn(level, BlockPos.containing(getX(), getY(), getZ()), EntitySpawnReason.MOB_SUMMONED);
		}
		this.discard();
	}

	private void spawnZaps(ServerLevel level, int count) {
		if (BuiltInRegistries.PARTICLE_TYPE.getValue(ShockCubeIds.Particles.ZAP) instanceof ParticleOptions zap) {
			level.sendParticles(zap, getX(), getY(), getZ(), count, 0.5, 0.5, 0.5, 0.0);
		}
	}

	/** A baby (from a spawn egg used on a creeblossom) is swapped for a grown one. */
	private void growUpInstantly(ServerLevel level) {
		CreeblossomEntity adult = CreeblossomModule.CREEBLOSSOM.spawn(level, this.blockPosition(), EntitySpawnReason.MOB_SUMMONED);
		if (adult != null) {
			adult.snapTo(getX(), getY(), getZ(), getYRot(), getXRot());
			adult.setYHeadRot(getYRot());
			adult.setYBodyRot(getYRot());
		}
		this.discard();
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		hitsLeft--;
		Sounds.playAt(this, isElectric() ? CreeblossomModule.ELECTRIC_ATTACK_SOUND : CreeblossomModule.ATTACK_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
		this.triggerAnim(Animations.ACTIONS, "attack");
		if (target instanceof Creeper) {
			target.hurtServer(level, level.damageSources().generic(), CREEPER_BONUS_DAMAGE);
		}
		return super.doHurtTarget(level, target);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isElectric()) {
			Sounds.playAt(this, CreeblossomModule.ELECTRIC_HURT_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
		}
		return super.hurtServer(level, source, amount);
	}

	/** Wild ones drop blossombush seeds and gunpowder. */
	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!(this.level() instanceof ServerLevel)) {
			return;
		}
		if (!isRecruited()) {
			Drops.dropSingles(this, CreeblossomModule.blossombushSeed, Drops.randomCount(this, 0, 2));
			Drops.dropSingles(this, Items.GUNPOWDER, Drops.randomCount(this, 0, 4));
		}
		if (isElectric()) {
			Sounds.playAt(this, CreeblossomModule.ELECTRIC_DEATH_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
		}
	}

	private SoundEvent primedSound() {
		return isElectric() ? CreeblossomModule.ELECTRIC_PRIMED_SOUND : CreeblossomModule.PRIMED_SOUND;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return CreeblossomModule.HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return CreeblossomModule.DEATH_SOUND;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return false;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return CreeblossomModule.CREEBLOSSOM.create(level, EntitySpawnReason.BREEDING);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Kind", kind().ordinal());
		output.putInt("LifeLeft", lifeLeft);
		output.putInt("HitsLeft", hitsLeft);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setKind(Kind.values()[Math.floorMod(input.getIntOr("Kind", 0), Kind.values().length)]);
		lifeLeft = input.getIntOr("LifeLeft", LIFESPAN);
		hitsLeft = input.getIntOr("HitsLeft", HITS_BEFORE_FUSE);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(Animations.idleWalk(this, "idle", "walk"));
		controllers.add(Animations.actions(this, "attack", "blow"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
