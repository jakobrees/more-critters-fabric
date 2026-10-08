package com.morecritters.fabric.client.core;

import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;

/**
 * The one particle shape the original mod uses everywhere: a flat sprite sheet animated
 * over its lifetime, with a size, gravity and speed. Modules register theirs with
 * {@link #register(SimpleParticleType, Settings)} from their client module.
 *
 * <p>The extras some of the original's particles have are switched on by chaining onto
 * {@link Settings#of}: {@code .translucentSheet()}, {@code .fullBright()} (the original's
 * {@code PARTICLE_SHEET_LIT} particles), {@code .spin(velocity, change)},
 * {@code .randomSprite()} and {@code .lifetimeSpread(spread)}.
 */
public final class SpriteParticle extends SingleQuadParticle {
	/**
	 * @param size           collision box size in blocks (MCreator "size")
	 * @param scale          visual scale multiplier (MCreator "scale")
	 * @param lifetime       ticks to live
	 * @param gravity        downward acceleration; negative floats upward
	 * @param physics        whether it collides with blocks
	 * @param speed          multiplier on the spawn velocity
	 * @param frames         number of frames in the sprite sheet
	 * @param frameTicks     ticks each frame shows; 0 keeps a single frame
	 * @param translucent    rendered on the translucent sheet instead of the opaque one
	 * @param isFullBright   glows at full brightness whatever the light around it
	 * @param spin           starting turn per tick, in radians (MCreator "angular velocity")
	 * @param spinChange     added to the turn each tick (MCreator "angular acceleration")
	 * @param isRandomSprite shows one random sprite of the set for its whole life
	 * @param lifetimeSpread the lifetime varies by up to half this either way
	 */
	public record Settings(float size, float scale, int lifetime, float gravity, boolean physics, double speed,
	                       int frames, int frameTicks, boolean translucent, boolean isFullBright,
	                       float spin, float spinChange, boolean isRandomSprite, int lifetimeSpread) {
		public static Settings of(float size, float scale, int lifetime, float gravity, boolean physics, double speed, int frames, int frameTicks) {
			return new Settings(size, scale, lifetime, gravity, physics, speed, frames, frameTicks, false, false, 0.0F, 0.0F, false, 0);
		}

		public Settings translucentSheet() {
			return new Settings(size, scale, lifetime, gravity, physics, speed, frames, frameTicks, true, isFullBright, spin, spinChange, isRandomSprite, lifetimeSpread);
		}

		public Settings fullBright() {
			return new Settings(size, scale, lifetime, gravity, physics, speed, frames, frameTicks, translucent, true, spin, spinChange, isRandomSprite, lifetimeSpread);
		}

		public Settings spin(float velocity, float change) {
			return new Settings(size, scale, lifetime, gravity, physics, speed, frames, frameTicks, translucent, isFullBright, velocity, change, isRandomSprite, lifetimeSpread);
		}

		public Settings randomSprite() {
			return new Settings(size, scale, lifetime, gravity, physics, speed, frames, frameTicks, translucent, isFullBright, spin, spinChange, true, lifetimeSpread);
		}

		public Settings lifetimeSpread(int spread) {
			return new Settings(size, scale, lifetime, gravity, physics, speed, frames, frameTicks, translucent, isFullBright, spin, spinChange, isRandomSprite, spread);
		}
	}

	public static void register(SimpleParticleType type, Settings settings) {
		ParticleProviderRegistry.getInstance().register(type, spriteSet -> provider(spriteSet, settings));
	}

	public static ParticleProvider<SimpleParticleType> provider(SpriteSet spriteSet, Settings settings) {
		return (type, level, x, y, z, vx, vy, vz, random) -> new SpriteParticle(level, x, y, z, vx, vy, vz, spriteSet, settings);
	}

	private final SpriteSet spriteSet;
	private final Settings settings;
	private float spin;

	private SpriteParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet, Settings settings) {
		super(level, x, y, z, spriteSet.first());
		this.spriteSet = spriteSet;
		this.settings = settings;
		setSize(settings.size(), settings.size());
		this.quadSize *= settings.scale();
		this.lifetime = settings.lifetime();
		if (settings.lifetimeSpread() > 0) {
			// The original's Math.max(1, lifetime + (nextInt(spread) - spread / 2)).
			this.lifetime = Math.max(1, this.lifetime + this.random.nextInt(settings.lifetimeSpread()) - settings.lifetimeSpread() / 2);
		}
		this.gravity = settings.gravity();
		this.hasPhysics = settings.physics();
		this.xd = vx * settings.speed();
		this.yd = vy * settings.speed();
		this.zd = vz * settings.speed();
		this.spin = settings.spin();
		if (settings.isRandomSprite()) {
			setSprite(spriteSet.get(this.random));
		} else {
			setSpriteFromAge(spriteSet);
		}
	}

	@Override
	protected Layer getLayer() {
		return settings.translucent() ? Layer.TRANSLUCENT : Layer.OPAQUE;
	}

	@Override
	protected int getLightCoords(float partialTick) {
		return settings.isFullBright() ? LightCoordsUtil.FULL_BRIGHT : super.getLightCoords(partialTick);
	}

	@Override
	public void tick() {
		super.tick();
		// Turns a little more each tick, the turn itself speeding up or slowing down.
		this.oRoll = this.roll;
		this.roll += this.spin;
		this.spin += settings.spinChange();
		if (!this.removed && settings.frameTicks() > 0) {
			setSprite(spriteSet.get(this.age / settings.frameTicks() % settings.frames() + 1, settings.frames()));
		}
	}
}
