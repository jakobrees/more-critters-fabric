package com.morecritters.fabric.module.warptrap;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Config;
import com.morecritters.fabric.core.Drops;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.TextureVariants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A nether ambush critter. Standing on nylium of its own colour (warped, or crimson for the
 * ones born in a crimson forest) with nothing to hunt, it digs itself in and lies hidden. Its
 * bite yanks the victim up onto its back; it hunts Endermen, tearing them for heavy damage
 * and leaving an ender pearl behind when one dies.
 *
 * <p>"Buried" is the entity's sneaking flag, as in the original: it is synced for free and
 * picks the {@code _dig} texture on the client.
 */
public class WarptrapEntity extends Monster implements GeoEntity, TextureVariants {
	/** True for the crimson-forest colouring; chosen once at spawn. */
	private static final EntityDataAccessor<Boolean> CRIMSON = SynchedEntityData.defineId(WarptrapEntity.class, EntityDataSerializers.BOOLEAN);

	/** How long the digger effect (and the dig before it is hidden) lasts. */
	static final int DIG_TICKS = 40;
	private static final float ENDERMAN_BITE_DAMAGE = 20.0F;
	private static final double BITE_LAUNCH = 0.5;
	private static final double MELEE_REACH_SQR = 4.0;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	/** Ticks until a started dig finishes and the warptrap is hidden; 0 when not digging. */
	private int buryCountdown;

	public WarptrapEntity(EntityType<? extends WarptrapEntity> type, Level level) {
		super(type, level);
		this.xpReward = 3;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			.add(Attributes.MAX_HEALTH, 30.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 16.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CRIMSON, false);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false) {
			@Override
			protected boolean canPerformAttack(LivingEntity target) {
				return this.isTimeToAttack() && this.mob.distanceToSqr(target) < MELEE_REACH_SQR && this.mob.getSensing().hasLineOfSight(target);
			}
		});
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Enderman.class, false, false) {
			@Override
			public boolean canUse() {
				return huntsEndermen() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return huntsEndermen() && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0));
		this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this) {
			@Override
			public boolean canUse() {
				return !isBuried() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return !isBuried() && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(6, new FloatGoal(this));
	}

	private static boolean huntsEndermen() {
		return Config.flag("warptrap_attack_endermen", true);
	}

	// --- state ------------------------------------------------------------------------

	public boolean isCrimson() { return this.entityData.get(CRIMSON); }
	public boolean isBuried() { return this.isShiftKeyDown(); }
	private void setBuried(boolean buried) { this.setShiftKeyDown(buried); }

	/** {@code warptrap}, {@code warptrap_crimson}, and either with {@code _dig} while hidden. */
	@Override
	public String textureName() {
		return "warptrap" + (isCrimson() ? "_crimson" : "") + (isBuried() ? "_dig" : "");
	}

	/** The nylium this warptrap digs into, and whose particles it kicks up. */
	Block homeNylium() {
		return isCrimson() ? Blocks.CRIMSON_NYLIUM : Blocks.WARPED_NYLIUM;
	}

	/** Sprays nylium bits around the warptrap, seen from afar like the original's forced {@code /particle}. */
	void kickUpNylium(ServerLevel level) {
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, homeNylium().defaultBlockState()), true, false,
			getX(), getY(), getZ(), 7, 0.5, 0.0, 0.5, 2.0);
	}

	// --- lifecycle --------------------------------------------------------------------

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, groupData);
		setBuried(false);
		this.entityData.set(CRIMSON, level.getBiome(this.blockPosition()).is(Biomes.CRIMSON_FOREST));
		return result;
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!(this.level() instanceof ServerLevel level)) return;

		boolean onHomeNylium = level.getBlockState(BlockPos.containing(getX(), getY() - 1.0, getZ())).is(homeNylium());
		if (isBuried() && !onHomeNylium) setBuried(false);

		if (this.getTarget() == null) {
			if (!isBuried() && onHomeNylium) startDigging();
		} else if (isBuried()) {
			// Burrowing towards its prey leaves a trail of nylium.
			kickUpNylium(level);
		}
		tickBuryCountdown();
	}

	/** Digs in for two seconds (slowed, with sound and particles from the digger effect), then hides. */
	private void startDigging() {
		if (!this.hasEffect(WarptrapModule.DIGGER)) {
			this.addEffect(new MobEffectInstance(WarptrapModule.DIGGER, DIG_TICKS, 0, false, false));
		}
		if (this.buryCountdown == 0) this.buryCountdown = DIG_TICKS;
	}

	private void tickBuryCountdown() {
		if (this.buryCountdown > 0 && --this.buryCountdown == 0) setBuried(true);
	}

	// --- combat -----------------------------------------------------------------------

	/** The bite pulls the victim up onto the warptrap and brings the warptrap out of hiding. */
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		triggerAnim(Animations.ACTIONS, "attack");
		Sounds.playAt(target, WarptrapModule.BITE_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
		target.setDeltaMovement(new Vec3(0.0, BITE_LAUNCH, 0.0));
		pullOnTop(target);
		setBuried(false);
		if (target instanceof Enderman) {
			target.hurtServer(level, level.damageSources().generic(), ENDERMAN_BITE_DAMAGE);
			// The enderman tries to teleport away when hurt; drag it back.
			pullOnTop(target);
		}
		return super.doHurtTarget(level, target);
	}

	private void pullOnTop(Entity target) {
		target.teleportTo(getX(), getY() + 1.0, getZ());
	}

	@Override
	public boolean killedEntity(ServerLevel level, LivingEntity victim, DamageSource source) {
		if (victim instanceof Enderman) Drops.dropSingles(victim, Items.ENDER_PEARL, 1);
		return super.killedEntity(level, victim, source);
	}

	/** Hitting a warptrap counts as meeting one; fire and explosions do not harm it. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		Advancements.award(source.getEntity(), MoreCritters.id("encounter_warptrap"));
		if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.EXPLOSION)) return false;
		return super.hurtServer(level, source, amount);
	}

	// --- save data --------------------------------------------------------------------

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Crimson", isCrimson());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.entityData.set(CRIMSON, input.getBooleanOr("Crimson", false));
	}

	// --- sounds -----------------------------------------------------------------------

	@Override
	protected SoundEvent getAmbientSound() {
		return WarptrapModule.IDLE_SOUND;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return WarptrapModule.HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return WarptrapModule.DEATH_SOUND;
	}

	// --- GeckoLib ---------------------------------------------------------------------

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(Animations.idleWalk(this, "idle", "walk"));
		controllers.add(Animations.actions(this, "attack", "dig"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
