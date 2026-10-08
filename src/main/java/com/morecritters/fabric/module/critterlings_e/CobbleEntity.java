package com.morecritters.fabric.module.critterlings_e;

import com.morecritters.fabric.module.critterling_system.Critterling;
import com.morecritters.fabric.module.critterling_system.CritterlingRarity;
import java.util.List;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The cobble: a little pebble critterling that never moves. It turns to face a random way when it
 * first appears and then sits still; an epic cobble slowly spins on the spot while a jukebox
 * nearby plays a disc.
 */
public class CobbleEntity extends Critterling {
	private static final float SPIN_PER_TICK = 1.0F;

	/** Whether it has turned to its random facing yet. */
	private boolean rotated;

	public CobbleEntity(EntityType<? extends CobbleEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.0)
			.add(Attributes.MAX_HEALTH, 3.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	// No goals: it just sits there.

	@Override
	protected List<String> fidgetAnimations() {
		return List.of();
	}

	@Override
	protected String idleAnimation() {
		return "idle0";
	}

	@Override
	protected @Nullable String walkAnimation() {
		return null;
	}

	/** The cobble has no dance animation (the epic one spins instead), so it never shows dancing. */
	@Override
	public boolean isDancing() {
		return false;
	}

	/** {@code cobble}, {@code cobble_rare}, {@code cobble_epic}. */
	@Override
	public String textureName() {
		return switch (rarity()) {
			case NORMAL -> "cobble";
			case RARE -> "cobble_rare";
			case EPIC -> "cobble_epic";
		};
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level().isClientSide()) return;
		if (rarity() == CritterlingRarity.EPIC && super.isDancing()) face(this.getYRot() + SPIN_PER_TICK);
		if (!this.rotated) {
			this.rotated = true;
			face(Mth.nextInt(this.random, -180, 180));
		}
	}

	private void face(float yaw) {
		this.setYRot(yaw);
		this.setXRot(0.0F);
		this.setYBodyRot(yaw);
		this.setYHeadRot(yaw);
		this.yRotO = yaw;
		this.xRotO = 0.0F;
		this.yBodyRotO = yaw;
		this.yHeadRotO = yaw;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return CritterlingsEModule.COBBLE_HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return CritterlingsEModule.COBBLE_HURT_SOUND;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Rotated", this.rotated);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.rotated = input.getBooleanOr("Rotated", false);
	}
}
