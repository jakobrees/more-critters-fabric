package com.morecritters.fabric.module.nightshroom;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Particle bursts in the shape of the original's {@code /particle ... force} commands: seen from
 * far away and regardless of the particle setting.
 */
final class Bursts {
	static void forced(ServerLevel level, ParticleOptions particle, Vec3 at, int count, double spreadX, double spreadY, double spreadZ, double speed) {
		level.sendParticles(particle, true, false, at.x, at.y, at.z, count, spreadX, spreadY, spreadZ, speed);
	}

	static void rot(ServerLevel level, Vec3 at, int count, double spreadX, double spreadY, double spreadZ, double speed) {
		forced(level, NightshroomModule.ROT_PARTICLE, at, count, spreadX, spreadY, spreadZ, speed);
	}

	/** Bits of mycelium kicked up where rot lands or roots break through. */
	static void mycelium(ServerLevel level, Vec3 at, int count, double spreadX, double spreadY, double spreadZ, double speed) {
		forced(level, block(Blocks.MYCELIUM), at, count, spreadX, spreadY, spreadZ, speed);
	}

	static BlockParticleOption block(Block block) {
		return new BlockParticleOption(ParticleTypes.BLOCK, block.defaultBlockState());
	}

	static ItemParticleOption item(Item item) {
		return new ItemParticleOption(ParticleTypes.ITEM, item);
	}

	private Bursts() {}
}
