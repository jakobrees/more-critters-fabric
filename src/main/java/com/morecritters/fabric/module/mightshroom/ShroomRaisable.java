package com.morecritters.fabric.module.mightshroom;

import com.morecritters.fabric.ids.MightshroomIds;
import com.morecritters.fabric.ids.NightshroomIds;
import net.minecraft.resources.Identifier;

/**
 * A body that the Spawn Mightshroom effect raises as a shroom when it wears off. The nightshroom module's ancient
 * skeleton is meant to implement this (the one allowed sibling-to-system class reference); it keeps the soup in
 * its synced data (the original's {@code shroomed}, which also picks its texture) and plays the animations.
 *
 * <p>This module does the rest: purgatorial mixture used on the body starts the effect, a stew poured on it
 * (once) chooses the shroom, and the effect raises it. An ancient skeleton that does not implement this still
 * takes the mixture and rises, always as a mightshroom, without soup or animations.
 */
public interface ShroomRaisable {
	/** The stew poured on the body, which decides the shroom that rises from it. */
	enum Soup {
		/** No stew: a mightshroom. */
		NONE(MightshroomIds.Entities.MIGHTSHROOM),
		/** Stew of death: a frightshroom. */
		DEATH(NightshroomIds.Entities.FRIGHTSHROOM),
		/** Stew of life: a nightshroom. */
		LIFE(NightshroomIds.Entities.NIGHTSHROOM);

		/** Entity type id of the shroom that rises. */
		public final Identifier shroom;

		Soup(Identifier shroom) {
			this.shroom = shroom;
		}
	}

	Soup soup();

	void pourSoup(Soup soup);

	/** The effect has just been applied: the body starts to shake (the original's {@code fossil_shake} animation). */
	void startRising();

	/** Twenty ticks before it rises, the body comes alive (the original's {@code fossil_alive} animation). */
	void comeAlive();
}
