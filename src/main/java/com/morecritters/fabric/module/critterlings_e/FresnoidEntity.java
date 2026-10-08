package com.morecritters.fabric.module.critterlings_e;

import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.module.critterling_system.Critterling;
import java.util.List;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.level.Level;

/**
 * The fresnoid: a sleepy critterling that wanders about and, when it fidgets, yawns (one time in
 * a hundred a goofy yawn), its mouth wide open for a moment.
 */
public class FresnoidEntity extends Critterling {
	private static final EntityDataAccessor<Boolean> YAWNING = SynchedEntityData.defineId(FresnoidEntity.class, EntityDataSerializers.BOOLEAN);

	private static final String YAWN_ANIMATION = "idle1";
	private static final int GOOFY_YAWN_CHANCE = 100;
	/** Ticks into the yawn animation when the mouth opens, and when it closes again. */
	private static final int MOUTH_OPENS = 13, MOUTH_CLOSES = MOUTH_OPENS + 28;

	public FresnoidEntity(EntityType<? extends FresnoidEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.4)
			.add(Attributes.MAX_HEALTH, 3.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, wanderGoal(0.6));
		this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(3, new FloatGoal(this));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(YAWNING, false);
	}

	/** Its only fidget is the yawn. */
	@Override
	protected List<String> fidgetAnimations() {
		return List.of(YAWN_ANIMATION);
	}

	@Override
	protected void playAction(String name) {
		super.playAction(name);
		if (name.equals(YAWN_ANIMATION)) yawn();
	}

	private void yawn() {
		SoundEvent sound = this.random.nextInt(GOOFY_YAWN_CHANCE) == 0 ? CritterlingsEModule.FRESNOID_YAWN_GOOFY_SOUND : CritterlingsEModule.FRESNOID_YAWN_SOUND;
		Sounds.playAt(this, sound, SoundSource.NEUTRAL, 1.0F, 1.0F);
		ServerScheduler.runLater(MOUTH_OPENS, () -> this.entityData.set(YAWNING, true));
		ServerScheduler.runLater(MOUTH_CLOSES, () -> this.entityData.set(YAWNING, false));
	}

	/** {@code fresnoid}, {@code fresnoid_rare}, {@code fresnoid_epic}, each with a {@code _yawn} form. */
	@Override
	public String textureName() {
		String name = switch (rarity()) {
			case NORMAL -> "fresnoid";
			case RARE -> "fresnoid_rare";
			case EPIC -> "fresnoid_epic";
		};
		return this.entityData.get(YAWNING) ? name + "_yawn" : name;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return CritterlingsEModule.FRESNOID_IDLE_SOUND;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return CritterlingsEModule.FRESNOID_HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return CritterlingsEModule.FRESNOID_HURT_SOUND;
	}
}
