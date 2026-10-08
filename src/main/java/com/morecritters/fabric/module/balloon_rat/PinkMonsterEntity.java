package com.morecritters.fabric.module.balloon_rat;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Particles;
import java.util.Comparator;
import java.util.Set;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
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
 * The pink monster, a hallucination the hallucinazium poison conjures beside a player. It hangs in
 * the air without moving, stares the poisoned player down and holds them still, ignores almost
 * every kind of damage, and vanishes in a puff of pink eyes after two seconds or as soon as the
 * nearest player is no longer hallucinating.
 */
public class PinkMonsterEntity extends PathfinderMob implements GeoEntity {
	private static final int LIFETIME = 40;
	private static final double PLAYER_SEARCH_SIZE = 8.0;
	private static final int VANISH_PARTICLES = 15;
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	/** Damage the hallucination shrugs off (on top of any direct hit by a player, arrow or potion). */
	private static final Set<ResourceKey<DamageType>> IGNORED_DAMAGE = Set.of(DamageTypes.IN_FIRE, DamageTypes.FALL, DamageTypes.CACTUS,
			DamageTypes.DROWN, DamageTypes.LIGHTNING_BOLT, DamageTypes.EXPLOSION, DamageTypes.TRIDENT, DamageTypes.FALLING_ANVIL,
			DamageTypes.DRAGON_BREATH, DamageTypes.WITHER, DamageTypes.WITHER_SKULL, DamageTypes.IN_WALL);

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private int timer;

	public PinkMonsterEntity(EntityType<? extends PinkMonsterEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setPersistenceRequired();
		this.setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.MAX_HEALTH, 10.0)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.FOLLOW_RANGE, 16.0)
				.add(Attributes.STEP_HEIGHT, 0.6)
				.add(Attributes.FLYING_SPEED, 0.3);
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
		this.timer = LIFETIME;
		return super.finalizeSpawn(level, difficulty, reason, data);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level() instanceof ServerLevel) {
			this.timer--;
			this.setDeltaMovement(Vec3.ZERO);
			haunt();
			if (this.isAlive() && this.timer == 1) {
				vanish();
			}
		}
	}

	/** Locks eyes with the nearest hallucinating player and freezes them; anyone sober dispels it. */
	private void haunt() {
		Player player = this.level().getEntitiesOfClass(Player.class, AABB.ofSize(this.position(), PLAYER_SEARCH_SIZE, PLAYER_SEARCH_SIZE, PLAYER_SEARCH_SIZE))
				.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
		if (player == null || !player.hasEffect(BalloonRatEffects.HALLUCINAZIUM)) {
			vanish();
			return;
		}
		this.lookAt(EntityAnchorArgument.Anchor.EYES, player.position().add(0.0, player.getBbHeight(), 0.0));
		player.lookAt(EntityAnchorArgument.Anchor.EYES, this.position().add(0.0, this.getBbHeight(), 0.0));
		player.setDeltaMovement(Vec3.ZERO);
	}

	private void vanish() {
		for (int i = 0; i < VANISH_PARTICLES; i++) {
			Particles.spawnAt(this, BalloonRatModule.PINK_EYE, 1, 0.5, 0.5, 0.5, 0.12);
		}
		this.discard();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		Entity direct = source.getDirectEntity();
		if (direct instanceof AbstractArrow || direct instanceof Player || direct instanceof AbstractThrownPotion || direct instanceof AreaEffectCloud) {
			return false;
		}
		for (ResourceKey<DamageType> type : IGNORED_DAMAGE) {
			if (source.is(type)) {
				return false;
			}
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
		return false;
	}

	@Override
	public void setNoGravity(boolean ignored) {
		super.setNoGravity(true);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	protected void pushEntities() {
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.GENERIC_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.GENERIC_DEATH;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("timer", this.timer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.timer = input.getIntOr("timer", 0);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<PinkMonsterEntity>(Animations.MOVEMENT, 4, test -> test.setAndContinue(IDLE)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}
}
