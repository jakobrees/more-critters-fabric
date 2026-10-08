package com.morecritters.fabric.module.corpse_crew;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.ids.CorpseGearIds;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * What every undead pirate of the corpse crew shares. Each tick a crew member looks around for
 * players and monsters (never creepers, its shipmates, or anyone dressed in pirate gear) and takes
 * one it can see as its target; it lets go of a target that died or is in creative. Between fights
 * it mutters a line now and then, and once in a while sings a shanty. It never drowns, shrugs off
 * explosions and never despawns. Killing one counts toward the crew advancements.
 */
public abstract class CorpseCrewMember extends Monster implements GeoEntity {
	private static final int SPEECH_MIN = 200;
	private static final int SPEECH_MAX = 600;
	private static final int AFTER_SONG = 600;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private int speechTimer;

	protected CorpseCrewMember(EntityType<? extends CorpseCrewMember> type, Level level, int xpReward) {
		super(type, level);
		this.xpReward = xpReward;
		this.setPersistenceRequired();
		this.speechTimer = firstSpeechDelay();
	}

	// --- What each member says and how it looks for trouble -----------------------------

	/** How far (a cube, in blocks) this member looks for someone to attack. */
	protected double lookoutRange() {
		return 8.0;
	}

	/** Whether {@code candidate} is someone this member would attack. */
	protected boolean wouldAttack(LivingEntity candidate) {
		return (candidate instanceof Player || candidate instanceof Monster)
			&& !(candidate instanceof Creeper)
			&& !(candidate instanceof CorpseCrewMember)
			&& this.hasLineOfSight(candidate)
			&& !isCreativeOrSpectator(candidate)
			&& !wearsPirateGear(candidate);
	}

	protected @Nullable SoundEvent speechSound() {
		return null;
	}

	protected @Nullable SoundEvent songSound() {
		return null;
	}

	/** One line in this many is a song instead of a remark. */
	protected int songOdds() {
		return 10;
	}

	protected int firstSpeechDelay() {
		return Mth.nextInt(this.random, SPEECH_MIN, SPEECH_MAX);
	}

	/** The sound the original played wherever this member hurts something, or null. */
	public @Nullable SoundEvent attackSound() {
		return null;
	}

	/** Played on the action controller when its melee strike lands, or null. */
	protected @Nullable String strikeAnimation() {
		return null;
	}

	/** The one-shot animations the server triggers on this member. */
	protected abstract String[] actionAnimations();

	/** The looping animation for the movement controller. */
	protected abstract RawAnimation movementAnimation(AnimationTest<CorpseCrewMember> test);

	// --- Tick -------------------------------------------------------------------------

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level() instanceof ServerLevel level) {
			pickTarget();
			crewTick(level);
			speak();
			dropFinishedTarget();
		}
	}

	/** The member's own behaviour, run each server tick between choosing and dropping a target. */
	protected abstract void crewTick(ServerLevel level);

	/**
	 * Every qualifying creature in range becomes the target in turn, nearest first, so the one
	 * kept is the farthest. That is how the original's loop worked; it is kept.
	 */
	private void pickTarget() {
		AABB area = new AABB(this.position(), this.position()).inflate(lookoutRange());
		List<LivingEntity> candidates = this.level().getEntitiesOfClass(LivingEntity.class, area, this::wouldAttack);
		candidates.stream()
			.max(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(this)))
			.ifPresent(this::setTarget);
	}

	private void speak() {
		if (--this.speechTimer > -1 || speechSound() == null) {
			return;
		}
		if (Mth.nextInt(this.random, 1, songOdds()) == 1) {
			this.speechTimer = AFTER_SONG;
			Sounds.playAt(this, songSound(), SoundSource.HOSTILE, 1.0F, 1.0F);
		} else {
			this.speechTimer = Mth.nextInt(this.random, SPEECH_MIN, SPEECH_MAX);
			Sounds.playAt(this, speechSound(), SoundSource.HOSTILE, 1.0F, 1.0F);
		}
	}

	private void dropFinishedTarget() {
		LivingEntity target = this.getTarget();
		if (target != null && (!target.isAlive() || target instanceof Player player && player.hasInfiniteMaterials())) {
			this.setTarget(null);
		}
	}

	// --- Shared helpers ---------------------------------------------------------------

	static boolean isCreativeOrSpectator(Entity entity) {
		return entity instanceof Player player && (player.isCreative() || player.isSpectator());
	}

	/** Any one piece of pirate armour makes the crew take its wearer for one of their own. */
	static boolean wearsPirateGear(LivingEntity entity) {
		return isWorn(entity, EquipmentSlot.HEAD, CorpseGearIds.Items.PIRATE_HELMET)
			|| isWorn(entity, EquipmentSlot.CHEST, CorpseGearIds.Items.PIRATE_CHESTPLATE)
			|| isWorn(entity, EquipmentSlot.LEGS, CorpseGearIds.Items.PIRATE_LEGGINGS)
			|| isWorn(entity, EquipmentSlot.FEET, CorpseGearIds.Items.PIRATE_BOOTS);
	}

	private static boolean isWorn(LivingEntity entity, EquipmentSlot slot, Identifier item) {
		var stack = entity.getItemBySlot(slot);
		return !stack.isEmpty() && BuiltInRegistries.ITEM.getOptional(item).map(stack::is).orElse(false);
	}

	/** Another module's entity type, or empty while that module is not installed. */
	static Optional<EntityType<?>> entityType(Identifier id) {
		return BuiltInRegistries.ENTITY_TYPE.getOptional(id);
	}

	/** Another module's simple particle, or empty while that module is not installed. */
	static Optional<ParticleOptions> particle(Identifier id) {
		return BuiltInRegistries.PARTICLE_TYPE.getOptional(id)
			.filter(type -> type instanceof ParticleOptions)
			.map(type -> (ParticleOptions) type);
	}

	/** A melee goal that strikes only within {@code reachSqr} (squared blocks), as the original's goals did. */
	protected MeleeAttackGoal meleeGoal(double speed, double reachSqr) {
		return new MeleeAttackGoal(this, speed, false) {
			@Override
			protected boolean canPerformAttack(LivingEntity target) {
				return this.isTimeToAttack() && this.mob.distanceToSqr(target) < reachSqr && this.mob.getSensing().hasLineOfSight(target);
			}
		};
	}

	// --- Combat and death -------------------------------------------------------------

	/** Drowning and explosions do nothing; the parrots trade drowning for falls. */
	protected boolean isImmuneTo(DamageSource source) {
		return source.is(DamageTypes.DROWN) || source.is(DamageTypes.EXPLOSION);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return !isImmuneTo(source) && super.hurtServer(level, source, damage);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		String strike = strikeAnimation();
		if (strike != null) {
			this.triggerAnim(Animations.ACTIONS, strike);
		}
		return super.doHurtTarget(level, target);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (source.getEntity() instanceof ServerPlayer player) {
			Advancements.award(player, MoreCritters.id("kill_corpse_crew_member"));
			awardKill(player);
		}
	}

	/** Extra advancements for killing this particular member. */
	protected void awardKill(ServerPlayer player) {
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	// --- Saving -----------------------------------------------------------------------

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("speech", this.speechTimer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.speechTimer = input.getIntOr("speech", this.speechTimer);
	}

	// --- Animation --------------------------------------------------------------------

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<CorpseCrewMember>(Animations.MOVEMENT, 2,
			test -> test.setAndContinue(movementAnimation(test))));
		controllers.add(Animations.actions(this, actionAnimations()));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}

	/** Walks when wandering, runs when it has someone to chase, otherwise stands idle. */
	protected static final class Gait {
		static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
		static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
		static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");

		static RawAnimation walkOrRun(CorpseCrewMember member, AnimationTest<CorpseCrewMember> test) {
			if (!test.isMoving()) return IDLE;
			return member.isAggressive() ? RUN : WALK;
		}

		static RawAnimation walkOnly(AnimationTest<CorpseCrewMember> test) {
			return test.isMoving() ? WALK : IDLE;
		}

		private Gait() {}
	}
}
