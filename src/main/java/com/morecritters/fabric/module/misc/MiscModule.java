package com.morecritters.fabric.module.misc;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.MiscIds;
import java.util.List;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The odds and ends: the creative tab and its icon items, four music discs, the party hat,
 * the anniveteran, the End dust bunny, the haste potion, the particles every critter shares,
 * and the extra loot in pillager outposts and woodland mansions.
 */
public final class MiscModule implements Module {
	public static EntityType<AnniveteranEntity> ANNIVETERAN;
	static SoundEvent ANNIVETERAN_HURT_SOUND, ANNIVETERAN_DEATH_SOUND, ANNIVETERAN_STEP_SOUND;

	/** The icon items, shown only in the creative tab and the Critter Atlas; each has one line of description. */
	private static final List<Identifier> ICONS = List.of(
		MiscIds.Items.TAB_ICON, MiscIds.Items.PEBBLE_ICON,
		MiscIds.Items.ICON_1, MiscIds.Items.ICON_2, MiscIds.Items.ICON_3, MiscIds.Items.ICON_4, MiscIds.Items.ICON_5,
		MiscIds.Items.ICON_6, MiscIds.Items.ICON_7, MiscIds.Items.ICON_8, MiscIds.Items.ICON_9, MiscIds.Items.ICON_10,
		MiscIds.Items.ICON_11, MiscIds.Items.ICON_12, MiscIds.Items.ICON_13, MiscIds.Items.ICON_14, MiscIds.Items.ICON_15,
		MiscIds.Items.ICON_16, MiscIds.Items.ICON_17, MiscIds.Items.ICON_18, MiscIds.Items.ICON_19, MiscIds.Items.ICON_20);

	private static final int HASTE_POTION_DURATION = 3600;
	private static final int ANNIVETERAN_SPAWN_WEIGHT = 30;

	@Override
	public void register() {
		registerSounds();
		MiscParticles.register();

		ANNIVETERAN = Registration.livingEntity(MiscIds.Entities.ANNIVETERAN,
			EntityType.Builder.of(AnniveteranEntity::new, MobCategory.CREATURE).sized(0.9F, 1.7F).clientTrackingRange(8).updateInterval(3),
			AnniveteranEntity.createAttributes());
		Registration.spawnEgg(MiscIds.Items.ANNIVETERAN_SPAWN_EGG, ANNIVETERAN);
		// Any biome; the placement rule allows it only in the Overworld on its anniversary.
		SpawnPlacements.register(ANNIVETERAN, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, AnniveteranEntity::canSpawnAt);
		BiomeModifications.addSpawn(BiomeSelectors.all(), MobCategory.CREATURE, ANNIVETERAN, ANNIVETERAN_SPAWN_WEIGHT, 1, 1);

		registerItems();
		Registry.register(BuiltInRegistries.POTION, MiscIds.Potions.HASTE_POTION,
			new Potion(MiscIds.Potions.HASTE_POTION.getPath(), new MobEffectInstance(MobEffects.HASTE, HASTE_POTION_DURATION, 0, false, true)));

		MoreCrittersTab.register();
		OutpostLoot.register();
	}

	private static void registerItems() {
		for (Identifier icon : ICONS) {
			Registration.item(icon, Tooltips.describe(new Item.Properties(), icon.getPath(), 1));
		}
		musicDisc(MiscIds.Items.MUSIC_DISC_SAILS);
		musicDisc(MiscIds.Items.MUSIC_DISC_GROOVEYARD);
		musicDisc(MiscIds.Items.MUSIC_DISC_WADDLE);
		musicDisc(MiscIds.Items.MUSIC_DISC_PARTY);

		Registration.item(MiscIds.Items.END_DUST_BUNNY, EndDustBunnyItem::new, new Item.Properties().rarity(Rarity.EPIC)
			.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).alwaysEdible().build()));
		Registration.item(MiscIds.Items.PARTY_HAT_HELMET, new Item.Properties().humanoidArmor(PartyHat.MATERIAL, ArmorType.HELMET));
	}

	/** The song of each disc is data: {@code jukebox_song/<disc>.json}. */
	private static void musicDisc(Identifier id) {
		Registration.item(id, new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
			.jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG, id)));
	}

	private static void registerSounds() {
		Registration.sound(MiscIds.Sounds.MUSIC_DISC_SAILS);
		Registration.sound(MiscIds.Sounds.MUSIC_DISC_GROOVEYARD);
		Registration.sound(MiscIds.Sounds.MUSIC_DISC_WADDLE);
		Registration.sound(MiscIds.Sounds.MUSIC_DISC_PARTY);
		// Registered as in the original; no disc or song uses it.
		Registration.sound(MiscIds.Sounds.MUSIC_DISC_7);
		ANNIVETERAN_HURT_SOUND = Registration.sound(MiscIds.Sounds.ENTITY_ANNIVETERAN_HURT);
		ANNIVETERAN_DEATH_SOUND = Registration.sound(MiscIds.Sounds.ENTITY_ANNIVETERAN_DEATH);
		ANNIVETERAN_STEP_SOUND = Registration.sound(MiscIds.Sounds.ENTITY_ANNIVETERAN_STEP);
	}
}
