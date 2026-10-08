package com.morecritters.fabric.client.module.balloon_rat;

import com.morecritters.fabric.client.ClientModule;

/** Rendering for the balloon_rat module: entity renderers, the poison particles and the hallucinazium overlay. */
public final class BalloonRatClientModule implements ClientModule {
	@Override
	public void registerClient() {
		BalloonRatEntitiesClient.register();
		BalloonRatEffectsClient.register();
	}
}
