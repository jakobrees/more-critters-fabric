package com.morecritters.fabric.client;

import java.util.List;

/** The client halves of the modules in {@code Modules}, in the same order. */
public final class ClientModules {
	private static final List<ClientModule> ALL = List.of(
		new com.morecritters.fabric.client.module.armossillo.ArmossilloClientModule(),
		new com.morecritters.fabric.client.module.avoider.AvoiderClientModule(),
		new com.morecritters.fabric.client.module.balloon_rat.BalloonRatClientModule(),
		new com.morecritters.fabric.client.module.blubberfish.BlubberfishClientModule(),
		new com.morecritters.fabric.client.module.bomb_jelly.BombJellyClientModule(),
		new com.morecritters.fabric.client.module.bouncelizard.BouncelizardClientModule(),
		new com.morecritters.fabric.client.module.bunbug.BunbugClientModule(),
		new com.morecritters.fabric.client.module.corpse_crew.CorpseCrewClientModule(),
		new com.morecritters.fabric.client.module.corpse_gear.CorpseGearClientModule(),
		new com.morecritters.fabric.client.module.creeblossom.CreeblossomClientModule(),
		new com.morecritters.fabric.client.module.critter_atlas.CritterAtlasClientModule(),
		new com.morecritters.fabric.client.module.critterling_system.CritterlingSystemClientModule(),
		new com.morecritters.fabric.client.module.critterlings_a.CritterlingsAClientModule(),
		new com.morecritters.fabric.client.module.critterlings_b.CritterlingsBClientModule(),
		new com.morecritters.fabric.client.module.critterlings_c.CritterlingsCClientModule(),
		new com.morecritters.fabric.client.module.critterlings_d.CritterlingsDClientModule(),
		new com.morecritters.fabric.client.module.critterlings_e.CritterlingsEClientModule(),
		new com.morecritters.fabric.client.module.custodian.CustodianClientModule(),
		new com.morecritters.fabric.client.module.dripper.DripperClientModule(),
		new com.morecritters.fabric.client.module.fossils.FossilsClientModule(),
		new com.morecritters.fabric.client.module.ghostly_wood.GhostlyWoodClientModule(),
		new com.morecritters.fabric.client.module.gravedigger.GravediggerClientModule(),
		new com.morecritters.fabric.client.module.iropod.IropodClientModule(),
		new com.morecritters.fabric.client.module.kelpire.KelpireClientModule(),
		new com.morecritters.fabric.client.module.mightshroom.MightshroomClientModule(),
		new com.morecritters.fabric.client.module.misc.MiscClientModule(),
		new com.morecritters.fabric.client.module.nauticrawl.NauticrawlClientModule(),
		new com.morecritters.fabric.client.module.nervoid.NervoidClientModule(),
		new com.morecritters.fabric.client.module.nightshroom.NightshroomClientModule(),
		new com.morecritters.fabric.client.module.ramchu.RamchuClientModule(),
		new com.morecritters.fabric.client.module.shadelet.ShadeletClientModule(),
		new com.morecritters.fabric.client.module.shimmerwing.ShimmerwingClientModule(),
		new com.morecritters.fabric.client.module.ship_fittings.ShipFittingsClientModule(),
		new com.morecritters.fabric.client.module.shock_cube.ShockCubeClientModule(),
		new com.morecritters.fabric.client.module.shriekbat.ShriekbatClientModule(),
		new com.morecritters.fabric.client.module.snowflake_spider.SnowflakeSpiderClientModule(),
		new com.morecritters.fabric.client.module.stincarp.StincarpClientModule(),
		new com.morecritters.fabric.client.module.treeplet.TreepletClientModule(),
		new com.morecritters.fabric.client.module.wandering_collector.WanderingCollectorClientModule(),
		new com.morecritters.fabric.client.module.warptrap.WarptrapClientModule()
	);

	static void registerAll() {
		for (ClientModule module : ALL) module.registerClient();
	}

	private ClientModules() {}
}
