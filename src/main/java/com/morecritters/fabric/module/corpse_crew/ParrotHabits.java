package com.morecritters.fabric.module.corpse_crew;

import com.geckolib.animatable.GeoEntity;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.ServerScheduler;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * How both corpse parrots, wild and tamed, get about. On the ground a parrot stays put, except
 * that every 5 to 15 seconds it crouches and hops forward into the air. In flight with a target it
 * drops a pebble on it every five seconds.
 */
final class ParrotHabits<P extends Mob & GeoEntity> {
	private static final int THROW_INTERVAL = 100;
	private static final int HOP_MIN = 100;
	private static final int HOP_MAX = 300;
	private static final int CROUCH_TICKS = 7;
	/** Ticks after the crouch during which the parrot is not held to the ground, so the hop can lift it. */
	private static final int HOP_WINDOW = CROUCH_TICKS + 3;
	private static final int PEBBLE_DELAY = 3;
	private static final double PEBBLE_DAMAGE = 2.0;
	private static final int PEBBLE_KNOCKBACK = 1;
	private static final float PEBBLE_SPEED = 1.0F;

	private final P parrot;
	private final SoundSource throwSoundSource;
	private int hopTimer;
	private int throwTimer = THROW_INTERVAL;
	private int hopping;

	ParrotHabits(P parrot, SoundSource throwSoundSource) {
		this.parrot = parrot;
		this.throwSoundSource = throwSoundSource;
		this.hopTimer = nextHopDelay();
	}

	private int nextHopDelay() {
		return Mth.nextInt(this.parrot.getRandom(), HOP_MIN, HOP_MAX);
	}

	/** One server tick of hopping, throwing and being held to the ground. */
	void tick() {
		if (--this.throwTimer <= 0) {
			this.throwTimer = THROW_INTERVAL;
			if (this.parrot.getTarget() != null && !this.parrot.onGround()) {
				throwPebble();
			}
		}
		if (--this.hopTimer <= 0) {
			this.hopTimer = nextHopDelay();
			if (this.parrot.onGround()) {
				crouchAndHop();
			}
		}
		if (this.hopping > 0) {
			this.hopping--;
		} else if (this.parrot.onGround()) {
			this.parrot.setDeltaMovement(0.0, -0.1, 0.0);
		}
	}

	private void throwPebble() {
		this.parrot.triggerAnim(Animations.ACTIONS, "throw");
		ServerScheduler.runLater(PEBBLE_DELAY, () -> {
			PebbleEntity pebble = new PebbleEntity(CorpseCrewModule.PEBBLE, this.parrot.level());
			pebble.setOwner(this.parrot);
			pebble.setBaseDamage(PEBBLE_DAMAGE);
			pebble.setKnockback(PEBBLE_KNOCKBACK);
			pebble.setSilent(true);
			pebble.setPos(this.parrot.getX(), this.parrot.getEyeY() - 0.1, this.parrot.getZ());
			Vec3 look = this.parrot.getLookAngle();
			pebble.shoot(look.x, look.y, look.z, PEBBLE_SPEED, 0.0F);
			this.parrot.level().addFreshEntity(pebble);
		});
		this.parrot.level().playSound(null, this.parrot.blockPosition(), CorpseCrewModule.PARROT_THROW, this.throwSoundSource, 1.0F, 1.0F);
	}

	private void crouchAndHop() {
		this.parrot.triggerAnim(Animations.ACTIONS, "ready");
		this.parrot.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 5, 29, false, false));
		this.hopping = HOP_WINDOW;
		ServerScheduler.runLater(CROUCH_TICKS, () -> {
			Vec3 look = this.parrot.getLookAngle();
			this.parrot.setDeltaMovement(look.x * 0.2, 0.2, look.z * 0.2);
		});
	}

	void save(ValueOutput output) {
		output.putInt("timer", this.hopTimer);
		output.putInt("throw", this.throwTimer);
	}

	void load(ValueInput input) {
		this.hopTimer = input.getIntOr("timer", this.hopTimer);
		this.throwTimer = input.getIntOr("throw", this.throwTimer);
	}
}
