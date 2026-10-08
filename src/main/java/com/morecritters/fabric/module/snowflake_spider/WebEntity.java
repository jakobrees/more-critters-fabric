package com.morecritters.fabric.module.snowflake_spider;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Sounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The freezing web that holds a creature with the Webbed effect. It sits still where the
 * effect started, solid to the touch, while the {@link WebbedEffect} pins its captive to it.
 * It breaks apart in a burst of frosty cobweb when nothing webbed is left inside it, when
 * someone cuts it with shears, or when it is destroyed; players' hits and most environmental
 * damage leave it alone.
 */
public class WebEntity extends PathfinderMob implements GeoEntity {
	/** The web and its captive are looked for in a one-block cube around the web. */
	static final double HOLD_SIZE = 1.0;

	/** Damage the web ignores, as in the original. */
	private static final List<ResourceKey<DamageType>> IMMUNE_TO = List.of(
		DamageTypes.IN_FIRE, DamageTypes.FALL, DamageTypes.CACTUS, DamageTypes.DROWN, DamageTypes.LIGHTNING_BOLT,
		DamageTypes.EXPLOSION, DamageTypes.TRIDENT, DamageTypes.FALLING_ANVIL, DamageTypes.DRAGON_BREATH,
		DamageTypes.WITHER, DamageTypes.WITHER_SKULL
	);

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	/** Set by a natural/summoned spawn; the "spawn" animation plays once clients can see the web. */
	private boolean spawnAnimationPending;

	public WebEntity(EntityType<? extends WebEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setNoAi(true);
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 5.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		this.spawnAnimationPending = true;
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!(this.level() instanceof ServerLevel level)) return;

		if (this.spawnAnimationPending) {
			this.spawnAnimationPending = false;
			this.triggerAnim(Animations.ACTIONS, "spawn");
		}
		if (!holdsWebbedCreature(level)) {
			breakApart(level);
		}
	}

	/** Whether some creature with the Webbed effect is still caught inside the web. */
	private boolean holdsWebbedCreature(ServerLevel level) {
		return !level.getEntitiesOfClass(LivingEntity.class, holdArea(),
			e -> e != this && e.hasEffect(SnowflakeSpiderModule.WEBBED)).isEmpty();
	}

	private AABB holdArea() {
		return AABB.ofSize(this.position(), HOLD_SIZE, HOLD_SIZE, HOLD_SIZE);
	}

	/** Shears cut the web loose. */
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		super.mobInteract(player, hand);
		if (this.level() instanceof ServerLevel level && player.getMainHandItem().is(Items.SHEARS)) {
			player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
			Sounds.playAt(this, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
			breakApart(level);
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * Vanishes in a few puffs of freezing cobweb. Creatures it held get their AI back; the
	 * effect would do that itself on its next tick, but not if it was cleared some other way.
	 */
	private void breakApart(ServerLevel level) {
		for (Mob captive : level.getEntitiesOfClass(Mob.class, holdArea(), e -> !(e instanceof WebEntity))) {
			captive.setNoAi(false);
		}
		BlockParticleOption cobweb = new BlockParticleOption(ParticleTypes.BLOCK, SnowflakeSpiderModule.FREEZING_COBWEB.defaultBlockState());
		int puffs = Mth.nextInt(this.getRandom(), 2, 5);
		for (int i = 0; i < puffs; i++) {
			level.sendParticles(cobweb, true, false, this.getX(), this.getY(), this.getZ(), 10, 0.2, 0.2, 0.2, 0.03);
			level.sendParticles(cobweb, true, false, this.getX(), this.getY() + 1.0, this.getZ(), 10, 0.2, 0.2, 0.2, 0.03);
		}
		this.discard();
	}

	/** Plays its shiver on any hit, but shrugs off players, arrows, potions and most environmental damage. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		this.triggerAnim(Animations.ACTIONS, "hurt");
		Entity direct = source.getDirectEntity();
		if (direct instanceof AbstractArrow || direct instanceof Player || direct instanceof AbstractThrownPotion || direct instanceof AreaEffectCloud) return false;
		for (ResourceKey<DamageType> immune : IMMUNE_TO) {
			if (source.is(immune)) return false;
		}
		return super.hurtServer(level, source, amount);
	}

	/** A destroyed web leaves nothing behind, not even a death animation. */
	@Override
	public void die(DamageSource source) {
		super.die(source);
		this.discard();
	}

	/** Solid like a boat while it stands. */
	@Override
	public boolean canBeCollidedWith(@Nullable Entity other) {
		return this.isAlive();
	}

	@Override
	public boolean canCollideWith(Entity entity) {
		return true;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
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

	// --- GeckoLib -------------------------------------------------------------------

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(Animations.idleWalk(this, "idle", "idle"));
		controllers.add(Animations.actions(this, "spawn", "hurt"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
