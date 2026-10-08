package com.morecritters.fabric.module.armossillo;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.ids.ArmossilloIds;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * The armossillo: a mossy armadillo of lush caves whose shells make sturdy armour and a biting
 * shield, plus the glowing ooze it sneezes and what is made from it.
 */
public final class ArmossilloModule implements Module {
	public static SimpleParticleType STURDY_DEBRIS;

	@Override
	public void register() {
		STURDY_DEBRIS = Particles.simple(ArmossilloIds.Particles.STURDY_DEBRIS, false);
		ArmossilloBlocks.register();
		ArmossilloItems.register();
		ArmossilloEntities.register();
	}
}
