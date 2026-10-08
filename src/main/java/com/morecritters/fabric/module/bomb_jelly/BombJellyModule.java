package com.morecritters.fabric.module.bomb_jelly;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Spawns;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.BombJellyIds;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.material.Fluids;

/** Content of the bomb_jelly module; see MODULES.md for the names it owns. */
public final class BombJellyModule implements Module {
	public static EntityType<BombJellyEntity> BOMB_JELLY_SMALL, BOMB_JELLY_MEDIUM, BOMB_JELLY_LARGE;
	public static EntityType<UnsizedBombJellyEntity> BOMB_JELLY;
	public static EntityType<JellyTorpedoEntity> JELLY_TORPEDO;
	public static Holder<MobEffect> COMBUSTION;

	static SoundEvent BEEP_SOUND, WARNING_SOUND, HURT_SOUND, EXPLODE_SOUND, TORPEDO_SPAWN_SOUND, TORPEDO_EXPLODE_SOUND;
	static Item explosiveJelly, smallBucket, mediumBucket, largeBucket;

	private static final int COMBUSTION_TICKS = 200;

	@Override
	public void register() {
		BEEP_SOUND = Registration.sound(BombJellyIds.Sounds.ENTITY_COMBUSTION_BEEP);
		WARNING_SOUND = Registration.sound(BombJellyIds.Sounds.ENTITY_COMBUSTION_WARNING);
		HURT_SOUND = Registration.sound(BombJellyIds.Sounds.ENTITY_BOMB_JELLY_HURT);
		EXPLODE_SOUND = Registration.sound(BombJellyIds.Sounds.ENTITY_BOMB_JELLY_EXPLODE);
		// Registered for its sound event; the original never plays it.
		Registration.sound(BombJellyIds.Sounds.ITEM_EXPLOSIVE_JELLY_RUB);
		TORPEDO_SPAWN_SOUND = Registration.sound(BombJellyIds.Sounds.ENTITY_JELLY_TORPEDO_SPAWN);
		TORPEDO_EXPLODE_SOUND = Registration.sound(BombJellyIds.Sounds.ENTITY_JELLY_TORPEDO_EXPLODE);

		COMBUSTION = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, BombJellyIds.Effects.COMBUSTION, new CombustionEffect());

		// The sized jellies and the torpedo are monsters in the original; kept so they count the same way.
		BOMB_JELLY_SMALL = sizedJelly(BombJellyIds.Entities.BOMB_JELLY_SMALL, BombJellySize.SMALL, 0.6F, 5.0);
		BOMB_JELLY_MEDIUM = sizedJelly(BombJellyIds.Entities.BOMB_JELLY_MEDIUM, BombJellySize.MEDIUM, 0.7F, 7.0);
		BOMB_JELLY_LARGE = sizedJelly(BombJellyIds.Entities.BOMB_JELLY_LARGE, BombJellySize.LARGE, 1.1F, 10.0);
		BOMB_JELLY = Registration.livingEntity(BombJellyIds.Entities.BOMB_JELLY,
			EntityType.Builder.of(UnsizedBombJellyEntity::new, MobCategory.WATER_CREATURE).sized(0.6F, 1.8F).clientTrackingRange(8).updateInterval(3),
			UnsizedBombJellyEntity.createAttributes());
		JELLY_TORPEDO = Registration.livingEntity(BombJellyIds.Entities.JELLY_TORPEDO,
			EntityType.Builder.of(JellyTorpedoEntity::new, MobCategory.MONSTER).sized(0.6F, 0.6F).fireImmune().clientTrackingRange(8).updateInterval(3),
			JellyTorpedoEntity.createAttributes());

		registerItems();

		Spawns.inBiomes(BOMB_JELLY, MobCategory.WATER_CREATURE, 5, 2, 4, "minecraft:deep_lukewarm_ocean", "minecraft:deep_ocean");
		Spawns.inWaterColumn(BOMB_JELLY);
	}

	private static EntityType<BombJellyEntity> sizedJelly(Identifier id, BombJellySize size, float side, double maxHealth) {
		return Registration.livingEntity(id,
			EntityType.Builder.<BombJellyEntity>of((type, level) -> new BombJellyEntity(type, level, size), MobCategory.MONSTER)
				.sized(side, side).clientTrackingRange(8).updateInterval(3),
			BombJellyEntity.createAttributes(maxHealth));
	}

	private static void registerItems() {
		// Eating explosive jelly lights a ten-second fuse inside you.
		explosiveJelly = Registration.item(BombJellyIds.Items.EXPLOSIVE_JELLY, Tooltips.describe(new Item.Properties()
			.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).alwaysEdible().build(),
				Consumables.defaultFood()
					.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(COMBUSTION, COMBUSTION_TICKS, 0, false, true)))
					.build()), "explosive_jelly", 1));
		Registration.spawnEgg(BombJellyIds.Items.BOMB_JELLY_SPAWN_EGG, BOMB_JELLY);
		smallBucket = jellyBucket(BombJellyIds.Items.SMALL_BOMB_JELLY_BUCKET, BOMB_JELLY_SMALL);
		mediumBucket = jellyBucket(BombJellyIds.Items.MEDIUM_BOMB_JELLY_BUCKET, BOMB_JELLY_MEDIUM);
		largeBucket = jellyBucket(BombJellyIds.Items.LARGE_BOMB_JELLY_BUCKET, BOMB_JELLY_LARGE);
		Registration.item(BombJellyIds.Items.JELLY_TORPEDO_ITEM, JellyTorpedoItem::new, new Item.Properties().stacksTo(16));
	}

	/** Each size has its own bucket entity type, so the bucket needs no extra data. */
	private static Item jellyBucket(Identifier id, EntityType<BombJellyEntity> jelly) {
		return Registration.item(id,
			properties -> new MobBucketItem(jelly, Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties),
			new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET));
	}

	static EntityType<BombJellyEntity> typeFor(BombJellySize size) {
		return switch (size) {
			case SMALL -> BOMB_JELLY_SMALL;
			case MEDIUM -> BOMB_JELLY_MEDIUM;
			case LARGE -> BOMB_JELLY_LARGE;
		};
	}

	static Item bucketFor(BombJellySize size) {
		return switch (size) {
			case SMALL -> smallBucket;
			case MEDIUM -> mediumBucket;
			case LARGE -> largeBucket;
		};
	}
}
