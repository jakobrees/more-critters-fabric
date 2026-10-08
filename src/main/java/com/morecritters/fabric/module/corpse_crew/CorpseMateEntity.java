package com.morecritters.fabric.module.corpse_crew;

import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.AnimationTest;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.TextureVariants;
import com.morecritters.fabric.ids.CorpseGearIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The deckhand of the crew, in one of five rag-and-bone outfits. Its own melee does nothing: once
 * its target is within two blocks it stops, growls, and a moment later swings its cutlass. A
 * blocking shield turns the swing into a miss, as does stepping out of reach; otherwise the victim
 * is slashed for 5. It stands still for the whole 28-tick swing. One in ten drops a cutlass.
 */
public class CorpseMateEntity extends CorpseCrewMember implements TextureVariants {
	private static final EntityDataAccessor<Integer> OUTFIT = SynchedEntityData.defineId(CorpseMateEntity.class, EntityDataSerializers.INT);
	private static final int OUTFITS = 5;
	private static final float SLASH_REACH = 2.0F;
	private static final float SLASH_DAMAGE = 5.0F;
	private static final int WIND_UP = 5;
	private static final int SWING = 3;
	private static final int RECOVERY = 23;
	private static final int CUTLASS_ODDS = 10;
	private static final ResourceKey<DamageType> SLASHED = ResourceKey.create(Registries.DAMAGE_TYPE, MoreCritters.id("slashed"));

	/** True from the growl until the swing has played out; holds the mate still. */
	private boolean slashing;

	public CorpseMateEntity(EntityType<? extends CorpseMateEntity> type, Level level) {
		super(type, level, 2);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			.add(Attributes.MAX_HEALTH, 25.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 0.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 5.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(OUTFIT, 0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new MeleeWhileFree(this));
		this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0) {
			@Override public boolean canUse() { return super.canUse() && !slashing; }
			@Override public boolean canContinueToUse() { return super.canContinueToUse() && !slashing; }
		});
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(4, new FloatGoal(this));
	}

	/** The plain chase, paused while a slash is under way. */
	private static final class MeleeWhileFree extends MeleeAttackGoal {
		private final CorpseMateEntity mate;

		MeleeWhileFree(CorpseMateEntity mate) {
			super(mate, 1.7, false);
			this.mate = mate;
		}

		@Override
		protected boolean canPerformAttack(LivingEntity target) {
			return this.isTimeToAttack() && this.mob.distanceToSqr(target) < 4.0 && this.mob.getSensing().hasLineOfSight(target);
		}

		@Override public boolean canUse() { return super.canUse() && !this.mate.slashing; }
		@Override public boolean canContinueToUse() { return super.canContinueToUse() && !this.mate.slashing; }
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		this.entityData.set(OUTFIT, Mth.nextInt(this.random, 0, OUTFITS - 1));
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	@Override
	protected void crewTick(ServerLevel level) {
		LivingEntity target = this.getTarget();
		if (target != null && !(target instanceof Player player && player.hasInfiniteMaterials())
				&& this.distanceTo(target) <= SLASH_REACH && !this.slashing) {
			startSlash();
		}
		if (this.slashing && this.onGround()) {
			this.setDeltaMovement(Vec3.ZERO);
		}
	}

	/** Growl, wind up, then swing at whoever is still in reach. */
	private void startSlash() {
		this.slashing = true;
		Sounds.playAt(this, CorpseCrewModule.MATE_READY, SoundSource.HOSTILE, 1.0F, 1.0F);
		this.triggerAnim(Animations.ACTIONS, "attack");
		ServerScheduler.runLater(WIND_UP, () -> {
			if (!this.isAlive()) return;
			LivingEntity target = this.getTarget();
			if (target != null) {
				if (this.distanceTo(target) <= SLASH_REACH) {
					ServerScheduler.runLater(SWING, this::swing);
				} else {
					Sounds.playAt(this, CorpseCrewModule.MATE_MISS, SoundSource.HOSTILE, 1.0F, 1.0F);
				}
			}
			ServerScheduler.runLater(RECOVERY, () -> this.slashing = false);
		});
	}

	private void swing() {
		LivingEntity target = this.getTarget();
		if (!this.isAlive() || target == null || !(this.level() instanceof ServerLevel level)) return;
		if (target.isBlocking()) {
			Sounds.playAt(this, CorpseCrewModule.MATE_MISS, SoundSource.HOSTILE, 1.0F, 1.0F);
		} else {
			target.hurtServer(level, level.damageSources().source(SLASHED), SLASH_DAMAGE);
			Sounds.playAt(this, CorpseCrewModule.MATE_ATTACK, SoundSource.HOSTILE, 1.0F, 1.0F);
		}
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		if (Mth.nextInt(this.random, 1, CUTLASS_ODDS) == 1) {
			BuiltInRegistries.ITEM.getOptional(CorpseGearIds.Items.CUTLASS)
				.ifPresent(cutlass -> this.spawnAtLocation(level, new ItemStack(cutlass)));
		}
	}

	@Override
	public String textureName() {
		return "corpse_mate" + this.entityData.get(OUTFIT);
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).scale(1.1F);
	}

	@Override protected @Nullable SoundEvent speechSound() { return CorpseCrewModule.MATE_SPEECH; }
	@Override protected @Nullable SoundEvent songSound() { return CorpseCrewModule.MATE_SING; }
	@Override protected SoundEvent getAmbientSound() { return CorpseCrewModule.MATE_IDLE; }
	@Override protected SoundEvent getHurtSound(DamageSource source) { return CorpseCrewModule.MATE_HURT; }
	@Override protected SoundEvent getDeathSound() { return CorpseCrewModule.MATE_DEATH; }

	@Override
	protected String[] actionAnimations() {
		return new String[] {"attack"};
	}

	@Override
	protected RawAnimation movementAnimation(AnimationTest<CorpseCrewMember> test) {
		return Gait.walkOrRun(this, test);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Datavariant", this.entityData.get(OUTFIT));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.entityData.set(OUTFIT, Mth.clamp(input.getIntOr("Datavariant", 0), 0, OUTFITS - 1));
	}
}
