package com.morecritters.fabric.module.gravedigger;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.ids.MiscIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A skittish underground scavenger. It shies away from players and growls at any it can see;
 * every five seconds, if a player is watching, it burrows into the ground and two seconds later
 * digs up something buried: usually a zombie, skeleton or spider, sometimes an Amalgam, and
 * very rarely a zombie clutching a music disc. Takes no fall damage.
 */
public class GravediggerEntity extends PathfinderMob implements GeoEntity {
	private static final int DIG_INTERVAL = 100;
	private static final int DIG_DOWN_TICKS = 40;
	private static final int RESET_DELAY = 30;
	private static final int GROWL_MIN = 100, GROWL_MAX = 200;
	private static final double NOTICE_RANGE = 12.0;
	/** Biomes of Alex's Caves, where the gravedigger never spawns. */
	private static final TagKey<Biome> ALEXS_CAVES_BIOMES = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("alexscaves", "alexs_caves_biomes"));
	private static final ResourceKey<Biome> THE_VOID = ResourceKey.create(Registries.BIOME, Identifier.withDefaultNamespace("the_void"));

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	/** Ticks until it next checks for a watching player and digs. */
	private int digCooldown = DIG_INTERVAL;
	/** Ticks left of burrowing down; zero when not digging. */
	private int digDownTimer;
	/** Ticks until it next growls at a player it can see. */
	private int growlTimer = GROWL_MAX;

	public GravediggerEntity(EntityType<? extends GravediggerEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 10.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	/** Natural spawns: deep in the Overworld (y 20 or lower), out of sight of the sky. */
	public static boolean canSpawnAt(EntityType<GravediggerEntity> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return level.getLevel().dimension() == Level.OVERWORLD
			&& pos.getY() <= 20
			&& !level.canSeeSkyFromBelowWater(pos)
			&& !level.getBiome(pos).is(THE_VOID)
			&& !level.getBiome(pos).is(ALEXS_CAVES_BIOMES);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 12.0F, 1.2, 1.2) {
			@Override public boolean canUse() { return !isDigging() && super.canUse(); }
			@Override public boolean canContinueToUse() { return !isDigging() && super.canContinueToUse(); }
		});
		this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0) {
			@Override public boolean canUse() { return !isDigging() && super.canUse(); }
			@Override public boolean canContinueToUse() { return !isDigging() && super.canContinueToUse(); }
		});
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(4, new FloatGoal(this));
	}

	public boolean isDigging() {
		return this.digDownTimer > 0;
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		this.growlTimer = Mth.nextInt(this.random, GROWL_MIN, GROWL_MAX);
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!(this.level() instanceof ServerLevel level)) return;
		if (--this.growlTimer <= 0) {
			this.growlTimer = Mth.nextInt(this.random, GROWL_MIN, GROWL_MAX);
			if (seesWatchingPlayer(level)) Sounds.playAt(this, GravediggerModule.GROWL_SOUND);
		}
		if (--this.digCooldown <= 0) {
			this.digCooldown = DIG_INTERVAL;
			if (this.onGround() && seesWatchingPlayer(level)) startDigging();
		}
		if (isDigging()) {
			burrow(level);
			if (--this.digDownTimer == 0) digUp(level);
		}
	}

	/** A survival or adventure player within twelve blocks and in line of sight. */
	private boolean seesWatchingPlayer(ServerLevel level) {
		return !level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(NOTICE_RANGE),
			player -> !player.isSpectator() && !player.isCreative() && this.hasLineOfSight(player)).isEmpty();
	}

	private void startDigging() {
		this.digDownTimer = DIG_DOWN_TICKS;
		this.triggerAnim(Animations.ACTIONS, "dig_down");
		Sounds.playAt(this, GravediggerModule.DIG_DOWN_SOUND);
	}

	/** Holds still, pressed into the ground, kicking up bits of the block beneath. */
	private void burrow(ServerLevel level) {
		// The flee goal does not clear its path when it stops, so drop the path or it keeps walking.
		this.getNavigation().stop();
		this.setDeltaMovement(0.0, -2.0, 0.0);
		BlockParticleOption dirt = new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(this.blockPosition().below()));
		level.sendParticles(dirt, true, false, this.getX(), this.getY(), this.getZ(), 3, 0.2, 0.0, 0.2, 1.0);
	}

	/** Surfaces and throws whatever it found up out of the ground. */
	private void digUp(ServerLevel level) {
		this.triggerAnim(Animations.ACTIONS, "dig_up");
		Sounds.playAt(this, GravediggerModule.DIG_UP_SOUND);
		unearth(level);
		ServerScheduler.runLater(RESET_DELAY, () -> {
			if (!this.isRemoved()) this.triggerAnim(Animations.ACTIONS, "reset");
		});
	}

	/**
	 * One in five digs is a rare find: an Amalgam (1/3), otherwise a zombie villager or an enderman.
	 * The rest are a zombie (1 in 50 of those holding the Waddle disc), skeleton, spider or cave spider.
	 */
	private void unearth(ServerLevel level) {
		BlockPos below = this.blockPosition().below();
		if (Mth.nextInt(this.random, 1, 5) == 1) {
			if (Mth.nextInt(this.random, 1, 3) == 1) {
				launch(GravediggerModule.AMALGAM.spawn(level, below, EntitySpawnReason.MOB_SUMMONED), 0.7);
			} else {
				EntityType<?> type = Mth.nextInt(this.random, 1, 2) == 1 ? EntityTypes.ZOMBIE_VILLAGER : EntityTypes.ENDERMAN;
				launch(type.spawn(level, below, EntitySpawnReason.MOB_SUMMONED), 0.7);
			}
			return;
		}
		switch (Mth.nextInt(this.random, 1, 4)) {
			case 1 -> {
				Mob zombie = EntityTypes.ZOMBIE.spawn(level, below, EntitySpawnReason.MOB_SUMMONED);
				if (zombie != null && Mth.nextInt(this.random, 1, 50) == 1) giveWaddleDisc(zombie);
				launch(zombie, 0.7);
			}
			case 2 -> launch(EntityTypes.SKELETON.spawn(level, below, EntitySpawnReason.MOB_SUMMONED), 0.7);
			case 3 -> launch(EntityTypes.SPIDER.spawn(level, this.blockPosition(), EntitySpawnReason.MOB_SUMMONED), 0.5);
			default -> launch(EntityTypes.CAVE_SPIDER.spawn(level, this.blockPosition(), EntitySpawnReason.MOB_SUMMONED), 0.5);
		}
	}

	private static void giveWaddleDisc(Mob zombie) {
		zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(BuiltInRegistries.ITEM.getValue(MiscIds.Items.MUSIC_DISC_WADDLE)));
		zombie.setDropChance(EquipmentSlot.MAINHAND, 1.0F);
	}

	private static void launch(@Nullable Entity entity, double upward) {
		if (entity != null) entity.setDeltaMovement(0.0, upward, 0.0);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypes.FALL)) return false;
		return super.hurtServer(level, source, damage);
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		this.spawnAtLocation(level, new ItemStack(GravediggerModule.APPENDAGE));
	}

	@Override protected SoundEvent getAmbientSound() { return GravediggerModule.SNIFF_SOUND; }
	@Override protected SoundEvent getHurtSound(DamageSource source) { return GravediggerModule.HURT_SOUND; }
	@Override protected SoundEvent getDeathSound() { return GravediggerModule.DEATH_SOUND; }

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("DigCooldown", this.digCooldown);
		output.putInt("DigDownTimer", this.digDownTimer);
		output.putInt("GrowlTimer", this.growlTimer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.digCooldown = input.getIntOr("DigCooldown", DIG_INTERVAL);
		this.digDownTimer = input.getIntOr("DigDownTimer", 0);
		this.growlTimer = input.getIntOr("GrowlTimer", GROWL_MAX);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(Animations.idleWalk(this, "idle", "walk"));
		controllers.add(Animations.actions(this, "dig_down", "dig_up", "reset"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
