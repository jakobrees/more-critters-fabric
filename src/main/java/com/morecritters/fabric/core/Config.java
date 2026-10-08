package com.morecritters.fabric.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.morecritters.fabric.MoreCritters;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/**
 * The gameplay options of the original mod ({@code config/more_critters.json}).
 * A module declares an option where it uses it, with its default; the file is
 * written with every declared option after the first start so players can see them.
 * Options are read on the server; values are not synced to clients.
 */
public final class Config {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve(MoreCritters.MOD_ID + ".json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<String, JsonElement> VALUES = new TreeMap<>();
	private static boolean loaded;

	public static double number(String key, double defaultValue) {
		JsonElement value = declare(key, defaultValue);
		return value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() ? value.getAsDouble() : defaultValue;
	}

	public static int integer(String key, int defaultValue) {
		return (int) number(key, defaultValue);
	}

	public static boolean flag(String key, boolean defaultValue) {
		JsonElement value = declare(key, defaultValue);
		return value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean() ? value.getAsBoolean() : defaultValue;
	}

	private static synchronized JsonElement declare(String key, Object defaultValue) {
		if (!loaded) load();
		JsonElement existing = VALUES.get(key);
		if (existing != null) return existing;
		JsonElement value = GSON.toJsonTree(defaultValue);
		VALUES.put(key, value);
		save();
		return value;
	}

	private static void load() {
		loaded = true;
		if (!Files.exists(FILE)) return;
		try {
			JsonObject object = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
			object.entrySet().forEach(e -> VALUES.put(e.getKey(), e.getValue()));
		} catch (IOException | RuntimeException e) {
			MoreCritters.LOGGER.warn("Could not read {}; using defaults", FILE, e);
		}
	}

	private static void save() {
		JsonObject object = new JsonObject();
		VALUES.forEach(object::add);
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(object));
		} catch (IOException e) {
			MoreCritters.LOGGER.warn("Could not write {}", FILE, e);
		}
	}

	private Config() {}
}
