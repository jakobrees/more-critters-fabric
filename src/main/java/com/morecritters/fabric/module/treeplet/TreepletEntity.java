package com.morecritters.fabric.module.treeplet;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.core.Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;

/**
 * A walking birch stump. It flees players it can see, spitting a slowing resin puddle behind it every second or so;
 * now and then it stops to spin in place. Killed, it falls apart into a top, middle and bottom treepling.
 */
public class TreepletEntity extends Monster implements GeoEntity {
	private static final int SPIN_MIN = 200, SPIN_MAX = 500, SPIN_FREEZE_TICKS = 80;
	private static final int SPIT_MIN = 15, SPIT_MAX = 25, FIRST_SPIT = 25;
	private static final double WATCH_BOX = 15.0;

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private int spinTimer = Mth.nextInt(this.random, SPIN_MIN, SPIN_MAX);
	private int spitTimer = FIRST_SPIT;

	public TreepletEntity(EntityType<? extends TreepletEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			.add(Attributes.MAX_HEALTH, 20.0)
			.add(Attributes.ATTACK_DAMAGE, 0.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 7.5F, 1.5, 1.5));
		this.goalSelector.addGoal(2, new PanicGoal(this, 1.2));
		this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(5, new FloatGoal(this));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
		this.spinTimer = Mth.nextInt(this.random, SPIN_MIN, SPIN_MAX);
		this.spitTimer = FIRST_SPIT;
		return super.finalizeSpawn(level, difficulty, reason, data);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level() instanceof ServerLevel level) {
			tickSpin();
			tickSpit(level);
			watchForPlayers(level);
		}
	}

	/** Every 10 to 25 seconds, unless fleeing, it stops dead and spins. */
	private void tickSpin() {
		if (--this.spinTimer > 0) return;
		this.spinTimer = Mth.nextInt(this.random, SPIN_MIN, SPIN_MAX);
		if (this.isSprinting()) return;
		triggerAnim(Animations.ACTIONS, "spin");
		Sounds.playAt(this, TreepletModule.TREEPLET_SPIN, SoundSource.NEUTRAL, 1.0F, 1.0F);
		this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SPIN_FREEZE_TICKS, 29, false, false));
	}

	/** While fleeing it spits resin, leaving a slowing puddle where it stands. */
	private void tickSpit(ServerLevel level) {
		if (--this.spitTimer > 0) return;
		this.spitTimer = Mth.nextInt(this.random, SPIT_MIN, SPIT_MAX);
		if (!this.isSprinting()) return;
		Sounds.playAt(this, TreepletModule.TREEPLET_SPIT, SoundSource.NEUTRAL, 1.0F, 1.0F);
		triggerAnim(Animations.ACTIONS, "shoot");
		Particles.spawnAt(this, TreepletModule.RESIN, 5, 0.2, 0.2, 0.2, 0.1);
		ResinPuddleEntity puddle = TreepletModule.RESIN_PUDDLE.spawn(level, this.blockPosition(), EntitySpawnReason.MOB_SUMMONED);
		if (puddle != null) puddle.setDeltaMovement(0.0, 0.0, 0.0);
	}

	/** "Sprinting" means fleeing: the nearest player within 7.5 blocks is in sight and not in creative. */
	private void watchForPlayers(ServerLevel level) {
		Player nearest = level.getEntitiesOfClass(Player.class, AABB.ofSize(this.position(), WATCH_BOX, WATCH_BOX, WATCH_BOX)).stream()
			.min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
		this.setSprinting(nearest != null && this.hasLineOfSight(nearest) && !nearest.hasInfiniteMaterials());
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() != null) Advancements.award(source.getEntity(), MoreCritters.id("encounter_treeplet"));
		return !source.is(DamageTypes.CACTUS) && super.hurtServer(level, source, amount);
	}

	/** Falls apart into three treeplings, stacked as they stood, with a burst of birch splinters. */
	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!(this.level() instanceof ServerLevel level)) return;
		splitOff(level, TreepletModule.TREEPLING_TOP, 2);
		splitOff(level, TreepletModule.TREEPLING_MIDDLE, 1);
		splitOff(level, TreepletModule.TREEPLING_BOTTOM, 0);
		this.discard();
		BlockParticleOption birch = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BIRCH_LOG.defaultBlockState());
		for (int height = 0; height <= 2; height++) {
			level.sendParticles(birch, true, true, this.getX(), this.getY() + height, this.getZ(), 5, 0.2, 0.2, 0.2, 0.1);
		}
	}

	private void splitOff(ServerLevel level, EntityType<TreeplingEntity> part, int height) {
		TreeplingEntity treepling = part.spawn(level, BlockPos.containing(this.getX(), this.getY() + height, this.getZ()), EntitySpawnReason.MOB_SUMMONED);
		if (treepling == null) return;
		treepling.setYRot(this.getYRot());
		treepling.setYBodyRot(this.getYRot());
		treepling.setYHeadRot(this.getYRot());
		treepling.setDeltaMovement(Mth.nextDouble(this.random, -0.2, 0.2), Mth.nextDouble(this.random, -0.2, 0.2), Mth.nextDouble(this.random, -0.2, 0.2));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("spin", this.spinTimer);
		output.putInt("shoot", this.spitTimer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.spinTimer = input.getIntOr("spin", this.spinTimer);
		this.spitTimer = input.getIntOr("shoot", this.spitTimer);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return TreepletModule.TREEPLET_IDLE;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return TreepletModule.TREEPLET_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return TreepletModule.TREEPLET_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(TreepletModule.TREEPLET_STEP, 0.15F, 1.0F);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		RawAnimation idle = RawAnimation.begin().thenLoop("idle");
		RawAnimation walk = RawAnimation.begin().thenLoop("walk");
		RawAnimation run = RawAnimation.begin().thenLoop("run");
		controllers.add(new AnimationController<TreepletEntity>(Animations.MOVEMENT, 2,
			test -> test.setAndContinue(this.isSprinting() ? run : test.isMoving() ? walk : idle)));
		controllers.add(Animations.actions(this, "spin", "shoot"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}
}
