package com.morecritters.fabric.module.corpse_crew;

import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.AnimationTest;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.TextureVariants;
import com.morecritters.fabric.ids.MiscIds;
import com.morecritters.fabric.ids.ShipFittingsIds;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The ghost ship's captain, whose coat shimmers through five frames. When it first spots a
 * target it stops, bellows an alarm, and half a second later every mate, quartermaster, tank and
 * parrot within 50 blocks that can see the target turns on it and closes in. Every 30 seconds in a
 * fight it calls the crew (itself included, the lookout excepted) and two seconds later they
 * regenerate and grow stronger; every 10 seconds it sets idle parrots on its target, or, at peace,
 * calls them back to it. Drops a treasure key.
 */
public class CorpseCaptainEntity extends CorpseCrewMember implements TextureVariants {
	/** Coat frames and how long each shows, in ticks. */
	private static final int[] COAT_FRAME_TICKS = {3, 4, 4, 4, 3};
	private static final int COAT_CYCLE = 18;

	private static final int RALLY_INTERVAL = 600;
	private static final int RALLY_RANGE = 32;
	private static final int RALLY_DELAY = 40;
	private static final int PARROT_CALL_INTERVAL = 200;
	private static final int FIRST_PARROT_CALL = 150;
	private static final int ALARM_RANGE = 50;
	private static final int ALARM_DELAY = 10;

	private int rallyTimer = RALLY_INTERVAL;
	private int parrotCallTimer = FIRST_PARROT_CALL;
	/** Whether the alarm has been raised for the current target. */
	private boolean alarmed;

	public CorpseCaptainEntity(EntityType<? extends CorpseCaptainEntity> type, Level level) {
		super(type, level, 7);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			.add(Attributes.MAX_HEALTH, 65.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 10.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, meleeGoal(1.3, 4.0));
		this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(4, new FloatGoal(this));
	}

	@Override
	protected void crewTick(ServerLevel level) {
		if (--this.rallyTimer == 0) {
			this.rallyTimer = RALLY_INTERVAL;
			if (this.getTarget() != null) rallyCrew();
		}
		if (--this.parrotCallTimer == 0) {
			this.parrotCallTimer = PARROT_CALL_INTERVAL;
			commandParrots();
		}
		if (this.getTarget() != null) {
			if (!this.alarmed) raiseAlarm(level);
			this.alarmed = true;
		} else {
			this.alarmed = false;
		}
	}

	/** Calls the crew together; two seconds later they regenerate and hit harder. */
	private void rallyCrew() {
		for (Entity member : nearby(RALLY_RANGE)) {
			if (member instanceof CorpseCrewMember crew && !(crew instanceof CorpseLookoutEntity)) {
				ServerScheduler.runLater(RALLY_DELAY, () -> {
					crew.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1, false, true));
					crew.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 1, false, true));
				});
			}
		}
		Sounds.playAt(this, CorpseCrewModule.CAPTAIN_HEAL, SoundSource.HOSTILE, 1.0F, 1.0F);
		this.triggerAnim(Animations.ACTIONS, "call");
		this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 45, 99, false, false));
	}

	/** In a fight, sets every parrot without a target on the captain's; otherwise calls them over. */
	private void commandParrots() {
		LivingEntity target = this.getTarget();
		for (Entity entity : nearby(RALLY_RANGE)) {
			if (entity instanceof CorpseParrotEntity parrot && parrot.getTarget() == null) {
				if (target != null) {
					parrot.setTarget(target);
				} else {
					parrot.getNavigation().moveTo(this.getX(), this.getY(), this.getZ(), 2.0);
				}
			}
		}
	}

	/** Stops, bellows, and half a second later sends the crew that can see the target after it. */
	private void raiseAlarm(ServerLevel level) {
		this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 30, false, false));
		this.triggerAnim(Animations.ACTIONS, "alert");
		Sounds.playAt(this, CorpseCrewModule.CAPTAIN_ALERT, SoundSource.HOSTILE, 2.0F, 1.0F);
		markAbove(level, this, MiscIds.Particles.ALERT);
		Vec3 musterPoint = this.position();
		ServerScheduler.runLater(ALARM_DELAY, () -> {
			LivingEntity target = this.getTarget();
			if (target == null) return;
			for (Entity entity : level.getEntitiesOfClass(Entity.class, new AABB(musterPoint, musterPoint).inflate(ALARM_RANGE))) {
				if (entity instanceof CorpseCrewMember crew && answersAlarm(crew) && crew.hasLineOfSight(target)) {
					crew.setTarget(target);
					crew.getNavigation().moveTo(musterPoint.x, musterPoint.y, musterPoint.z, 0.8);
					markAbove(level, crew, MiscIds.Particles.ALERTED);
				}
			}
		});
	}

	private static boolean answersAlarm(CorpseCrewMember crew) {
		return crew instanceof CorpseParrotEntity || crew instanceof CorpseMateEntity
			|| crew instanceof CorpseQuartermasterEntity || crew instanceof CorpseTankEntity;
	}

	/** One particle a block above the entity's head (the misc module owns the alert particles). */
	private static void markAbove(ServerLevel level, Entity entity, Identifier particleId) {
		particle(particleId).ifPresent(particle ->
			level.sendParticles(particle, entity.getX(), entity.getY() + entity.getBbHeight() + 1.0, entity.getZ(), 1, 0.0, 0.0, 0.0, 0.0));
	}

	private List<Entity> nearby(double range) {
		return this.level().getEntitiesOfClass(Entity.class, new AABB(this.position(), this.position()).inflate(range));
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		BuiltInRegistries.ITEM.getOptional(ShipFittingsIds.Items.TREASURE_KEY)
			.ifPresent(key -> this.spawnAtLocation(level, new ItemStack(key)));
	}

	@Override
	protected void awardKill(ServerPlayer player) {
		Advancements.award(player, MoreCritters.id("kill_captain"));
	}

	@Override
	public String textureName() {
		int tick = this.tickCount % COAT_CYCLE;
		int frame = 0;
		while (tick >= COAT_FRAME_TICKS[frame]) {
			tick -= COAT_FRAME_TICKS[frame++];
		}
		return "corpse_captain" + (frame + 1);
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).scale(1.1F);
	}

	@Override public @Nullable SoundEvent attackSound() { return CorpseCrewModule.CAPTAIN_ATTACK; }
	@Override protected @Nullable String strikeAnimation() { return "attack"; }
	@Override protected @Nullable SoundEvent speechSound() { return CorpseCrewModule.CAPTAIN_SPEECH; }
	@Override protected @Nullable SoundEvent songSound() { return CorpseCrewModule.CAPTAIN_SING; }
	@Override protected SoundEvent getAmbientSound() { return CorpseCrewModule.CAPTAIN_IDLE; }
	@Override protected SoundEvent getHurtSound(DamageSource source) { return CorpseCrewModule.CAPTAIN_HURT; }
	@Override protected SoundEvent getDeathSound() { return CorpseCrewModule.CAPTAIN_DEATH; }

	@Override
	protected String[] actionAnimations() {
		return new String[] {"attack", "alert", "call"};
	}

	@Override
	protected RawAnimation movementAnimation(AnimationTest<CorpseCrewMember> test) {
		return Gait.walkOrRun(this, test);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("callcrew", this.rallyTimer);
		output.putInt("call", this.parrotCallTimer);
		output.putBoolean("target", this.alarmed);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.rallyTimer = input.getIntOr("callcrew", this.rallyTimer);
		this.parrotCallTimer = input.getIntOr("call", this.parrotCallTimer);
		this.alarmed = input.getBooleanOr("target", false);
	}
}
