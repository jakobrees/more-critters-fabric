package com.morecritters.fabric.module.critterling_system;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.TextureVariants;
import com.morecritters.fabric.ids.ArmossilloIds;
import com.morecritters.fabric.ids.AvoiderIds;
import com.morecritters.fabric.ids.BalloonRatIds;
import com.morecritters.fabric.ids.BlubberfishIds;
import com.morecritters.fabric.ids.BombJellyIds;
import com.morecritters.fabric.ids.BouncelizardIds;
import com.morecritters.fabric.ids.BunbugIds;
import com.morecritters.fabric.ids.CorpseCrewIds;
import com.morecritters.fabric.ids.CreeblossomIds;
import com.morecritters.fabric.ids.CritterlingsAIds;
import com.morecritters.fabric.ids.CritterlingsBIds;
import com.morecritters.fabric.ids.CritterlingsCIds;
import com.morecritters.fabric.ids.CritterlingsDIds;
import com.morecritters.fabric.ids.CritterlingsEIds;
import com.morecritters.fabric.ids.CustodianIds;
import com.morecritters.fabric.ids.DripperIds;
import com.morecritters.fabric.ids.GravediggerIds;
import com.morecritters.fabric.ids.IropodIds;
import com.morecritters.fabric.ids.KelpireIds;
import com.morecritters.fabric.ids.MightshroomIds;
import com.morecritters.fabric.ids.NauticrawlIds;
import com.morecritters.fabric.ids.NervoidIds;
import com.morecritters.fabric.ids.NightshroomIds;
import com.morecritters.fabric.ids.RamchuIds;
import com.morecritters.fabric.ids.ShadeletIds;
import com.morecritters.fabric.ids.ShimmerwingIds;
import com.morecritters.fabric.ids.ShriekbatIds;
import com.morecritters.fabric.ids.SnowflakeSpiderIds;
import com.morecritters.fabric.ids.StincarpIds;
import com.morecritters.fabric.ids.TreepletIds;
import com.morecritters.fabric.ids.WanderingCollectorIds;
import com.morecritters.fabric.ids.WarptrapIds;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A big-mouthed lurker that cannot walk, only lunge: every few seconds it gathers itself and
 * leaps where it looks. It hunts critterlings and nearly every other critter of the mod, opening
 * its mouth while it has prey in sight and snapping it shut on a bite. Can itself be caught in a
 * critterling sack.
 */
public class CritterEaterEntity extends PathfinderMob implements GeoEntity, TextureVariants {
	private static final EntityDataAccessor<Boolean> MOUTH_OPEN = SynchedEntityData.defineId(CritterEaterEntity.class, EntityDataSerializers.BOOLEAN);

	/** Every critter it hunts, from the original's sixty-odd target goals. */
	private static final Set<Identifier> PREY = Set.of(
		CritterlingsAIds.Entities.CUBEFROG, CritterlingsAIds.Entities.PLAINSWYRM, CritterlingsAIds.Entities.DUNGER, CritterlingsAIds.Entities.SNEK,
		CritterlingsBIds.Entities.EXPY, CritterlingsBIds.Entities.SCOWL, CritterlingsBIds.Entities.ROLLBALL, CritterlingsBIds.Entities.OPALCRAB,
		CritterlingsCIds.Entities.MOTHKID, CritterlingsCIds.Entities.GILLMUNCH, CritterlingsCIds.Entities.DOMINIC, CritterlingsCIds.Entities.OLMER,
		CritterlingsDIds.Entities.STALK, CritterlingsDIds.Entities.FLARG, CritterlingsDIds.Entities.PIRANHEED, CritterlingsDIds.Entities.MANGOTRICE,
		CritterlingsEIds.Entities.FRESNOID,
		ShimmerwingIds.Entities.SHIMMERWORM, ShimmerwingIds.Entities.SHIMMERWING, BunbugIds.Entities.BABY_BUNBUG, BunbugIds.Entities.BUNBUG,
		SnowflakeSpiderIds.Entities.SNOWFLAKE_SPIDER, ShriekbatIds.Entities.SHRIEKBAT, CreeblossomIds.Entities.CREEBLOSSOM,
		BouncelizardIds.Entities.BOUNCELIZARD, StincarpIds.Entities.STINCARP, BalloonRatIds.Entities.BALLOON_RAT, WarptrapIds.Entities.WARPTRAP,
		WanderingCollectorIds.Entities.CARRYBUG, NightshroomIds.Entities.FRIGHTSHROOM, MightshroomIds.Entities.MIGHTSHROOM,
		NightshroomIds.Entities.NIGHTSHROOM, BombJellyIds.Entities.BOMB_JELLY_LARGE, BombJellyIds.Entities.BOMB_JELLY_MEDIUM,
		BombJellyIds.Entities.BOMB_JELLY_SMALL, AvoiderIds.Entities.AVOIDER, AvoiderIds.Entities.AVOIDER_FRY, BlubberfishIds.Entities.BLUBBERFISH,
		BlubberfishIds.Entities.BLUBBERFISH_FRY, IropodIds.Entities.IROPOD, IropodIds.Entities.BLACK_IROPOD, KelpireIds.Entities.KELPIRE,
		NauticrawlIds.Entities.NAUTICRAWL, NauticrawlIds.Entities.ZOMBIE_NAUTICRAWL, ShadeletIds.Entities.SHADELET, TreepletIds.Entities.TREEPLET,
		TreepletIds.Entities.TREEPLING_BOTTOM, TreepletIds.Entities.TREEPLING_MIDDLE, TreepletIds.Entities.TREEPLING_TOP, NervoidIds.Entities.NERVOID,
		CorpseCrewIds.Entities.CORPSE_CAPTAIN, CorpseCrewIds.Entities.CORPSE_MATE, CorpseCrewIds.Entities.CORPSE_PARROT,
		CorpseCrewIds.Entities.CORPSE_QUARTERMASTER, CorpseCrewIds.Entities.CORPSE_TANK, CorpseCrewIds.Entities.TAMED_CORPSE_PARROT,
		GravediggerIds.Entities.GRAVEDIGGER, ArmossilloIds.Entities.ARMOSSILLO, ArmossilloIds.Entities.BABY_ARMOSSILLO,
		RamchuIds.Entities.RAMCHU, RamchuIds.Entities.RAMCHU_FRY, DripperIds.Entities.DRIPPER, CustodianIds.Entities.CUSTODIAN);

	/** Ticks between lunges: short while hunting, long while idle. */
	private static final int HUNTING_PAUSE_MIN = 20, HUNTING_PAUSE_MAX = 40, IDLE_PAUSE_MIN = 60, IDLE_PAUSE_MAX = 250;
	private static final int LUNGE_WIND_UP = 10;
	private static final double LUNGE_SPEED = 1.2;
	/** The original's attack reach: squared distance below 4. */
	private static final double BITE_REACH_SQR = 4.0;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	/** Counts down to the next lunge; the lunge starts when it reaches 1 (the original's {@code walk}). */
	private int lungeTimer;

	public CritterEaterEntity(EntityType<? extends CritterEaterEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.0)
			.add(Attributes.MAX_HEALTH, 35.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 10.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(MOUTH_OPEN, false);
	}

	@Override
	protected void registerGoals() {
		// It never walks (speed 0): the melee goal only turns it towards prey and bites within reach.
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.0, false) {
			@Override
			protected boolean canPerformAttack(LivingEntity target) {
				return this.isTimeToAttack() && this.mob.distanceToSqr(target) < BITE_REACH_SQR && this.mob.getSensing().hasLineOfSight(target);
			}
		});
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, false,
			(target, level) -> PREY.contains(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()))));
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(4, new FloatGoal(this));
	}

	@Override
	public String textureName() {
		return this.entityData.get(MOUTH_OPEN) ? "critter_eater_open" : "critter_eater";
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		this.lungeTimer = Mth.nextInt(this.random, IDLE_PAUSE_MIN, IDLE_PAUSE_MAX);
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level().isClientSide() || --this.lungeTimer != 1) return;
		boolean hunting = this.getTarget() != null;
		this.lungeTimer = hunting ? Mth.nextInt(this.random, HUNTING_PAUSE_MIN, HUNTING_PAUSE_MAX) : Mth.nextInt(this.random, IDLE_PAUSE_MIN, IDLE_PAUSE_MAX);
		this.entityData.set(MOUTH_OPEN, hunting);
		lunge();
	}

	/** Crouches, and half a second later leaps where it is looking. */
	private void lunge() {
		this.triggerAnim(Animations.ACTIONS, "walk1");
		ServerScheduler.runLater(LUNGE_WIND_UP, () -> {
			if (!this.isAlive()) return;
			this.setDeltaMovement(this.getLookAngle().scale(LUNGE_SPEED));
			Sounds.playAt(this, CritterlingSystemModule.CRITTER_EATER_MOVE_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
			this.triggerAnim(Animations.ACTIONS, "walk2");
		});
	}

	/** A bite shuts its mouth. */
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		this.entityData.set(MOUTH_OPEN, false);
		Sounds.playAt(target, CritterlingSystemModule.CRITTER_EATER_ATTACK_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
		this.triggerAnim(Animations.ACTIONS, "attack");
		return super.doHurtTarget(level, target);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		this.triggerAnim(Animations.ACTIONS, "hurt");
		return super.hurtServer(level, source, damage);
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("MouthOpen", this.entityData.get(MOUTH_OPEN));
		output.putInt("LungeTimer", this.lungeTimer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.entityData.set(MOUTH_OPEN, input.getBooleanOr("MouthOpen", false));
		this.lungeTimer = input.getIntOr("LungeTimer", 0);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return CritterlingSystemModule.CRITTER_EATER_IDLE_SOUND;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return CritterlingSystemModule.CRITTER_EATER_HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return CritterlingSystemModule.CRITTER_EATER_DEATH_SOUND;
	}

	// --- GeckoLib ---------------------------------------------------------------------

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		RawAnimation idle = RawAnimation.begin().thenLoop("idle");
		controllers.add(new AnimationController<CritterEaterEntity>(Animations.MOVEMENT, 0, test -> test.setAndContinue(idle)));
		controllers.add(Animations.actions(this, "walk1", "walk2", "hurt", "attack"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
