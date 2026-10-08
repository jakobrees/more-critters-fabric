package com.morecritters.fabric.module.bomb_jelly;

import com.morecritters.fabric.core.Config;

/** The three bomb jellies differ only in these numbers, the texture set and their bucket. */
public enum BombJellySize {
	SMALL("bomb_jelly", 1, 3, "small_bomb_jelly_explosion_power", 2.0, 1.0F),
	MEDIUM("bomb_jelly_medium", 2, 5, "medium_bomb_jelly_explosion_power", 3.0, 2.0F),
	LARGE("bomb_jelly_large", 3, 7, "large_bomb_jelly_explosion_power", 4.0, 3.0F);

	/** Textures are {@code <texturePrefix>1} to {@code <texturePrefix>5}; the frames loop. */
	public static final int TEXTURE_FRAMES = 5;

	final String texturePrefix;
	final int experience;
	/** Ticks between two texture frames when {@code animate_bomb_jelly} is on. */
	final int ticksPerFrame;
	private final String powerKey;
	private final double defaultPower;
	/** The explosion of a jelly lit with flint and steel is fixed, not configured. */
	final float ignitedPower;

	BombJellySize(String texturePrefix, int experience, int ticksPerFrame, String powerKey, double defaultPower, float ignitedPower) {
		this.texturePrefix = texturePrefix;
		this.experience = experience;
		this.ticksPerFrame = ticksPerFrame;
		this.powerKey = powerKey;
		this.defaultPower = defaultPower;
		this.ignitedPower = ignitedPower;
	}

	/** Power of the explosion when something swims into the jelly. */
	float contactPower() {
		return (float) Config.number(this.powerKey, this.defaultPower);
	}
}
