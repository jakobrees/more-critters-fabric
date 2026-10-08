package com.morecritters.fabric;

/**
 * One self-contained piece of content: a critter, or a system such as the Critter Atlas.
 * A module registers only the names MODULES.md lists for it; it reaches anything another
 * module owns through the registries, by the identifiers in {@code ids/}.
 */
public interface Module {
	/** Registers blocks, items, entity types, sounds and everything else this module adds. Called on both sides. */
	void register();
}
