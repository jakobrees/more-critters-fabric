package com.morecritters.fabric.module.snowflake_spider;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import com.morecritters.fabric.ids.BalloonRatIds;
import com.morecritters.fabric.ids.StincarpIds;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Brittle bones: every hit the victim takes also knocks one to three extra health straight off,
 * with a crunch and a little cracked heart. Balloon rats are immune. Suffering it together with
 * the other four poisons earns the "get all poisons" advancement.
 */
final class BrittlenessEffect extends MobEffect {
	private static final int COLOUR = -10462905;
	private static final float EXTRA_DAMAGE_MIN = 1.0F, EXTRA_DAMAGE_MAX = 3.0F;
	private static final int HEARTS_MIN = 1, HEARTS_MAX = 2;
	private static final double HEART_SPEED = 0.02;

	/** The other poisons of the set, owned by the stincarp and balloon rat modules. */
	private static final List<Identifier> OTHER_POISONS = List.of(
		StincarpIds.Effects.ASPHYXIATION,
		BalloonRatIds.Effects.HALLUCINAZIUM,
		BalloonRatIds.Effects.MUSCLE_ACHE,
		BalloonRatIds.Effects.STAGNATION);

	BrittlenessEffect() {
		super(MobEffectCategory.HARMFUL, COLOUR);
	}

	/** Hooks the extra damage into every hit; called once from the module. */
	static void registerDamageHook() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(BrittlenessEffect::onIncomingDamage);
	}

	private static boolean onIncomingDamage(LivingEntity victim, DamageSource source, float amount) {
		if (victim.hasEffect(SnowflakeSpiderModule.BRITTLENESS) && !isBalloonRat(victim) && victim.level() instanceof ServerLevel level) {
			victim.setHealth(victim.getHealth() - Mth.nextFloat(level.getRandom(), EXTRA_DAMAGE_MIN, EXTRA_DAMAGE_MAX));
			level.playSound(null, victim.blockPosition(), SnowflakeSpiderModule.EXTENSIVE_HURT_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
			int hearts = Mth.nextInt(level.getRandom(), HEARTS_MIN, HEARTS_MAX);
			for (int i = 0; i < hearts; i++) {
				level.sendParticles(SnowflakeSpiderModule.BRITTLE_HEART, true, true,
					victim.getX(), victim.getY() + victim.getBbHeight(), victim.getZ(), 1, 0, 0, 0, HEART_SPEED);
			}
		}
		return true;
	}

	private static boolean isBalloonRat(LivingEntity entity) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(BalloonRatIds.Entities.BALLOON_RAT);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity victim, int amplification) {
		if (victim instanceof ServerPlayer player && hasAllOtherPoisons(player)) {
			Advancements.award(player, MoreCritters.id("get_all_poisons"));
		}
		return true;
	}

	private static boolean hasAllOtherPoisons(LivingEntity entity) {
		return OTHER_POISONS.stream().allMatch(id -> BuiltInRegistries.MOB_EFFECT.get(id)
			.map(entity::hasEffect)
			.orElse(false));
	}
}
