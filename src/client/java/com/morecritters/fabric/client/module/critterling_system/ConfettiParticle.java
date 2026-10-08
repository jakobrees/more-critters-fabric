package com.morecritters.fabric.client.module.critterling_system;

import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * A scrap of confetti: a random colour from the sprite sheet, thrown out, falling fast and
 * spinning, the spin slowing as it falls. Values from the original's {@code ConfettiParticle}.
 */
final class ConfettiParticle extends SingleQuadParticle {
	private static final float SIZE = 0.2F, GRAVITY = 0.42F;
	private static final int LIFETIME = 25;
	private static final float SPIN = 2.0F, SPIN_CHANGE = -0.07F;

	private float spin = SPIN;

	static void register(SimpleParticleType type) {
		ParticleProviderRegistry.getInstance().register(type, sprites ->
			(options, level, x, y, z, vx, vy, vz, random) -> new ConfettiParticle(level, x, y, z, vx, vy, vz, sprites.get(random)));
	}

	private ConfettiParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
			TextureAtlasSprite sprite) {
		super(level, x, y, z, sprite);
		setSize(SIZE, SIZE);
		this.lifetime = LIFETIME;
		this.gravity = GRAVITY;
		this.hasPhysics = true;
		this.xd = vx;
		this.yd = vy;
		this.zd = vz;
	}

	@Override
	public void tick() {
		super.tick();
		this.oRoll = this.roll;
		this.roll += this.spin;
		this.spin += SPIN_CHANGE;
	}

	@Override
	protected Layer getLayer() {
		return Layer.OPAQUE;
	}
}
