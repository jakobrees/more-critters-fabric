package com.morecritters.fabric.module.shock_cube;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;

/**
 * Electrocuted: the victim crackles and hums. Whatever it hits takes the charge over (and a
 * little extra damage), and if it dies charged it bursts into a shock cube.
 */
public class ElectrocutedEffect extends MobEffect {
	private static final int COLOUR = -8716314;
	/** One tick in this many the victim sparks; one in {@link #HUM_CHANCE} it hums. */
	private static final int ZAP_CHANCE = 10;
	private static final int HUM_CHANCE = 30;

	ElectrocutedEffect() {
		super(MobEffectCategory.NEUTRAL, COLOUR);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity victim, int amplifier) {
		if (victim.getRandom().nextInt(ZAP_CHANCE) == 0) {
			int sparks = 4; // the original's (int) nextDouble(4, 5)
			level.sendParticles(ShockCubeModule.ZAP, victim.getX(), victim.getY() + 1.0, victim.getZ(), sparks, 0.5, 0.5, 0.5, 0.0);
		}
		if (victim.getRandom().nextInt(HUM_CHANCE) == 0) {
			level.playSound(null, victim.blockPosition(), ShockCubeModule.ELECTRIC_HUM_SOUND, SoundSource.AMBIENT, 3.0F, 1.0F);
		}
		return true;
	}

	static void registerEvents() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) -> {
			passChargeOn(victim, source);
			return true;
		});
		ServerLivingEntityEvents.AFTER_DEATH.register(ElectrocutedEffect::burstOnDeath);
	}

	/** An electrocuted attacker hands its charge to the one it hits, which also takes 1–3 damage. */
	private static void passChargeOn(LivingEntity victim, DamageSource source) {
		if (!(source.getEntity() instanceof LivingEntity attacker) || !(victim.level() instanceof ServerLevel level)) return;
		MobEffectInstance charge = attacker.getEffect(ShockCubeModule.ELECTROCUTED);
		if (charge == null) return;
		level.playSound(null, victim.blockPosition(), ShockCubeModule.ELECTRIC_TRANSFER_SOUND, SoundSource.PLAYERS, 3.0F, 1.0F);
		victim.addEffect(new MobEffectInstance(ShockCubeModule.ELECTROCUTED, charge.getDuration(), charge.getAmplifier(), false, false));
		attacker.removeEffect(ShockCubeModule.ELECTROCUTED);
		victim.hurtServer(level, level.damageSources().generic(), (float) Mth.nextDouble(victim.getRandom(), 1.0, 3.0));
	}

	/** Dying while electrocuted leaves a shock cube where the body fell. */
	private static void burstOnDeath(LivingEntity dead, DamageSource source) {
		if (!dead.hasEffect(ShockCubeModule.ELECTROCUTED) || !(dead.level() instanceof ServerLevel level)) return;
		ShockCubeModule.SHOCK_CUBE.spawn(level, dead.blockPosition(), EntitySpawnReason.MOB_SUMMONED);
		level.playSound(null, dead.blockPosition(), ShockCubeModule.ELECTRIC_BLAST_SOUND, SoundSource.BLOCKS, 3.0F, 1.0F);
	}
}
