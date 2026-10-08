package com.morecritters.fabric.module.critterlings_c;

import com.morecritters.fabric.module.critterling_system.Critterling;
import com.morecritters.fabric.module.critterling_system.CritterlingRarity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

/** What this module's critterlings share: the same stats, a dry-footed stroll (mothkid, gillmunch), rarity-suffixed textures (dominic, olmer). */
final class CommonTraits {
	/** All four: 3 health, speed 0.3, 3 attack (unused, they never attack), follow range 16. */
	static AttributeSupplier.Builder common() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 3.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	/** A stroll that keeps out of water and pauses while the critterling dances. */
	static WaterAvoidingRandomStrollGoal dryWander(Critterling critterling, double speed) {
		return new WaterAvoidingRandomStrollGoal(critterling, speed) {
			@Override
			public boolean canUse() {
				return !critterling.isDancing() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return !critterling.isDancing() && super.canContinueToUse();
			}
		};
	}

	/** Texture name for critterlings whose files put the rarity after the name ({@code olmer_rare}). */
	static String suffixedTexture(String base, CritterlingRarity rarity) {
		return switch (rarity) {
			case NORMAL -> base;
			case RARE -> base + "_rare";
			case EPIC -> base + "_epic";
		};
	}

	private CommonTraits() {}
}
