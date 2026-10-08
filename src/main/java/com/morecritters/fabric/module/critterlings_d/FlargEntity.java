package com.morecritters.fabric.module.critterlings_d;

import com.morecritters.fabric.core.ServerScheduler;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The flarg, a little fire critterling. Besides strolling, fidgeting and dancing like every
 * critterling, it flickers: about every fifth tick a flame pops up from its head, an ordinary
 * flame when normal, a blue soul flame when rare and its own {@code flarg_flame} when epic. It
 * holds the flame in while dancing or fidgeting.
 */
public class FlargEntity extends WanderingCritterling {
	private static final double MOVEMENT_SPEED = 0.3;
	private static final int FLAME_CHANCE = 5;
	/** The flame appears this many ticks after it is rolled, where the flarg stood then. */
	private static final int FLAME_DELAY = 3;
	private static final double FLAME_HEIGHT = 0.8;
	/** How long the original kept a fidget animation set before clearing it. */
	private static final int FIDGET_TICKS = 45;

	/** Game tick until which the current fidget plays. */
	private int fidgetingUntil;

	public FlargEntity(EntityType<? extends FlargEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return attributes(MOVEMENT_SPEED);
	}

	@Override
	public String textureName() {
		return suffixedTexture("flarg", rarity());
	}

	@Override
	protected SoundEvent idleSound() {
		return CritterlingsDModule.FLARG_IDLE_SOUND;
	}

	@Override
	protected SoundEvent hurtSound() {
		return CritterlingsDModule.FLARG_HURT_SOUND;
	}

	@Override
	protected void playAction(String name) {
		super.playAction(name);
		this.fidgetingUntil = this.tickCount + FIDGET_TICKS;
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level() instanceof ServerLevel level && this.random.nextInt(FLAME_CHANCE) == 0
				&& !isDancing() && this.tickCount >= this.fidgetingUntil) {
			flicker(level);
		}
	}

	private void flicker(ServerLevel level) {
		ParticleOptions flame = switch (rarity()) {
			case NORMAL -> ParticleTypes.FLAME;
			case RARE -> ParticleTypes.SOUL_FIRE_FLAME;
			case EPIC -> CritterlingsDModule.FLARG_FLAME;
		};
		Vec3 at = this.position().add(0.0, FLAME_HEIGHT, 0.0);
		ServerScheduler.runLater(FLAME_DELAY, () -> level.sendParticles(flame, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0));
	}
}
