package com.morecritters.fabric.module.snowflake_spider;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

/**
 * Caught in a freezing web. When the effect starts a {@link WebEntity} appears where the
 * creature stands; every tick after that the creature is held at the web, made unable to move
 * and, if it is a mob, stopped from thinking. Once the web is gone the effect ends and the
 * creature is free. (The original also hid the effect's icon with a NeoForge client hook,
 * which Fabric has no equivalent for.)
 */
public class WebbedEffect extends MobEffect {
	private static final int SLOWNESS_TICKS = 20;
	private static final int SLOWNESS_AMPLIFIER = 50;

	public WebbedEffect() {
		super(MobEffectCategory.NEUTRAL, 0x90C4E1);
	}

	/** Spins a web around the creature, every time the effect is applied. */
	@Override
	public void onEffectStarted(LivingEntity mob, int amplifier) {
		if (mob.level() instanceof ServerLevel level) {
			WebEntity web = SnowflakeSpiderModule.WEB_ENTITY.spawn(level, mob.blockPosition(), EntitySpawnReason.MOB_SUMMONED);
			if (web != null) web.setDeltaMovement(0.0, 0.0, 0.0);
		}
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
		return true;
	}

	/** Holds the creature at the nearest web; with no web left, frees it and ends the effect. */
	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplifier) {
		List<WebEntity> webs = level.getEntitiesOfClass(WebEntity.class,
			AABB.ofSize(mob.position(), WebEntity.HOLD_SIZE, WebEntity.HOLD_SIZE, WebEntity.HOLD_SIZE), e -> true);
		if (webs.isEmpty()) {
			release(mob);
			return false;
		}

		WebEntity web = webs.stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElseThrow();
		mob.teleportTo(web.getX(), web.getY(), web.getZ());
		mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOWNESS_TICKS, SLOWNESS_AMPLIFIER, false, false));
		MobEffectInstance webbed = mob.getEffect(SnowflakeSpiderModule.WEBBED);
		if (webbed != null && webbed.endsWithin(1)) {
			// Last tick: the original gave the AI back when the effect expired.
			release(mob);
		} else if (mob instanceof Mob held) {
			held.setNoAi(true);
		}
		return true;
	}

	private static void release(LivingEntity mob) {
		if (mob instanceof Mob held) held.setNoAi(false);
	}
}
