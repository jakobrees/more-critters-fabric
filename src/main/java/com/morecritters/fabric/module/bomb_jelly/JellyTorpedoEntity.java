package com.morecritters.fabric.module.bomb_jelly;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A jelly torpedo launched underwater from the item. It races straight ahead the way it was
 * aimed and, once armed a quarter second after launch, explodes on hitting a creature or a wall.
 * Out of water it bursts harmlessly into jelly. Nearly every kind of damage leaves it untouched.
 */
public class JellyTorpedoEntity extends PathfinderMob implements GeoEntity {
	private static final double SPEED = 0.6;
	private static final int ARMING_TICKS = 5;
	private static final double HIT_REACH = 1.5;
	private static final float EXPLOSION_POWER = 2.0F;
	private static final List<ResourceKey<DamageType>> IGNORED_DAMAGE = List.of(
		DamageTypes.IN_FIRE, DamageTypes.FALL, DamageTypes.CACTUS, DamageTypes.DROWN, DamageTypes.LIGHTNING_BOLT,
		DamageTypes.EXPLOSION, DamageTypes.TRIDENT, DamageTypes.FALLING_ANVIL, DamageTypes.DRAGON_BREATH,
		DamageTypes.WITHER, DamageTypes.WITHER_SKULL);

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private int ticksUntilArmed = ARMING_TICKS;

	public JellyTorpedoEntity(EntityType<? extends JellyTorpedoEntity> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 1.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 16.0)
			.add(Attributes.STEP_HEIGHT, 0.6);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level() instanceof ServerLevel level) {
			if (this.ticksUntilArmed > 0) {
				this.ticksUntilArmed--;
			}
			if (this.isInWater()) {
				run(level);
			} else {
				fizzle(level);
			}
		}
	}

	private boolean armed() {
		return this.ticksUntilArmed <= 0;
	}

	private void run(ServerLevel level) {
		this.setDeltaMovement(this.getLookAngle().scale(SPEED));
		if (this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6) {
			Vec3 trail = this.position();
			ServerScheduler.runLater(5, () -> level.sendParticles(ParticleTypes.BUBBLE, true, true,
				trail.x, trail.y + 0.2, trail.z, 3, 0.1, 0.1, 0.1, 0.1));
			if (this.armed() && hitsCreature(level)) {
				explode(level);
				return;
			}
		}
		if (this.armed() && hitsWall(level)) {
			explode(level);
		}
	}

	private boolean hitsCreature(ServerLevel level) {
		return !level.getEntitiesOfClass(LivingEntity.class, new AABB(this.position(), this.position()).inflate(HIT_REACH),
			target -> !(target instanceof JellyTorpedoEntity) && !(target instanceof Player player && player.hasInfiniteMaterials())).isEmpty();
	}

	/** A solid block half a block to either side, along x or z. */
	private boolean hitsWall(ServerLevel level) {
		double x = this.getX(), y = this.getY(), z = this.getZ();
		return level.getBlockState(BlockPos.containing(x + 0.5, y, z)).canOcclude()
			|| level.getBlockState(BlockPos.containing(x - 0.5, y, z)).canOcclude()
			|| level.getBlockState(BlockPos.containing(x, y, z + 0.5)).canOcclude()
			|| level.getBlockState(BlockPos.containing(x, y, z - 0.5)).canOcclude();
	}

	private void explode(ServerLevel level) {
		this.discard();
		Sounds.playAt(this, BombJellyModule.TORPEDO_EXPLODE_SOUND, SoundSource.BLOCKS, 2.0F, 1.0F);
		level.explode(null, this.getX(), this.getY(), this.getZ(), EXPLOSION_POWER, Level.ExplosionInteraction.MOB);
	}

	private void fizzle(ServerLevel level) {
		ItemParticleOption jelly = new ItemParticleOption(ParticleTypes.ITEM, BombJellyModule.explosiveJelly);
		for (int i = 0; i < 3; i++) {
			level.sendParticles(jelly, true, true, this.getX(), this.getY(), this.getZ(), 3, 0.5, 0.5, 0.5, 0.0);
		}
		this.discard();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		Entity direct = source.getDirectEntity();
		if (direct instanceof AbstractArrow || direct instanceof Player
			|| direct instanceof AbstractThrownPotion || direct instanceof AreaEffectCloud
			|| IGNORED_DAMAGE.stream().anyMatch(source::is)) {
			return false;
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(Animations.idleWalk(this, "idle", "idle"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}
}
