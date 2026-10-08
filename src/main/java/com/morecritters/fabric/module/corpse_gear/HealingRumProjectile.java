package com.morecritters.fabric.module.corpse_gear;

import com.morecritters.fabric.ids.MiscIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A bottle of healing rum the quartermaster drops among its crew. It breaks as soon as it is near a corpse mate
 * or quartermaster, healing every crew member within six blocks by 5-10; one that reaches the ground instead
 * shatters into a short-lived regeneration cloud. It sinks harmlessly in water.
 */
public class HealingRumProjectile extends GearProjectile {
	/** The box around the bottle in which a mate or quartermaster catches it. */
	private static final double CATCH_SIZE = 2.0;
	private static final double HEAL_RADIUS = 6.0;
	private static final double HEAL_MIN = 5.0;
	private static final double HEAL_MAX = 10.0;
	/** The cloud: radius 2, shrinking by 0.033 a tick, gone after 60 ticks. */
	private static final float CLOUD_RADIUS = 2.0F;
	private static final float CLOUD_RADIUS_PER_TICK = -0.033F;
	private static final int CLOUD_TICKS = 60;
	private static final int WHITE = -1;

	public HealingRumProjectile(EntityType<? extends HealingRumProjectile> type, Level level) {
		super(type, level);
	}

	@Override
	protected Item item() {
		return CorpseGearModule.HEALING_RUM;
	}

	/** Runs on the landing tick too, so a bottle dropped at a quartermaster's feet still heals the crew. */
	@Override
	protected void flightTick() {
		if (!(this.level() instanceof ServerLevel level)) return;
		if (isNearRumCarrier(level)) {
			breakAmongCrew(level);
		} else if (this.isInWaterBlock()) {
			this.discard();
		}
	}

	private boolean isNearRumCarrier(ServerLevel level) {
		AABB box = AABB.ofSize(this.position(), CATCH_SIZE, CATCH_SIZE, CATCH_SIZE);
		return !level.getEntities(this, box, entity -> Crew.isOneOf(entity, Crew.RUM_CARRIERS)).isEmpty();
	}

	/** Breaks over the crew: sound, sparkles and glass, and every crew member nearby heals 5-10. */
	private void breakAmongCrew(ServerLevel level) {
		Vec3 spot = this.position();
		level.playSound(null, BlockPos.containing(spot), CorpseGearModule.HEALING_RUM_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
		shatterParticles(level, spot);
		AABB around = new AABB(spot, spot).inflate(HEAL_RADIUS);
		for (LivingEntity member : level.getEntitiesOfClass(LivingEntity.class, around, entity -> Crew.isOneOf(entity, Crew.HEALED_BY_RUM))) {
			if (member.isAlive()) member.setHealth(member.getHealth() + (float) Mth.nextDouble(this.random, HEAL_MIN, HEAL_MAX));
		}
		this.discard();
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (!(this.level() instanceof ServerLevel level)) return;
		Vec3 spot = cornerOf(hit.getBlockPos());
		shatterParticles(level, spot);
		level.playSound(null, hit.getBlockPos(), CorpseGearModule.HEALING_RUM_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
		spawnRegenerationCloud(level, spot);
	}

	/** Healing pluses and bits of glass bottle a block above the spot, offset half a block as in the original. */
	private static void shatterParticles(ServerLevel level, Vec3 spot) {
		double x = spot.x + 0.5, y = spot.y + 1.0, z = spot.z + 0.5;
		if (BuiltInRegistries.PARTICLE_TYPE.getValue(MiscIds.Particles.HEAL_PLUS) instanceof ParticleOptions healPlus) {
			level.sendParticles(healPlus, x, y, z, 6, 0.5, 0.5, 0.5, 0.01);
		}
		level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.GLASS_BOTTLE), x, y, z, 4, 0.2, 0.2, 0.2, 0.01);
	}

	private static void spawnRegenerationCloud(ServerLevel level, Vec3 spot) {
		AreaEffectCloud cloud = new AreaEffectCloud(level, spot.x + 0.5, spot.y + 1.0, spot.z + 0.5);
		cloud.setRadius(CLOUD_RADIUS);
		cloud.setRadiusPerTick(CLOUD_RADIUS_PER_TICK);
		cloud.setDuration(CLOUD_TICKS);
		cloud.setPotionContents(new PotionContents(Potions.REGENERATION));
		cloud.setCustomParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, WHITE));
		level.addFreshEntity(cloud);
	}
}
