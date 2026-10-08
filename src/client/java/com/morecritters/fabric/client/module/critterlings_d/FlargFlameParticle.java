package com.morecritters.fabric.client.module.critterlings_d;

import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;

/**
 * The epic flarg's flame: an eight-frame flicker, one frame per tick, that glows at full
 * brightness whatever the light around it. Values from the original's {@code FlargFlameParticle}.
 */
final class FlargFlameParticle extends SingleQuadParticle {
	private static final float SIZE = 0.2F;
	private static final int LIFETIME = 7, FRAMES = 8;

	private final SpriteSet sprites;

	static void register(SimpleParticleType type) {
		ParticleProviderRegistry.getInstance().register(type, sprites ->
			(options, level, x, y, z, vx, vy, vz, random) -> new FlargFlameParticle(level, x, y, z, vx, vy, vz, sprites));
	}

	private FlargFlameParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
		super(level, x, y, z, sprites.first());
		this.sprites = sprites;
		setSize(SIZE, SIZE);
		this.lifetime = LIFETIME;
		this.gravity = 0.0F;
		this.hasPhysics = true;
		this.xd = vx;
		this.yd = vy;
		this.zd = vz;
		setSpriteFromAge(sprites);
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.removed) {
			setSprite(this.sprites.get(this.age % FRAMES + 1, FRAMES));
		}
	}

	@Override
	protected int getLightCoords(float partialTick) {
		return LightCoordsUtil.FULL_BRIGHT;
	}

	@Override
	protected Layer getLayer() {
		return Layer.OPAQUE;
	}
}
