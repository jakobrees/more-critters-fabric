package com.morecritters.fabric.module.critterling_system;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.TextureVariants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * An illager mage who lives in the evolutioner tower. He keeps his distance from the player he
 * hunts, and every seven and a half seconds chants a spell: he stands still, arm raised, and
 * shortly before the spell ends "evolves" every witch, pillager, vindicator and evoker within five
 * blocks into a different one of the four. If there are none of those around, he raises five
 * evolite maws from the ground around him instead.
 */
public class EvolutionerEntity extends Monster implements GeoEntity, TextureVariants {
	private static final EntityDataAccessor<Boolean> CASTING = SynchedEntityData.defineId(EvolutionerEntity.class, EntityDataSerializers.BOOLEAN);

	private static final int FIRST_SPELL = 160, SPELL_INTERVAL = 150, SPELL_LENGTH = 70;
	/** Points in the spell's countdown: the casting gesture, the effect, the end of the casting animation, and the end. */
	private static final int CAST_GESTURE_AT = 10, SPELL_EFFECT_AT = 5, GESTURE_END_AT = 0, SPELL_END_AT = 1;
	private static final double TRANSFORM_REACH = 5.0, ILLAGER_CHECK_SIZE = 10.0;
	private static final int MAWS = 5;
	private static final double MAW_SPREAD = 8.0;
	private static final double KEEP_AWAY_DISTANCE = 4.0, BACK_OFF_PUSH = 0.1;

	private static final RawAnimation SPELL = RawAnimation.begin().thenLoop("spell");

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	/** Counts down to the next spell; fires at 1 (the original's {@code attack}). */
	private int spellTimer;
	/** Counts down through the spell being cast (the original's {@code magic}). */
	private int castTimer;

	public EvolutionerEntity(EntityType<? extends EvolutionerEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			.add(Attributes.MAX_HEALTH, 50.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CASTING, false);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new RandomStrollGoal(this, 1.0));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
		// Follows its target but never strikes: its only weapons are spells.
		this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, false) {
			@Override
			protected boolean canPerformAttack(LivingEntity target) {
				return false;
			}
		});
		this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(6, new FloatGoal(this));
	}

	public boolean isCasting() {
		return this.entityData.get(CASTING);
	}

	@Override
	public String textureName() {
		return isCasting() ? "evolutioner_arm" : "evolutioner";
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		this.spellTimer = FIRST_SPELL;
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!(this.level() instanceof ServerLevel level)) return;
		this.spellTimer--;
		this.castTimer--;
		dropTargetIfOutOfReach();
		if (isCasting()) {
			this.getNavigation().stop();
			level.sendParticles(ParticleTypes.WITCH, true, false, this.getX(), this.getY() + 1.0, this.getZ(), 1, 0.1, 1.0, 0.1, 1.0);
		}
		if (this.spellTimer == 1) startSpell();
		if (this.castTimer == CAST_GESTURE_AT && this.getTarget() != null) this.triggerAnim(Animations.ACTIONS, "cast");
		if (this.castTimer == GESTURE_END_AT) this.stopTriggeredAnim(Animations.ACTIONS, "cast");
		if (this.castTimer == SPELL_EFFECT_AT && this.getTarget() != null) castSpell(level, this.getTarget());
		if (this.castTimer == SPELL_END_AT) this.entityData.set(CASTING, false);
		backOffFromTarget();
	}

	/** Starts chanting, if there is someone to cast at. */
	private void startSpell() {
		this.spellTimer = SPELL_INTERVAL;
		this.castTimer = SPELL_LENGTH;
		if (this.getTarget() != null) {
			this.entityData.set(CASTING, true);
			Sounds.playAt(this, CritterlingSystemModule.EVOLUTIONER_CHANT_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
		}
	}

	private void castSpell(ServerLevel level, LivingEntity target) {
		List<Mob> allies = level.getEntitiesOfClass(Mob.class, new AABB(this.position(), this.position()).inflate(TRANSFORM_REACH),
			mob -> mob instanceof Witch || mob instanceof Pillager || mob instanceof Vindicator || mob instanceof Evoker);
		for (Mob ally : allies) {
			evolve(level, ally);
		}
		if (noIllagersAround(level) && this.hasLineOfSight(target)) {
			for (int i = 0; i < MAWS; i++) {
				BlockPos at = BlockPos.containing(this.getX() + this.random.nextDouble() * 2 * MAW_SPREAD - MAW_SPREAD, this.getY(),
					this.getZ() + this.random.nextDouble() * 2 * MAW_SPREAD - MAW_SPREAD);
				CritterlingSystemModule.EVOLITE_MAW.spawn(level, at, EntitySpawnReason.MOB_SUMMONED);
			}
		}
	}

	/** Turns a witch or illager into one of the other three, in a puff of smoke. */
	private void evolve(ServerLevel level, Mob ally) {
		EntityType<? extends Mob> into = evolvedForm(ally);
		Mob evolved = into.spawn(level, ally.blockPosition(), EntitySpawnReason.MOB_SUMMONED);
		if (evolved != null) {
			evolved.setYRot(this.getYRot());
			evolved.setYBodyRot(this.getYRot());
			evolved.setYHeadRot(this.getYRot());
			evolved.setDeltaMovement(Vec3.ZERO);
		}
		level.sendParticles(ParticleTypes.WITCH, true, false, ally.getX(), ally.getY(), ally.getZ(), 12, 0.5, 1.0, 0.5, 0.02);
		level.sendParticles(ParticleTypes.POOF, true, false, ally.getX(), ally.getY() + 1.0, ally.getZ(), 5, 1.0, 0.5, 1.0, 0.02);
		level.playSound(null, ally.blockPosition(), CritterlingSystemModule.EVOLUTIONER_TRANSFORM_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
		ally.discard();
	}

	private EntityType<? extends Mob> evolvedForm(Mob ally) {
		List<EntityType<? extends Mob>> forms = new ArrayList<>(List.of(EntityTypes.WITCH, EntityTypes.PILLAGER, EntityTypes.VINDICATOR, EntityTypes.EVOKER));
		forms.remove(ally.getType());
		return forms.get(this.random.nextInt(forms.size()));
	}

	private boolean noIllagersAround(ServerLevel level) {
		AABB area = AABB.ofSize(this.position(), ILLAGER_CHECK_SIZE, ILLAGER_CHECK_SIZE, ILLAGER_CHECK_SIZE);
		return level.getEntitiesOfClass(Mob.class, area,
			mob -> mob instanceof Vindicator || mob instanceof Pillager || mob instanceof Witch || mob instanceof SpellcasterIllager).isEmpty();
	}

	/** Steps back from a target that comes within four blocks, unless it is casting. */
	private void backOffFromTarget() {
		LivingEntity target = this.getTarget();
		if (target == null || isCasting() || !this.onGround()) return;
		float distance = this.distanceTo(target);
		if (distance > 0.0F && distance <= KEEP_AWAY_DISTANCE) {
			Vec3 look = this.getLookAngle();
			this.push(-BACK_OFF_PUSH * look.x, -BACK_OFF_PUSH, -BACK_OFF_PUSH * look.z);
		}
	}

	/** Gives up on creative-mode players and on targets that died. */
	private void dropTargetIfOutOfReach() {
		LivingEntity target = this.getTarget();
		if (target != null && (target instanceof Player player && player.hasInfiniteMaterials() || !target.isAlive())) {
			this.setTarget(null);
		}
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Casting", isCasting());
		output.putInt("SpellTimer", this.spellTimer);
		output.putInt("CastTimer", this.castTimer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.entityData.set(CASTING, input.getBooleanOr("Casting", false));
		this.spellTimer = input.getIntOr("SpellTimer", FIRST_SPELL);
		this.castTimer = input.getIntOr("CastTimer", 0);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return CritterlingSystemModule.EVOLUTIONER_IDLE_SOUND;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return CritterlingSystemModule.EVOLUTIONER_HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return CritterlingSystemModule.EVOLUTIONER_DEATH_SOUND;
	}

	// --- GeckoLib ---------------------------------------------------------------------

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(Animations.idleWalk(this, "idle", "walk"));
		controllers.add(new AnimationController<EvolutionerEntity>("spell", 4,
			test -> isCasting() ? test.setAndContinue(SPELL) : PlayState.STOP));
		controllers.add(Animations.actions(this, "cast"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
