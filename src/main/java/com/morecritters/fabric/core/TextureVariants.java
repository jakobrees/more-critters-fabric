package com.morecritters.fabric.core;

/**
 * A critter whose texture depends on its state. The renderer reads
 * {@link #textureName()} each frame and loads {@code textures/entities/<name>.png}.
 */
public interface TextureVariants {
	String textureName();
}
