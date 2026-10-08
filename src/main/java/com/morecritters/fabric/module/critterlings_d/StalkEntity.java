package com.morecritters.fabric.module.critterlings_d;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * The stalk, an eye-stalk critterling: strolls, fidgets, dances and chirps. Its glowing parts
 * are a client render layer ({@code stalk_glow}).
 */
public class StalkEntity extends WanderingCritterling {
	private static final double MOVEMENT_SPEED = 0.3;

	public StalkEntity(EntityType<? extends StalkEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return attributes(MOVEMENT_SPEED);
	}

	@Override
	protected SoundEvent idleSound() {
		return CritterlingsDModule.STALK_IDLE_SOUND;
	}

	@Override
	protected SoundEvent hurtSound() {
		return CritterlingsDModule.STALK_HURT_SOUND;
	}
}
