package com.morecritters.fabric.module.nightshroom;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.TextureVariants;
import com.morecritters.fabric.ids.MightshroomIds;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A geyser of rot that a frightshroom's stomp brings down near its target. It drops out of the
 * sky unseen, lands with a splash that flings three rot pieces into the air, and for five to ten
 * seconds bubbles in place, swelling to twice its size and tossing anything that steps on it
 * (except shrooms) into the air. It shrugs off players, projectiles and the elements, and
 * vanishes if it ends up inside a block.
 */
public class RotSplashEntity extends PathfinderMob implements GeoEntity, TextureVariants {
	private static final int LIFE_MIN = 100, LIFE_MAX = 200, END_TICKS = 22;
	private static final int SPLASH_PIECES = 3;
	private static final float SPLASH_PIECE_DAMAGE = 3.0F;
	private static final int SPLASH_PIECE_KNOCKBACK = 1;
	private static final float GROWTH_PER_TICK = 0.1F, FULL_SIZE = 2.0F;
	private static final double TOSS_REACH = 1.25, TOSS_STRENGTH = 0.5;
	private static final int TEXTURE_FRAMES = 5, TEXTURE_FRAME_TICKS = 2;
	private static final int HIGH_ROT_CHANCE = 3, BUBBLE_SOUND_CHANCE = 5;

	private static final List<ResourceKey<DamageType>> IMMUNE_TO = List.of(
		DamageTypes.IN_FIRE, DamageTypes.FALL, DamageTypes.CACTUS, DamageTypes.DROWN, DamageTypes.LIGHTNING_BOLT,
		DamageTypes.EXPLOSION, DamageTypes.TRIDENT, DamageTypes.FALLING_ANVIL, DamageTypes.DRAGON_BREATH,
		DamageTypes.WITHER, DamageTypes.WITHER_SKULL
	);
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	/** Ticks until it starts to dry up. */
	private int lifeTimer = LIFE_MAX;
	/** Ticks left of its end animation; zero while it is still bubbling. */
	private int endTimer;
	/** Its size, from nothing to twice full; grows on both sides so the client sees it swell. */
	private float growth;

	public RotSplashEntity(EntityType<? extends RotSplashEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 10.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	/** Splashes down: hidden, with a gush of rot and mycelium and three rot pieces thrown up. */
	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, groupData);
		ServerLevel serverLevel = level.getLevel();
		ServerScheduler.runLater(1, () -> this.triggerAnim(Animations.ACTIONS, "start"));
		this.lifeTimer = Mth.nextInt(this.random, LIFE_MIN, LIFE_MAX);
		Bursts.rot(serverLevel, this.position(), 5, 0.2, 0.0, 0.2, 0.01);
		Bursts.mycelium(serverLevel, this.position(), 2, 0.3, 0.0, 0.3, 0.01);
		this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 1, false, false));
		Sounds.playAt(this, NightshroomModule.ROT_SPLASH_START_SOUND, SoundSource.BLOCKS, 2.0F, 1.0F);
		for (int i = 0; i < SPLASH_PIECES; i++) {
			RotPieceEntity piece = RotPieceEntity.create(serverLevel, null, SPLASH_PIECE_DAMAGE, SPLASH_PIECE_KNOCKBACK);
			piece.setPos(this.getX(), this.getY(), this.getZ());
			piece.shoot(Mth.nextDouble(this.random, -0.5, 0.5), 2.0, Mth.nextDouble(this.random, -0.5, 0.5), 1.0F, 0.0F);
			serverLevel.addFreshEntity(piece);
		}
		this.growth = 0.0F;
		return result;
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.growth < FULL_SIZE) {
			this.growth += GROWTH_PER_TICK;
			this.refreshDimensions();
		}
		if (this.level() instanceof ServerLevel level) {
			tossWhatStandsOnIt(level);
			fallOrSettle();
			bubble(level);
			ageAndDryUp();
			if (!this.isRemoved() && this.isInWall()) end();
		}
	}

	private void tossWhatStandsOnIt(ServerLevel level) {
		Vec3 center = this.position();
		for (Entity entity : level.getEntities(this, new AABB(center, center).inflate(TOSS_REACH), RotSplashEntity::isTossed)) {
			entity.push(0.0, TOSS_STRENGTH, 0.0);
		}
	}

	/** Everything but rot splashes and shrooms. */
	private static boolean isTossed(Entity entity) {
		return !(entity instanceof RotSplashEntity || entity instanceof FrightshroomEntity || entity instanceof NightshroomEntity
			|| OtherModules.isType(entity, MightshroomIds.Entities.MIGHTSHROOM));
	}

	/** Plunges straight down, unseen, until it lands; on the ground it shows itself. */
	private void fallOrSettle() {
		this.setDeltaMovement(0.0, -2.0, 0.0);
		if (!this.onGround()) {
			this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 10, 0, false, false));
		} else {
			this.removeAllEffects();
		}
	}

	private void bubble(ServerLevel level) {
		Bursts.rot(level, this.position(), 5, 0.2, 0.0, 0.2, 0.01);
		Bursts.mycelium(level, this.position(), 2, 0.3, 0.0, 0.3, 0.01);
		if (this.random.nextInt(HIGH_ROT_CHANCE) == 0) {
			Bursts.rot(level, this.position().add(0.0, 3.0, 0.0), 5, 0.5, 0.0, 0.5, 0.01);
		}
		if (this.random.nextInt(BUBBLE_SOUND_CHANCE) == 0) {
			Sounds.playAt(this, NightshroomModule.ROT_SPLASH_IDLE_SOUND, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
	}

	private void ageAndDryUp() {
		if (--this.lifeTimer == 1) {
			this.triggerAnim(Animations.ACTIONS, "end");
			this.endTimer = END_TICKS;
		}
		if (this.endTimer > 0 && --this.endTimer == 0) end();
	}

	private void end() {
		Sounds.playAt(this, NightshroomModule.ROT_SPLASH_END_SOUND, SoundSource.BLOCKS, 1.0F, 1.0F);
		this.discard();
	}

	public float growth() {
		return this.growth;
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).scale(this.growth);
	}

	/** Five frames of bubbling rot, two ticks each. */
	@Override
	public String textureName() {
		return "rot_splash" + this.tickCount / TEXTURE_FRAME_TICKS % TEXTURE_FRAMES;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		Entity direct = source.getDirectEntity();
		if (direct instanceof AbstractArrow || direct instanceof Player || direct instanceof AbstractThrownPotion || direct instanceof AreaEffectCloud) return false;
		for (ResourceKey<DamageType> immune : IMMUNE_TO) {
			if (source.is(immune)) return false;
		}
		return super.hurtServer(level, source, damage);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!this.level().isClientSide()) end();
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.GENERIC_HURT; }
	@Override protected SoundEvent getDeathSound() { return SoundEvents.GENERIC_DEATH; }

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("LifeTimer", this.lifeTimer);
		output.putInt("EndTimer", this.endTimer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.lifeTimer = input.getIntOr("LifeTimer", LIFE_MAX);
		this.endTimer = input.getIntOr("EndTimer", 0);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(Animations.MOVEMENT, 0, (AnimationTest<RotSplashEntity> test) -> test.setAndContinue(IDLE)));
		controllers.add(Animations.actions(this, "start", "end"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
