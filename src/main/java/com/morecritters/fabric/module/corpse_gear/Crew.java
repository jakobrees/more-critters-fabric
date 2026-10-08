package com.morecritters.fabric.module.corpse_gear;

import com.morecritters.fabric.ids.CorpseCrewIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Optional;
import java.util.Set;

/** The corpse crew's mobs belong to the corpse_crew module; this module knows them by id only. */
final class Crew {
	/** Those who carry healing rum: a rum flying near one of them breaks and heals the crew. */
	static final Set<Identifier> RUM_CARRIERS = Set.of(CorpseCrewIds.Entities.CORPSE_MATE, CorpseCrewIds.Entities.CORPSE_QUARTERMASTER);
	/** Those whom broken healing rum heals (the lookout is left out, as in the original). */
	static final Set<Identifier> HEALED_BY_RUM = Set.of(CorpseCrewIds.Entities.CORPSE_MATE, CorpseCrewIds.Entities.CORPSE_QUARTERMASTER,
		CorpseCrewIds.Entities.CORPSE_TANK, CorpseCrewIds.Entities.CORPSE_CAPTAIN, CorpseCrewIds.Entities.CORPSE_PARROT);
	/** Those whom crew's rum agrees with. */
	static final Set<Identifier> RUM_DRINKERS = Set.of(CorpseCrewIds.Entities.CORPSE_CAPTAIN, CorpseCrewIds.Entities.CORPSE_MATE,
		CorpseCrewIds.Entities.CORPSE_LOOKOUT, CorpseCrewIds.Entities.CORPSE_PARROT, CorpseCrewIds.Entities.CORPSE_TANK,
		CorpseCrewIds.Entities.CORPSE_QUARTERMASTER);
	/** Who soul rum raises, one of them at random. */
	static final Identifier[] RAISED_BY_SOUL_RUM = {CorpseCrewIds.Entities.CORPSE_MATE, CorpseCrewIds.Entities.CORPSE_QUARTERMASTER,
		CorpseCrewIds.Entities.CORPSE_TANK};

	static boolean isOneOf(Entity entity, Set<Identifier> members) {
		return members.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
	}

	/** The crew member's entity type, if the corpse_crew module has registered it. */
	static Optional<EntityType<?>> type(Identifier member) {
		return BuiltInRegistries.ENTITY_TYPE.getOptional(member);
	}

	private Crew() {}
}
