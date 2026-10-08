package com.morecritters.fabric;

import java.util.List;

/**
 * The fixed list of content modules. Each module owns one directory under
 * {@code module/} and registers only its own content; this class is the only
 * place that knows about all of them.
 */
public final class Modules {
	private static final List<Module> ALL = List.of(
		new com.morecritters.fabric.module.armossillo.ArmossilloModule(),
		new com.morecritters.fabric.module.avoider.AvoiderModule(),
		new com.morecritters.fabric.module.balloon_rat.BalloonRatModule(),
		new com.morecritters.fabric.module.blubberfish.BlubberfishModule(),
		new com.morecritters.fabric.module.bomb_jelly.BombJellyModule(),
		new com.morecritters.fabric.module.bouncelizard.BouncelizardModule(),
		new com.morecritters.fabric.module.bunbug.BunbugModule(),
		new com.morecritters.fabric.module.corpse_crew.CorpseCrewModule(),
		new com.morecritters.fabric.module.corpse_gear.CorpseGearModule(),
		new com.morecritters.fabric.module.creeblossom.CreeblossomModule(),
		new com.morecritters.fabric.module.critter_atlas.CritterAtlasModule(),
		new com.morecritters.fabric.module.critterling_system.CritterlingSystemModule(),
		new com.morecritters.fabric.module.critterlings_a.CritterlingsAModule(),
		new com.morecritters.fabric.module.critterlings_b.CritterlingsBModule(),
		new com.morecritters.fabric.module.critterlings_c.CritterlingsCModule(),
		new com.morecritters.fabric.module.critterlings_d.CritterlingsDModule(),
		new com.morecritters.fabric.module.critterlings_e.CritterlingsEModule(),
		new com.morecritters.fabric.module.custodian.CustodianModule(),
		new com.morecritters.fabric.module.dripper.DripperModule(),
		new com.morecritters.fabric.module.fossils.FossilsModule(),
		new com.morecritters.fabric.module.ghostly_wood.GhostlyWoodModule(),
		new com.morecritters.fabric.module.gravedigger.GravediggerModule(),
		new com.morecritters.fabric.module.iropod.IropodModule(),
		new com.morecritters.fabric.module.kelpire.KelpireModule(),
		new com.morecritters.fabric.module.mightshroom.MightshroomModule(),
		new com.morecritters.fabric.module.misc.MiscModule(),
		new com.morecritters.fabric.module.nauticrawl.NauticrawlModule(),
		new com.morecritters.fabric.module.nervoid.NervoidModule(),
		new com.morecritters.fabric.module.nightshroom.NightshroomModule(),
		new com.morecritters.fabric.module.ramchu.RamchuModule(),
		new com.morecritters.fabric.module.shadelet.ShadeletModule(),
		new com.morecritters.fabric.module.shimmerwing.ShimmerwingModule(),
		new com.morecritters.fabric.module.ship_fittings.ShipFittingsModule(),
		new com.morecritters.fabric.module.shock_cube.ShockCubeModule(),
		new com.morecritters.fabric.module.shriekbat.ShriekbatModule(),
		new com.morecritters.fabric.module.snowflake_spider.SnowflakeSpiderModule(),
		new com.morecritters.fabric.module.stincarp.StincarpModule(),
		new com.morecritters.fabric.module.treeplet.TreepletModule(),
		new com.morecritters.fabric.module.wandering_collector.WanderingCollectorModule(),
		new com.morecritters.fabric.module.warptrap.WarptrapModule()
	);

	static void registerAll() {
		for (Module module : ALL) module.register();
	}

	private Modules() {}
}
