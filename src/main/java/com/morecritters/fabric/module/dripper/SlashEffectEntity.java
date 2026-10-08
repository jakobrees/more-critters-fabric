package com.morecritters.fabric.module.dripper;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.TextureVariants;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
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

import java.util.List;

/**
 * The wave of energy a crouching Slashklub hit releases. It glides forward along the
 * wielder's facing for a second and a half, fading through five textures, and every tick
 * shoves away and hurts every non-player creature within two and a half blocks.
 */
public class SlashEffectEntity extends Monster implements GeoEntity, TextureVariants {
	/** Texture 1..5, synced so the client can show the fade. */
	private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(SlashEffectEntity.class, EntityDataSerializers.INT);

	private static final int LIFETIME = 30;
	/** Remaining-life values at which the texture moves to stage 2, 3, 4 and 5. */
	private static final int[] STAGE_STARTS = {25, 20, 15, 10};
	private static final double GLIDE_SPEED = 0.3;
	private static final double REACH = 2.5;
	private static final double SHOVE = 0.5;
	private static final float DAMAGE = 3.0F;

	/** Damage the effect ignores, as in the original. */
	private static final List<ResourceKey<DamageType>> IMMUNE_TO = List.of(
		DamageTypes.IN_FIRE, DamageTypes.FALL, DamageTypes.CACTUS, DamageTypes.DROWN, DamageTypes.LIGHTNING_BOLT,
		DamageTypes.EXPLOSION, DamageTypes.TRIDENT, DamageTypes.FALLING_ANVIL, DamageTypes.DRAGON_BREATH,
		DamageTypes.WITHER, DamageTypes.WITHER_SKULL
	);

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	/** Ticks left before it vanishes. */
	private int lifeLeft = LIFETIME;

	public SlashEffectEntity(EntityType<? extends SlashEffectEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 10.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(STAGE, 1);
	}

	@Override
	public String textureName() {
		return "slash_effect" + this.entityData.get(STAGE);
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		this.lifeLeft = LIFETIME;
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		Vec3 look = this.getLookAngle();
		this.setDeltaMovement(GLIDE_SPEED * look.x, 0.0, GLIDE_SPEED * look.z);
		if (!(this.level() instanceof ServerLevel level)) return;

		if (--this.lifeLeft < 0) {
			this.discard();
			return;
		}
		this.entityData.set(STAGE, stageFor(this.lifeLeft));
		strikeNearby(level);
	}

	private static int stageFor(int lifeLeft) {
		int stage = 1;
		for (int start : STAGE_STARTS) {
			if (lifeLeft <= start) stage++;
		}
		return stage;
	}

	/** Every creature in reach except players and other slashes is turned towards it, shoved back and hurt. */
	private void strikeNearby(ServerLevel level) {
		Vec3 centre = this.position();
		List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(REACH),
			e -> !(e instanceof SlashEffectEntity) && !(e instanceof Player));
		for (LivingEntity victim : victims) {
			victim.lookAt(EntityAnchorArgument.Anchor.EYES, centre);
			Vec3 facing = victim.getLookAngle();
			victim.push(-SHOVE * facing.x, -SHOVE * facing.y, -SHOVE * facing.z);
			victim.hurtServer(level, level.damageSources().generic(), DAMAGE);
		}
	}

	/** Shrugs off most environmental damage, arrows, potions and players' hits. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		Entity direct = source.getDirectEntity();
		if (direct instanceof AbstractArrow || direct instanceof Player || direct instanceof AbstractThrownPotion || direct instanceof AreaEffectCloud) return false;
		for (ResourceKey<DamageType> immune : IMMUNE_TO) {
			if (source.is(immune)) return false;
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).scale(2.0F);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("LifeLeft", this.lifeLeft);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.lifeLeft = input.getIntOr("LifeLeft", LIFETIME);
		this.entityData.set(STAGE, stageFor(this.lifeLeft));
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.GENERIC_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.GENERIC_DEATH;
	}

	// --- GeckoLib: the model has no animations the entity drives ---------------------

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
