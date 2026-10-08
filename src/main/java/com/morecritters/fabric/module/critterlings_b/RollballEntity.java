package com.morecritters.fabric.module.critterlings_b;

import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.module.critterling_system.Critterling;
import java.util.List;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A critterling that curls up into a ball and uncurls again. Every 3-10 seconds (unless dancing)
 * it plays its {@code roll} animation and half a second later switches between curled
 * ({@code rollball_rolled} texture, roll sound) and uncurled (unroll sound). It keeps walking
 * either way, and looks at living things within six blocks.
 */
public class RollballEntity extends Critterling {
	private static final EntityDataAccessor<Boolean> ROLLED = SynchedEntityData.defineId(RollballEntity.class, EntityDataSerializers.BOOLEAN);

	private static final int FIRST_ROLL = 200, ROLL_MIN = 60, ROLL_MAX = 200;
	/** Ticks between the start of the roll animation and the change of shape. */
	private static final int ROLL_DELAY = 10;
	private static final String ROLL = "roll";

	/** Ticks until the next roll. */
	private int rollTimer = FIRST_ROLL;
	/** Ticks until the shape changes during a roll; 0 when not rolling. */
	private int shapeChangeIn;

	public RollballEntity(EntityType<? extends RollballEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 3.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, wanderGoal(0.7));
		this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, LivingEntity.class, 6.0F));
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(4, new FloatGoal(this));
	}

	/** As the original's spawn procedure: the first roll comes 60-200 ticks after spawning, like every later one. */
	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		this.rollTimer = Mth.nextInt(this.random, ROLL_MIN, ROLL_MAX);
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	public boolean isRolled() {
		return this.entityData.get(ROLLED);
	}

	@Override
	public String textureName() {
		return isRolled() ? super.textureName() + "_rolled" : super.textureName();
	}

	@Override
	protected List<String> extraActions() {
		return List.of(ROLL);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ROLLED, false);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!(this.level() instanceof ServerLevel)) return;
		if (--this.rollTimer <= 1) {
			this.rollTimer = Mth.nextInt(this.random, ROLL_MIN, ROLL_MAX);
			if (!isDancing()) {
				playAction(ROLL);
				this.shapeChangeIn = ROLL_DELAY;
			}
		}
		if (this.shapeChangeIn > 0 && --this.shapeChangeIn == 0) {
			changeShape();
		}
	}

	/** Curls up if uncurled, uncurls if curled. */
	private void changeShape() {
		boolean rolled = !isRolled();
		Sounds.playAt(this, rolled ? CritterlingsBModule.ROLLBALL_ROLL : CritterlingsBModule.ROLLBALL_UNROLL);
		this.entityData.set(ROLLED, rolled);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return CritterlingsBModule.ROLLBALL_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return CritterlingsBModule.ROLLBALL_HURT;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Rolled", isRolled());
		output.putInt("RollTimer", this.rollTimer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.entityData.set(ROLLED, input.getBooleanOr("Rolled", false));
		this.rollTimer = input.getIntOr("RollTimer", FIRST_ROLL);
	}
}
