package com.morecritters.fabric.module.critterlings_d;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/** The piranheed, a snapping-plant critterling: strolls, fidgets and dances. Nothing else special. */
public class PiranheedEntity extends WanderingCritterling {
	private static final double MOVEMENT_SPEED = 0.4;

	public PiranheedEntity(EntityType<? extends PiranheedEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return attributes(MOVEMENT_SPEED);
	}

	@Override
	public String textureName() {
		return suffixedTexture("piranheed", rarity());
	}

	@Override
	protected SoundEvent idleSound() {
		return CritterlingsDModule.PIRANHEED_IDLE_SOUND;
	}

	@Override
	protected SoundEvent hurtSound() {
		return CritterlingsDModule.PIRANHEED_HURT_SOUND;
	}
}
