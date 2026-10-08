package com.morecritters.fabric.module.misc;

import com.morecritters.fabric.ids.CritterlingSystemIds;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.TextureVariants;
import java.time.LocalDate;
import java.time.Month;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The anniversary veteran: a shy party guest that wanders the Overworld only on the mod's
 * anniversary (7 to 9 August), keeps away from players, and bursts into confetti when it dies.
 * Each one wears one of four looks, picked when it spawns.
 */
public class AnniveteranEntity extends PathfinderMob implements GeoEntity, TextureVariants {
	private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(AnniveteranEntity.class, EntityDataSerializers.INT);
	private static final int VARIANT_COUNT = 4;

	private static final Month ANNIVERSARY_MONTH = Month.AUGUST;
	private static final int ANNIVERSARY_FIRST_DAY = 7;
	private static final int ANNIVERSARY_LAST_DAY = 9;

	private static final float STEP_VOLUME = 0.15F;
	private static final int CONFETTI_COUNT = 12;
	private static final double CONFETTI_HEIGHT = 2.0;
	private static final double CONFETTI_SPREAD = 0.4;
	private static final double CONFETTI_SPEED = 0.1;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);

	public AnniveteranEntity(EntityType<? extends AnniveteranEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 20.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	/** Natural spawns only happen in the Overworld during the anniversary, by the server's clock. */
	public static boolean canSpawnAt(EntityType<AnniveteranEntity> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return level.getLevel().dimension() == Level.OVERWORLD && isAnniversary(LocalDate.now());
	}

	private static boolean isAnniversary(LocalDate today) {
		return today.getMonth() == ANNIVERSARY_MONTH
			&& today.getDayOfMonth() >= ANNIVERSARY_FIRST_DAY && today.getDayOfMonth() <= ANNIVERSARY_LAST_DAY;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(VARIANT, 0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 12.0F, 1.0, 1.2));
		this.goalSelector.addGoal(2, new PanicGoal(this, 1.2));
		this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(5, new FloatGoal(this));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		this.entityData.set(VARIANT, Mth.nextInt(this.random, 0, VARIANT_COUNT - 1));
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	private int variant() {
		return this.entityData.get(VARIANT);
	}

	/** {@code anniveteran}, {@code anniveteran1}, {@code anniveteran2} or {@code anniveteran3}. */
	@Override
	public String textureName() {
		return variant() == 0 ? "anniveteran" : "anniveteran" + variant();
	}

	// --- Sounds -----------------------------------------------------------------------

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(MiscModule.ANNIVETERAN_STEP_SOUND, STEP_VOLUME, 1.0F);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return MiscModule.ANNIVETERAN_HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return MiscModule.ANNIVETERAN_DEATH_SOUND;
	}

	// --- Hurt and death ---------------------------------------------------------------

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		triggerAnim(Animations.ACTIONS, "hurt");
		return super.hurtServer(level, source, amount);
	}

	/** A burst of confetti above the body. */
	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (this.level() instanceof ServerLevel level
			&& BuiltInRegistries.PARTICLE_TYPE.getValue(CritterlingSystemIds.Particles.CONFETTI) instanceof ParticleOptions confetti) {
			level.sendParticles(confetti, true, false, getX(), getY() + CONFETTI_HEIGHT, getZ(), CONFETTI_COUNT,
				CONFETTI_SPREAD, CONFETTI_SPREAD, CONFETTI_SPREAD, CONFETTI_SPEED);
		}
	}

	// --- Saving -----------------------------------------------------------------------

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Datavariant", variant());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.entityData.set(VARIANT, Mth.clamp(input.getIntOr("Datavariant", 0), 0, VARIANT_COUNT - 1));
	}

	// --- GeckoLib ---------------------------------------------------------------------

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(Animations.idleWalk(this, "idle", "walk"));
		controllers.add(Animations.actions(this, "hurt"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
