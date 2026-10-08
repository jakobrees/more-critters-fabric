package com.morecritters.fabric.module.mightshroom;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.TextureVariants;
import com.morecritters.fabric.ids.MightshroomIds;
import com.morecritters.fabric.ids.ShockCubeIds;
import com.morecritters.fabric.ids.ShriekbatIds;
import java.util.List;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * A ripple left behind for two seconds, fading through its textures every ten ticks. The mightshroom's echo
 * (where it lands from a leap) zaps everything within four blocks now and then; the heal echoes (from the fungal
 * staff) are only a sight. Echoes stand still, cannot be pushed and shrug off nearly all damage.
 */
public class EchoEntity extends PathfinderMob implements GeoEntity, TextureVariants {
	/** Which echo it is: its textures from fresh to faded, and whether it zaps. */
	public enum Kind {
		MIGHTSHROOM(true, "mightshroom_echo", "mightshroom_echo1", "mightshroom_echo2", "mightshroom_echo3"),
		HEAL(false, "heal_echo1", "heal_echo2", "heal_echo3", "heal_echo4");

		final boolean zaps;
		final List<String> textures;

		Kind(boolean zaps, String... textures) {
			this.zaps = zaps;
			this.textures = List.of(textures);
		}
	}

	private static final EntityDataAccessor<Integer> FADE = SynchedEntityData.defineId(EchoEntity.class, EntityDataSerializers.INT);
	private static final int LIFESPAN = 40;
	private static final int FADE_STEP = 10;
	private static final int ZAP_CHANCE = 6;
	private static final double ZAP_RANGE = 4.0;
	private static final double ZAP_MIN = 2.0, ZAP_MAX = 6.0;
	private static final List<ResourceKey<DamageType>> IGNORED_DAMAGE = List.of(
		DamageTypes.IN_FIRE, DamageTypes.FALL, DamageTypes.CACTUS, DamageTypes.DROWN, DamageTypes.LIGHTNING_BOLT,
		DamageTypes.EXPLOSION, DamageTypes.TRIDENT, DamageTypes.FALLING_ANVIL, DamageTypes.DRAGON_BREATH,
		DamageTypes.WITHER, DamageTypes.WITHER_SKULL, DamageTypes.IN_WALL);
	/** The mightshroom's own kind, shriekbat echoes and shock cubes are spared by the zaps. */
	private static final List<Identifier> SPARED = List.of(
		MightshroomIds.Entities.MIGHTSHROOM, MightshroomIds.Entities.MIGHTSHROOM_ECHO,
		ShriekbatIds.Entities.ECHO, ShriekbatIds.Entities.LARGE_ECHO, ShockCubeIds.Entities.SHOCK_CUBE);

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private final Kind kind;
	private int lifeLeft = LIFESPAN;

	public EchoEntity(EntityType<? extends EchoEntity> type, Level level, Kind kind) {
		super(type, level);
		this.kind = kind;
		this.xpReward = 0;
		this.setNoAi(true);
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
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
		builder.define(FADE, 0);
	}

	@Override
	public String textureName() {
		return kind.textures.get(Math.min(this.entityData.get(FADE), kind.textures.size() - 1));
	}

	@Override
	public void baseTick() {
		super.baseTick();
		setDeltaMovement(0.0, 0.0, 0.0);
		lookAt(EntityAnchorArgument.Anchor.EYES, position());
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		lifeLeft--;
		if (lifeLeft < 0) {
			discard();
			return;
		}
		if (lifeLeft % FADE_STEP == 0 && lifeLeft > 0) {
			this.entityData.set(FADE, (LIFESPAN - lifeLeft) / FADE_STEP);
		}
		if (kind.zaps && this.random.nextInt(ZAP_CHANCE) == 0) {
			zap(level);
		}
	}

	/** Lightning damage to everything (items too) within four blocks, but its own kind. */
	private void zap(ServerLevel level) {
		AABB area = new AABB(position(), position()).inflate(ZAP_RANGE);
		for (Entity entity : level.getEntitiesOfClass(Entity.class, area, entity -> !isSpared(entity))) {
			entity.hurtServer(level, level.damageSources().lightningBolt(), (float) Mth.nextDouble(this.random, ZAP_MIN, ZAP_MAX));
		}
	}

	private static boolean isSpared(Entity entity) {
		return SPARED.stream().anyMatch(type -> OtherModules.isOfType(entity, type));
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		Entity direct = source.getDirectEntity();
		if (direct instanceof AbstractArrow || direct instanceof Player || direct instanceof AbstractThrownPotion || direct instanceof AreaEffectCloud) {
			return false;
		}
		if (IGNORED_DAMAGE.stream().anyMatch(source::is)) {
			return false;
		}
		return super.hurtServer(level, source, amount);
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
		output.putInt("LifeLeft", lifeLeft);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		lifeLeft = input.getIntOr("LifeLeft", LIFESPAN);
		this.entityData.set(FADE, Math.max(0, (LIFESPAN - lifeLeft) / FADE_STEP));
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(Animations.idleWalk(this, "idle", "idle"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
