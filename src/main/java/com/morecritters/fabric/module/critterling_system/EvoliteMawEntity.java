package com.morecritters.fabric.module.critterling_system;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.ServerScheduler;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Illusioner;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
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
 * The evolutioner's summoned trap, like evoker fangs: it slams down to the ground unseen, rises
 * with a crunch, bites everything standing on its spot for 7 damage (sparing the evolutioner and
 * illager-kind) and is gone two seconds after it appeared. Almost nothing can hurt it.
 */
public class EvoliteMawEntity extends PathfinderMob implements GeoEntity {
	private static final int LIFETIME = 40;
	private static final int RISE_DELAY = 10, BITE_DELAY = 15;
	private static final float BITE_DAMAGE = 7.0F;
	private static final double BITE_REACH = 0.5;
	private static final double FALL_SPEED = -2.0;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private int ticksLeft = LIFETIME;

	public EvoliteMawEntity(EntityType<? extends EvoliteMawEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
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

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		emerge(level.getLevel());
		return data;
	}

	/** Witch sparks, then after half a second the crunch and the rise, and a quarter second later the bite. */
	private void emerge(ServerLevel level) {
		hide();
		Vec3 spot = this.position();
		level.sendParticles(ParticleTypes.WITCH, true, false, spot.x, spot.y + 1.0, spot.z, 25, 0.1, 1.0, 0.1, 1.0);
		ServerScheduler.runLater(RISE_DELAY, () -> {
			level.playSound(null, this.blockPosition(), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.NEUTRAL, 1.0F, 1.0F);
			this.triggerAnim(Animations.ACTIONS, "spawn");
			ServerScheduler.runLater(BITE_DELAY, () -> bite(level, spot));
		});
	}

	private void bite(ServerLevel level, Vec3 spot) {
		List<Entity> victims = level.getEntities(this, new AABB(spot, spot).inflate(BITE_REACH), EvoliteMawEntity::isBitten);
		for (Entity victim : victims) {
			victim.hurtServer(level, level.damageSources().generic(), BITE_DAMAGE);
		}
	}

	private static boolean isBitten(Entity entity) {
		return !(entity instanceof EvoliteMawEntity || entity instanceof EvolutionerEntity || entity instanceof Witch
			|| entity instanceof Evoker || entity instanceof Pillager || entity instanceof Vindicator || entity instanceof Illusioner);
	}

	private void hide() {
		this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 10, 0, false, false));
	}

	/** Slams down while in the air, unseen; gone when stuck in a wall or when its time is up. */
	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level().isClientSide()) return;
		if (!this.onGround()) {
			hide();
			this.setDeltaMovement(0.0, FALL_SPEED, 0.0);
		} else {
			this.removeAllEffects();
		}
		if (this.isInWall() || --this.ticksLeft <= 0) {
			this.discard();
		}
	}

	/** Immune to nearly everything: fire, arrows, players, potions, falls, cacti, drowning, lightning, explosions, tridents, anvils, dragon breath and withering. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		Entity direct = source.getDirectEntity();
		if (direct instanceof AbstractArrow || direct instanceof Player || direct instanceof AbstractThrownPotion || direct instanceof AreaEffectCloud) {
			return false;
		}
		if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.FALL) || source.is(DamageTypes.CACTUS) || source.is(DamageTypes.DROWN)
			|| source.is(DamageTypes.LIGHTNING_BOLT) || source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.TRIDENT)
			|| source.is(DamageTypes.FALLING_ANVIL) || source.is(DamageTypes.DRAGON_BREATH) || source.is(DamageTypes.WITHER)
			|| source.is(DamageTypes.WITHER_SKULL)) {
			return false;
		}
		return super.hurtServer(level, source, damage);
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("TicksLeft", this.ticksLeft);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.ticksLeft = input.getIntOr("TicksLeft", LIFETIME);
	}

	// --- GeckoLib ---------------------------------------------------------------------

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		RawAnimation idle = RawAnimation.begin().thenLoop("0");
		controllers.add(new AnimationController<EvoliteMawEntity>(Animations.MOVEMENT, 0, test -> test.setAndContinue(idle)));
		controllers.add(Animations.actions(this, "spawn"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
