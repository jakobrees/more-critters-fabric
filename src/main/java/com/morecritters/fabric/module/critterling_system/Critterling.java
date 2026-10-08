package com.morecritters.fabric.module.critterling_system;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.TextureVariants;
import com.morecritters.fabric.ids.MiscIds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * What every critterling shares: a {@link CritterlingRarity} (normal, rare, epic) that picks its
 * texture ({@code cubefrog}, {@code rare_cubefrog}, {@code epic_cubefrog}); a fidget animation
 * every 10-20 seconds while standing still; dancing while a jukebox within three blocks plays a
 * disc; and, when epic, an occasional sparkle. Critterlings never despawn, take no fall damage
 * and (unless {@link #drowns()}) do not drown. They are caught with an empty critterling sack
 * ({@link CritterlingSackItem}).
 *
 * <p>Subclasses add their goals (use {@link #wanderGoal} so they stand still while dancing),
 * attributes, sounds and anything special, and may change the animation names below.
 */
public abstract class Critterling extends PathfinderMob implements GeoEntity, TextureVariants {
	private static final EntityDataAccessor<Integer> RARITY = SynchedEntityData.defineId(Critterling.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DANCING = SynchedEntityData.defineId(Critterling.class, EntityDataSerializers.BOOLEAN);

	private static final int FIDGET_MIN = 200, FIDGET_MAX = 400;
	/** The original's starting value before the first spawn roll. */
	private static final int FIDGET_UNSET = 100;
	/** Blocks searched for a playing jukebox: from -3 to +2 on each axis, as the original's 6x6x6 loop. */
	private static final int JUKEBOX_REACH_BELOW = 3, JUKEBOX_REACH_ABOVE = 2;
	private static final int EPIC_SPARKLE_CHANCE = 100;
	private static final double STANDING_STILL = 1.0E-6;

	private static final RawAnimation DANCE = RawAnimation.begin().thenLoop("dance");
	private static final RawAnimation DANCE_EPIC = RawAnimation.begin().thenLoop("dance_epic");

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private final String textureBase;
	/** Ticks until the next fidget animation. */
	private int fidgetTimer = FIDGET_UNSET;

	protected Critterling(EntityType<? extends Critterling> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.textureBase = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
	}

	// --- rarity and dancing -----------------------------------------------------------

	public CritterlingRarity rarity() {
		return CritterlingRarity.byId(this.entityData.get(RARITY));
	}

	public void setRarity(CritterlingRarity rarity) {
		this.entityData.set(RARITY, rarity.ordinal());
	}

	/** True while a jukebox nearby plays a disc. Synced, so the client dances too. */
	public boolean isDancing() {
		return this.entityData.get(DANCING);
	}

	@Override
	public String textureName() {
		return rarity().texture(this.textureBase);
	}

	// --- what subclasses may change ---------------------------------------------------

	/** One-shot fidgets picked at random while standing still; empty for none. */
	protected List<String> fidgetAnimations() {
		return List.of("idle1", "idle2", "idle3");
	}

	/** Further one-shot animations the subclass plays with {@link #playAction}. */
	protected List<String> extraActions() {
		return List.of();
	}

	protected String idleAnimation() {
		return "idle";
	}

	/** The loop while moving; null if the critterling keeps its idle loop. */
	protected @Nullable String walkAnimation() {
		return "walk";
	}

	/** Most critterlings cannot drown; the ones that can override this. */
	protected boolean drowns() {
		return false;
	}

	/** Plays a one-shot animation named in {@link #fidgetAnimations} or {@link #extraActions}. */
	protected void playAction(String name) {
		this.triggerAnim(Animations.ACTIONS, name);
	}

	/** A random stroll that pauses while the critterling dances. */
	protected RandomStrollGoal wanderGoal(double speed) {
		return new RandomStrollGoal(this, speed) {
			@Override
			public boolean canUse() {
				return !isDancing() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return !isDancing() && super.canContinueToUse();
			}
		};
	}

	// --- lifecycle --------------------------------------------------------------------

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(RARITY, CritterlingRarity.NORMAL.ordinal());
		builder.define(DANCING, false);
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		this.fidgetTimer = Mth.nextInt(this.random, FIDGET_MIN, FIDGET_MAX);
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!(this.level() instanceof ServerLevel level)) return;
		this.entityData.set(DANCING, jukeboxPlayingNearby(level));
		fidget();
		if (rarity() == CritterlingRarity.EPIC && this.random.nextInt(EPIC_SPARKLE_CHANCE) == 0) {
			sparkle(level);
		}
	}

	/** Now and then, when standing still, stops and plays one of its fidget animations. */
	private void fidget() {
		if (--this.fidgetTimer > 1) return;
		this.fidgetTimer = Mth.nextInt(this.random, FIDGET_MIN, FIDGET_MAX);
		List<String> fidgets = fidgetAnimations();
		if (fidgets.isEmpty() || isDancing() || this.getDeltaMovement().horizontalDistanceSqr() > STANDING_STILL) return;
		this.getNavigation().stop();
		playAction(fidgets.get(this.random.nextInt(fidgets.size())));
	}

	private boolean jukeboxPlayingNearby(ServerLevel level) {
		BlockPos centre = this.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-JUKEBOX_REACH_BELOW, -JUKEBOX_REACH_BELOW, -JUKEBOX_REACH_BELOW),
				centre.offset(JUKEBOX_REACH_ABOVE, JUKEBOX_REACH_ABOVE, JUKEBOX_REACH_ABOVE))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(Blocks.JUKEBOX) && state.getValue(JukeboxBlock.HAS_RECORD)) return true;
		}
		return false;
	}

	/** The misc module's {@code epic_particle}; skipped while that module is not there. */
	private void sparkle(ServerLevel level) {
		if (BuiltInRegistries.PARTICLE_TYPE.getValue(MiscIds.Particles.EPIC_PARTICLE) instanceof ParticleOptions particle) {
			Sparkle.at(level, particle, this.position());
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypes.FALL) || !drowns() && source.is(DamageTypes.DROWN)) return false;
		return super.hurtServer(level, source, damage);
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Variant", rarity().ordinal());
		output.putInt("FidgetTimer", this.fidgetTimer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setRarity(CritterlingRarity.byId(input.getIntOr("Variant", 0)));
		this.fidgetTimer = input.getIntOr("FidgetTimer", FIDGET_UNSET);
	}

	// --- GeckoLib ---------------------------------------------------------------------

	/**
	 * A loop controller (dance while dancing, else walk while moving, else idle) and the
	 * {@link Animations#ACTIONS} controller for the one-shot animations. Subclasses adding
	 * controllers call {@code super.registerControllers} first.
	 */
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		RawAnimation idle = RawAnimation.begin().thenLoop(idleAnimation());
		String walkName = walkAnimation();
		RawAnimation walk = walkName == null ? idle : RawAnimation.begin().thenLoop(walkName);
		controllers.add(new AnimationController<Critterling>(Animations.MOVEMENT, 2, test -> {
			if (isDancing()) return test.setAndContinue(rarity() == CritterlingRarity.EPIC ? DANCE_EPIC : DANCE);
			return test.setAndContinue(test.isMoving() ? walk : idle);
		}));
		List<String> actions = new ArrayList<>(fidgetAnimations());
		actions.addAll(extraActions());
		controllers.add(Animations.actions(this, actions.toArray(String[]::new)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
