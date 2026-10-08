package com.morecritters.fabric.core;

import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Items drawn by GeckoLib from a geo model instead of a flat sprite (the gravedigger jar, the
 * tazegun, ...). The item class implements {@code GeoItem} and overrides
 * {@code getRenderProvider()} to return {@link #providerFor(Item)}; its client module registers
 * the renderer with {@code client.core.GeoItemRenderers}. The item definition in resources uses
 * the {@code minecraft:special} model with GeckoLib's renderer type (tools/upgrade_resources.py).
 * Common code cannot name GeckoLib's client types, hence {@code Object}.
 */
public final class GeoItems {
	private static final Map<Item, Object> PROVIDERS = new IdentityHashMap<>();

	/** Client side only; the provider is a {@code GeoRenderProvider}. */
	public static void registerRenderer(Item item, Object provider) {
		PROVIDERS.put(item, provider);
	}

	public static @Nullable Object providerFor(Item item) {
		return PROVIDERS.get(item);
	}

	private GeoItems() {}
}
