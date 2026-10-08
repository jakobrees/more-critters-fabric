package com.morecritters.fabric.module.critterlings_d;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/** The mangotrice, a mango-bird critterling: strolls, fidgets, dances and sings. Nothing else special. */
public class MangotriceEntity extends WanderingCritterling {
	private static final double MOVEMENT_SPEED = 0.4;

	public MangotriceEntity(EntityType<? extends MangotriceEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return attributes(MOVEMENT_SPEED);
	}

	@Override
	public String textureName() {
		return suffixedTexture("mangotrice", rarity());
	}

	@Override
	protected SoundEvent idleSound() {
		return CritterlingsDModule.MANGOTRICE_IDLE_SOUND;
	}

	@Override
	protected SoundEvent hurtSound() {
		return CritterlingsDModule.MANGOTRICE_HURT_SOUND;
	}
}
