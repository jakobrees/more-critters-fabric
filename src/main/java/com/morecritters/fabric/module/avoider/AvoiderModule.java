package com.morecritters.fabric.module.avoider;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Spawns;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.AvoiderIds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;

/** The avoider: a shy ocean fish that bolts from players, its fry, its tail and the booster pump made from it. */
public final class AvoiderModule implements Module {
	public static EntityType<AvoiderEntity> AVOIDER;
	public static EntityType<AvoiderFryEntity> AVOIDER_FRY;

	static SoundEvent HURT_SOUND, STUN_SOUND, PUMP_UNDERWATER_SOUND, PUMP_LAND_SOUND;
	static Item avoiderBucket, fryBucket;

	private static final int BOOSTER_PUMP_DURABILITY = 300;

	@Override
	public void register() {
		HURT_SOUND = Registration.sound(AvoiderIds.Sounds.ENTITY_AVOIDER_HURT);
		STUN_SOUND = Registration.sound(AvoiderIds.Sounds.ENTITY_AVOIDER_STUN);
		PUMP_UNDERWATER_SOUND = Registration.sound(AvoiderIds.Sounds.ITEM_BOOSTER_PUMP_UNDERWATER);
		PUMP_LAND_SOUND = Registration.sound(AvoiderIds.Sounds.ITEM_BOOSTER_PUMP_LAND);

		AVOIDER = Registration.livingEntity(AvoiderIds.Entities.AVOIDER,
			EntityType.Builder.of(AvoiderEntity::new, MobCategory.WATER_CREATURE).sized(0.8F, 0.5F).clientTrackingRange(8).updateInterval(3),
			AvoiderEntity.createAttributes());
		// The original registers the fry as a monster; kept so it counts the same way.
		AVOIDER_FRY = Registration.livingEntity(AvoiderIds.Entities.AVOIDER_FRY,
			EntityType.Builder.of(AvoiderFryEntity::new, MobCategory.MONSTER).sized(0.3F, 0.3F).clientTrackingRange(8).updateInterval(3),
			AvoiderFryEntity.createAttributes());

		Registration.spawnEgg(AvoiderIds.Items.AVOIDER_SPAWN_EGG, AVOIDER);
		Registration.item(AvoiderIds.Items.AVOIDER_TAIL, new Item.Properties());
		Registration.item(AvoiderIds.Items.BOOSTER_PUMP, BoosterPumpItem::new,
			Tooltips.describe(new Item.Properties().durability(BOOSTER_PUMP_DURABILITY), "booster_pump", 2));
		avoiderBucket = Registration.item(AvoiderIds.Items.AVOIDER_BUCKET_BUCKET,
			properties -> new MobBucketItem(AVOIDER, Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties),
			fishBucket());
		fryBucket = Registration.item(AvoiderIds.Items.AVOIDER_FRY_BUCKET_BUCKET,
			properties -> new MobBucketItem(AVOIDER_FRY, Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties),
			fishBucket());

		SpawnPlacements.register(AVOIDER, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, AvoiderEntity::canSpawnIn);
		Spawns.inBiomes(AVOIDER, MobCategory.WATER_CREATURE, 5, 1, 2, "minecraft:deep_ocean", "minecraft:ocean");
	}

	private static Item.Properties fishBucket() {
		return new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET);
	}
}
