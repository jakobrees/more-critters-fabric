package com.morecritters.fabric.module.ramchu;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.RamchuIds;
import com.morecritters.fabric.module.ramchu.RamchuEntity.ShellState;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;

/** The ramchu of underground aquifers, its fry, its buckets, ramchu oil and the oiled up effect. */
public final class RamchuModule implements Module {
	public static EntityType<RamchuEntity> RAMCHU;
	public static EntityType<RamchuFryEntity> RAMCHU_FRY;
	public static Holder<MobEffect> OILED_UP;

	static SoundEvent IDLE_SOUND, HURT_SOUND, BUST_SOUND, OIL_SOUND, OIL_SLIP_SOUND;
	static Item oilBottle, fryBucket, shelledBucket, noShellBucket, noOilBucket;

	private static final int OILED_UP_TICKS = 6000;
	/** Ramchus only spawn at or below this height, out of sight of the sky. */
	private static final int MAX_SPAWN_Y = 20;
	private static final TagKey<Biome> ALEXS_CAVES_BIOMES = TagKey.create(Registries.BIOME, Identifier.parse("alexscaves:alexs_caves_biomes"));

	@Override
	public void register() {
		IDLE_SOUND = Registration.sound(RamchuIds.Sounds.ENTITY_RAMCHU_IDLE);
		HURT_SOUND = Registration.sound(RamchuIds.Sounds.ENTITY_RAMCHU_HURT);
		// Registered for its sound event; the original never plays it.
		Registration.sound(RamchuIds.Sounds.ENTITY_RAMCHU_ATTACK);
		BUST_SOUND = Registration.sound(RamchuIds.Sounds.ENTITY_RAMCHU_BUST);
		OIL_SOUND = Registration.sound(RamchuIds.Sounds.ENTITY_RAMCHU_OIL);
		OIL_SLIP_SOUND = Registration.sound(RamchuIds.Sounds.EFFECT_OILED_UP_OIL_SLIP);

		OILED_UP = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, RamchuIds.Effects.OILED_UP, new OiledUpEffect());
		OiledUpEffect.registerSlipping();

		RAMCHU = Registration.livingEntity(RamchuIds.Entities.RAMCHU,
			EntityType.Builder.of(RamchuEntity::new, MobCategory.UNDERGROUND_WATER_CREATURE).sized(1.0F, 0.8F).clientTrackingRange(8).updateInterval(3),
			RamchuEntity.createAttributes());
		// The original registers the fry as a monster; kept so it counts the same way.
		RAMCHU_FRY = Registration.livingEntity(RamchuIds.Entities.RAMCHU_FRY,
			EntityType.Builder.of(RamchuFryEntity::new, MobCategory.MONSTER).sized(0.3F, 0.3F).clientTrackingRange(8).updateInterval(3),
			RamchuFryEntity.createAttributes());

		Registration.spawnEgg(RamchuIds.Items.RAMCHU_SPAWN_EGG, RAMCHU);
		registerItems();

		SpawnPlacements.register(RAMCHU, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, RamchuModule::canSpawnAt);
		BiomeModifications.addSpawn(BiomeSelectors.all(), MobCategory.UNDERGROUND_WATER_CREATURE, RAMCHU, 200, 2, 5);
	}

	private static void registerItems() {
		oilBottle = Registration.item(RamchuIds.Items.RAMCHU_OIL_BOTTLE, Tooltips.describe(new Item.Properties()
			.stacksTo(16)
			.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).alwaysEdible().build(),
				Consumables.defaultDrink()
					.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(OILED_UP, OILED_UP_TICKS, 0, false, true)))
					.build())
			.usingConvertsTo(Items.GLASS_BOTTLE), "ramchu_oil_bottle", 1));

		fryBucket = Registration.item(RamchuIds.Items.RAMCHU_FRY_BUCKET_BUCKET,
			properties -> new MobBucketItem(RAMCHU_FRY, Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties),
			fishBucket());
		shelledBucket = ramchuBucket(RamchuIds.Items.RAMCHU_BUCKET_BUCKET, ShellState.SHELLED);
		noShellBucket = ramchuBucket(RamchuIds.Items.RAMCHU_BUCKET_NO_SHELL_BUCKET, ShellState.NO_SHELL);
		noOilBucket = ramchuBucket(RamchuIds.Items.RAMCHU_BUCKET_NO_OIL_BUCKET, ShellState.NO_OIL);
	}

	/**
	 * One bucket per shell state, all releasing a ramchu. Each carries its shell state as default
	 * bucket entity data, so a bucket from the creative tab releases the right ramchu too.
	 */
	private static Item ramchuBucket(Identifier id, ShellState state) {
		CompoundTag entityData = new CompoundTag();
		entityData.putInt(RamchuEntity.SHELL_STATE_KEY, state.ordinal());
		return Registration.item(id,
			properties -> new MobBucketItem(RAMCHU, Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties),
			fishBucket().component(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(entityData)));
	}

	static Item bucketFor(ShellState state) {
		return switch (state) {
			case SHELLED -> shelledBucket;
			case NO_SHELL -> noShellBucket;
			case NO_OIL -> noOilBucket;
		};
	}

	private static Item.Properties fishBucket() {
		return new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET);
	}

	/** Overworld aquifers: low down and with no sky above the water, outside Alex's Caves biomes. */
	private static boolean canSpawnAt(EntityType<RamchuEntity> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return level.getLevel().dimension() == Level.OVERWORLD
			&& !level.getBiome(pos).is(Biomes.THE_VOID)
			&& pos.getY() <= MAX_SPAWN_Y
			&& !level.canSeeSkyFromBelowWater(pos)
			&& !level.getBiome(pos).is(ALEXS_CAVES_BIOMES);
	}
}
