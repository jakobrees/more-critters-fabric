package com.morecritters.fabric.module.iropod;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Spawns;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.IropodIds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;

/** The iropod and black iropod, their shells, buckets and helmet, and the iroball. */
public final class IropodModule implements Module {
	public static EntityType<IropodEntity> IROPOD;
	public static EntityType<BlackIropodEntity> BLACK_IROPOD;
	public static EntityType<IroballEntity> IROBALL;
	static SoundEvent IDLE_SOUND, HURT_SOUND, DEATH_SOUND, LOCK_SOUND, UNLOCK_SOUND, SHED_SOUND, IROBALL_HURT_SOUND, IROBALL_HIT_SOUND;
	static Item moldedShell, iropodBucket, blackIropodBucket, iroballItem, spikedIroball;

	@Override
	public void register() {
		registerSounds();
		IROPOD = Registration.livingEntity(IropodIds.Entities.IROPOD,
			EntityType.Builder.of(IropodEntity::new, MobCategory.UNDERGROUND_WATER_CREATURE).sized(1.0F, 0.9F).clientTrackingRange(8).updateInterval(3),
			IropodEntity.createAttributes());
		// The original files the black iropod and the iroball under monsters; kept so they count the same way.
		BLACK_IROPOD = Registration.livingEntity(IropodIds.Entities.BLACK_IROPOD,
			EntityType.Builder.of(BlackIropodEntity::new, MobCategory.MONSTER).sized(1.0F, 0.9F).clientTrackingRange(8).updateInterval(3),
			IropodEntity.createAttributes());
		IROBALL = Registration.livingEntity(IropodIds.Entities.IROBALL,
			EntityType.Builder.of(IroballEntity::new, MobCategory.MONSTER).fireImmune().sized(0.7F, 0.7F).clientTrackingRange(8).updateInterval(3),
			IroballEntity.createAttributes());
		registerItems();
		SpawnPlacements.register(IROPOD, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, IropodEntity::canSpawnAt);
		Spawns.inBiomes(IROPOD, MobCategory.UNDERGROUND_WATER_CREATURE, 5, 2, 5,
			"minecraft:deep_frozen_ocean", "minecraft:deep_cold_ocean", "minecraft:frozen_ocean", "minecraft:cold_ocean");
	}

	private static void registerSounds() {
		IDLE_SOUND = Registration.sound(IropodIds.Sounds.ENTITY_IROPOD_IDLE);
		HURT_SOUND = Registration.sound(IropodIds.Sounds.ENTITY_IROPOD_HURT);
		DEATH_SOUND = Registration.sound(IropodIds.Sounds.ENTITY_IROPOD_DEATH);
		LOCK_SOUND = Registration.sound(IropodIds.Sounds.ENTITY_IROPOD_LOCK);
		UNLOCK_SOUND = Registration.sound(IropodIds.Sounds.ENTITY_IROPOD_UNLOCK);
		SHED_SOUND = Registration.sound(IropodIds.Sounds.ENTITY_IROPOD_SHED);
		IROBALL_HURT_SOUND = Registration.sound(IropodIds.Sounds.ENTITY_IROBALL_HURT);
		IROBALL_HIT_SOUND = Registration.sound(IropodIds.Sounds.ENTITY_IROBALL_HIT);
	}

	private static void registerItems() {
		Registration.spawnEgg(IropodIds.Items.IROPOD_SPAWN_EGG, IROPOD);
		moldedShell = Registration.item(IropodIds.Items.MOLDED_SHELL, new Item.Properties());
		iropodBucket = Registration.item(IropodIds.Items.IROPOD_BUCKET_BUCKET,
			properties -> new MobBucketItem(IROPOD, Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties),
			Tooltips.describe(fishBucket(), "iropod_bucket_bucket", 1));
		blackIropodBucket = Registration.item(IropodIds.Items.BLACK_IROPOD_BUCKET_BUCKET,
			properties -> new MobBucketItem(BLACK_IROPOD, Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties),
			Tooltips.describe(fishBucket(), "black_iropod_bucket_bucket", 1));
		Registration.item(IropodIds.Items.IROPOD_HELMET_HELMET, IropodHelmet::new, Tooltips.describe(new Item.Properties()
			.humanoidArmor(IropodHelmet.MATERIAL, ArmorType.HELMET)
			.repairable(moldedShell), "iropod_helmet_helmet", 1));
		iroballItem = Registration.item(IropodIds.Items.IROBALL_ITEM, properties -> new IroballItem(false, properties),
			new Item.Properties().stacksTo(1));
		spikedIroball = Registration.item(IropodIds.Items.SPIKED_IROBALL, properties -> new IroballItem(true, properties),
			new Item.Properties().stacksTo(1));
	}

	private static Item.Properties fishBucket() {
		return new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET);
	}
}
