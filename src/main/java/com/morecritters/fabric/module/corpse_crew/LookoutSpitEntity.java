package com.morecritters.fabric.module.corpse_crew;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;

/** A gob of sea water the lookout spits. Invisible itself; it drips water as it flies and splats on a hit. */
public class LookoutSpitEntity extends CrewProjectile {
	private static final BlockParticleOption WATER = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.WATER.defaultBlockState());

	public LookoutSpitEntity(EntityType<? extends LookoutSpitEntity> type, Level level) {
		super(type, level);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.FALLING_WATER, this.getX(), this.getY(), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
			level.sendParticles(WATER, this.getX(), this.getY(), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (!this.level().isClientSide()) {
			this.level().playSound(null, this.blockPosition(), CorpseCrewModule.LOOKOUT_SPIT_HITS, SoundSource.NEUTRAL, 1.0F, 1.0F);
		}
	}

	@Override
	public ItemStack getItem() {
		return ItemStack.EMPTY;
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		return ItemStack.EMPTY;
	}
}
