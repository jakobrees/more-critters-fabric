package com.morecritters.fabric.module.blubberfish;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Spawns;
import com.morecritters.fabric.ids.BlubberfishIds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;

/** The blubberfish: a deep lukewarm ocean fish, its fry, its meat and blubber, and sprinkles that make it pop. */
public final class BlubberfishModule implements Module {
	public static EntityType<BlubberfishEntity> BLUBBERFISH;
	public static EntityType<BlubberfishFryEntity> BLUBBERFISH_FRY;
	public static BlubberBlock BLUBBER;

	static SoundEvent HURT_SOUND, EXPLODE_SOUND;
	static Item rawBlubberfish, sprinkles, blubberfishBucket, fryBucket;

	/** Sprinkles are eaten in half a second rather than the usual 1.6. */
	private static final float SPRINKLES_EAT_SECONDS = 0.5F;

	@Override
	public void register() {
		HURT_SOUND = Registration.sound(BlubberfishIds.Sounds.ENTITY_BLUBBERFISH_HURT);
		EXPLODE_SOUND = Registration.sound(BlubberfishIds.Sounds.ENTITY_BLUBBERFISH_EXPLODE);

		BLUBBERFISH = Registration.livingEntity(BlubberfishIds.Entities.BLUBBERFISH,
			EntityType.Builder.of(BlubberfishEntity::new, MobCategory.WATER_CREATURE).sized(0.7F, 0.7F).clientTrackingRange(8).updateInterval(3),
			BlubberfishEntity.createAttributes());
		// The original registers the fry as a monster; kept so it counts the same way.
		BLUBBERFISH_FRY = Registration.livingEntity(BlubberfishIds.Entities.BLUBBERFISH_FRY,
			EntityType.Builder.of(BlubberfishFryEntity::new, MobCategory.MONSTER).sized(0.3F, 0.3F).clientTrackingRange(8).updateInterval(3),
			BlubberfishFryEntity.createAttributes());

		BLUBBER = Registration.blockWithItem(BlubberfishIds.Blocks.BLUBBER, BlubberBlock::new, BlockBehaviour.Properties.of()
			.sound(SoundType.SLIME_BLOCK).instabreak().noCollision().noOcclusion().isRedstoneConductor((state, level, pos) -> false));

		Registration.spawnEgg(BlubberfishIds.Items.BLUBBERFISH_SPAWN_EGG, BLUBBERFISH);
		registerItems();

		SpawnPlacements.register(BLUBBERFISH, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlubberfishEntity::canSpawnIn);
		Spawns.inBiomes(BLUBBERFISH, MobCategory.WATER_CREATURE, 25, 3, 5, "minecraft:deep_lukewarm_ocean");
	}

	private static void registerItems() {
		rawBlubberfish = Registration.item(BlubberfishIds.Items.RAW_BLUBBERFISH, food(4, 0.3F));
		Registration.item(BlubberfishIds.Items.COOKED_BLUBBERFISH, food(8, 0.5F));
		sprinkles = Registration.item(BlubberfishIds.Items.SPRINKLES, new Item.Properties()
			.food(foodValues(1, 0.1F), Consumables.defaultFood().consumeSeconds(SPRINKLES_EAT_SECONDS).build()));
		blubberfishBucket = Registration.item(BlubberfishIds.Items.BLUBBERFISH_BUCKET_BUCKET,
			properties -> new MobBucketItem(BLUBBERFISH, Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties),
			fishBucket());
		fryBucket = Registration.item(BlubberfishIds.Items.BLUBBERFISH_FRY_BUCKET_BUCKET,
			properties -> new MobBucketItem(BLUBBERFISH_FRY, Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties),
			fishBucket());
	}

	private static Item.Properties fishBucket() {
		return new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET);
	}

	private static Item.Properties food(int nutrition, float saturation) {
		return new Item.Properties().food(foodValues(nutrition, saturation));
	}

	private static FoodProperties foodValues(int nutrition, float saturation) {
		return new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build();
	}
}
