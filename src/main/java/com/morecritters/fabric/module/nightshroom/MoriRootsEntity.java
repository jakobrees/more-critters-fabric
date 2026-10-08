package com.morecritters.fabric.module.nightshroom;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Sounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Roots of the mori shroom that burst out of the ground (the mightshroom module's fungal staff
 * summons them). For two and a half seconds they hold fast anything living that stands in them,
 * then let go with a creak and sink away. Nothing can hurt them.
 */
public class MoriRootsEntity extends PathfinderMob implements GeoEntity {
	private static final int LIFETIME = 50, RELEASE_SOUND_AT = 10;
	private static final double GRIP_REACH = 0.75;
	private static final RawAnimation GRIP = RawAnimation.begin().thenLoop("spawn");

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	/** Ticks until the roots let go and vanish. */
	private int lifeTimer = LIFETIME;

	public MoriRootsEntity(EntityType<? extends MoriRootsEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 50.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		this.lifeTimer = LIFETIME;
		level.getLevel().sendParticles(Bursts.block(Blocks.MYCELIUM), this.getX(), this.getY(), this.getZ(), 55, 0.2, 0.0, 0.2, 0.05);
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!(this.level() instanceof ServerLevel level)) return;
		this.lifeTimer--;
		if (this.lifeTimer == RELEASE_SOUND_AT) {
			Sounds.playAt(this, NightshroomModule.MORI_ROOTS_RELEASE_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
		}
		if (this.lifeTimer == 1) {
			this.discard();
			return;
		}
		this.setDeltaMovement(0.0, 0.0, 0.0);
		AABB grip = new AABB(this.position(), this.position()).inflate(GRIP_REACH);
		for (LivingEntity caught : level.getEntitiesOfClass(LivingEntity.class, grip)) {
			if (caught != this) caught.teleportTo(this.getX(), this.getY(), this.getZ());
		}
	}

	/** Nothing hurts them but the void and /kill. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
		return super.hurtServer(level, source, damage);
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
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.lifeTimer = input.getIntOr("LifeTimer", LIFETIME);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(Animations.MOVEMENT, 0, (AnimationTest<MoriRootsEntity> test) -> test.setAndContinue(GRIP)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
