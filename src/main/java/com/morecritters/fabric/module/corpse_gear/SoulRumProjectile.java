package com.morecritters.fabric.module.corpse_gear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A bottle of soul rum. Wherever it breaks, on a creature or a block, a corpse mate, quartermaster or tank rises
 * from a puff of cloud and pink swirls. It sinks harmlessly in water. Nothing throws it; it is a creative toy.
 */
public class SoulRumProjectile extends GearProjectile {
	private static final int KNOCKBACK = 1;
	private static final int SWIRLS = 20;
	private static final ColorParticleOption PINK_SWIRL = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.91F, 0.57F, 0.75F);

	public SoulRumProjectile(EntityType<? extends SoulRumProjectile> type, Level level) {
		super(type, level);
	}

	@Override
	protected Item item() {
		return CorpseGearModule.SOUL_RUM;
	}

	@Override
	protected int knockback() {
		return KNOCKBACK;
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide() && !this.isRemoved() && this.isInWaterBlock()) this.discard();
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (this.level() instanceof ServerLevel level) raiseCrewMember(level, this.position());
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (this.level() instanceof ServerLevel level) raiseCrewMember(level, cornerOf(hit.getBlockPos()));
	}

	/** One of mate, quartermaster or tank appears a block above the spot (half a block over, as in the original). */
	private void raiseCrewMember(ServerLevel level, Vec3 spot) {
		double x = spot.x + 0.5, y = spot.y + 1.0, z = spot.z + 0.5;
		level.sendParticles(ParticleTypes.CLOUD, true, false, x, y, z, 10, 0.5, 0.0, 0.5, 0.02);
		for (int i = 0; i < SWIRLS; i++) {
			// Count 0: the "spread" is the swirl's motion, as the original's /particle command had it.
			level.sendParticles(PINK_SWIRL, x + Mth.nextDouble(this.random, -1.0, 1.0), y + Mth.nextDouble(this.random, 1.0, 2.0),
				z + Mth.nextDouble(this.random, -1.0, 1.0), 0, 0.91, 0.57, 0.75, 1.0);
		}
		Crew.type(Crew.RAISED_BY_SOUL_RUM[this.random.nextInt(Crew.RAISED_BY_SOUL_RUM.length)]).ifPresent(type -> {
			Entity member = type.spawn(level, BlockPos.containing(x, y, z), EntitySpawnReason.MOB_SUMMONED);
			if (member != null) member.setDeltaMovement(Vec3.ZERO);
		});
		level.playSound(null, BlockPos.containing(spot), CorpseGearModule.SOUL_RUM_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
	}
}
