package com.morecritters.fabric.module.critterling_system;

/**
 * The three forms of every critterling: as caught, evolved once at the evolution table ("rare"),
 * and evolved twice ("epic"). The original stored this as the entity's {@code variant} 0/1/2.
 */
public enum CritterlingRarity {
	NORMAL(""),
	RARE("rare_"),
	EPIC("epic_");

	private final String texturePrefix;

	CritterlingRarity(String texturePrefix) {
		this.texturePrefix = texturePrefix;
	}

	/** {@code cubefrog}, {@code rare_cubefrog}, {@code epic_cubefrog}. */
	public String texture(String baseName) {
		return this.texturePrefix + baseName;
	}

	public static CritterlingRarity byId(int id) {
		CritterlingRarity[] values = values();
		return id >= 0 && id < values.length ? values[id] : NORMAL;
	}
}
