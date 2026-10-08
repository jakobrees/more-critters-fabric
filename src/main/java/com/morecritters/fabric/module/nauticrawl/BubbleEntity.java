package com.morecritters.fabric.module.nauticrawl;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.SwimmingFish;
import com.morecritters.fabric.ids.MiscIds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;

/**
 * A blood bubble left in the water by a kelpire's bite. It hangs where it was made; a player
 * who touches or hits it pops it and heals two health. Out of water it pops on its own.
 */
public class BubbleEntity extends PathfinderMob implements GeoEntity {
	private static final float HEAL = 2.0F;
	private static final double TOUCH_RANGE = 0.5;

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public BubbleEntity(EntityType<? extends BubbleEntity> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
		this.setPathfindingMalus(PathType.WATER, 0.0F);
		this.moveControl = SwimmingFish.setUp(this);
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
	protected PathNavigation createNavigation(Level level) {
		return SwimmingFish.navigation(this, level);
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
	public void baseTick() {
		super.baseTick();
		if (!(level() instanceof ServerLevel level) || isRemoved()) return;
		if (!isInWater()) {
			pop(level);
			return;
		}
		for (Player player : level.getEntitiesOfClass(Player.class, new AABB(position(), position()).inflate(TOUCH_RANGE))) {
			player.setHealth(player.getHealth() + HEAL);
			pop(level);
			return;
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() == null) return super.hurtServer(level, source, amount);
		pop(level);
		if (source.getEntity() instanceof LivingEntity attacker) attacker.setHealth(attacker.getHealth() + HEAL);
		return true;
	}

	private void pop(ServerLevel level) {
		if (BuiltInRegistries.PARTICLE_TYPE.getValue(MiscIds.Particles.BLOOD_BUBBLE_POP) instanceof ParticleOptions popParticle) {
			level.sendParticles(popParticle, true, false, getX(), getY() + 0.3, getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		}
		level.playSound(null, blockPosition(), NauticrawlModule.BUBBLE_POP_SOUND, SoundSource.BLOCKS, 1.0F, 1.0F);
		discard();
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		RawAnimation idle = RawAnimation.begin().thenLoop("idle");
		controllers.add(new AnimationController<BubbleEntity>(Animations.MOVEMENT, 2, test -> test.setAndContinue(idle)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
