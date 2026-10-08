package com.morecritters.fabric;

import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.StructureFeature;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MoreCritters implements ModInitializer {
	public static final String MOD_ID = "more_critters";
	public static final Logger LOGGER = LoggerFactory.getLogger("More Critters");

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ServerScheduler.init();
		StructureFeature.register();
		Modules.registerAll();
	}
}
