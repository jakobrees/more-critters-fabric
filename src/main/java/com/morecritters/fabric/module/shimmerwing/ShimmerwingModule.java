package com.morecritters.fabric.module.shimmerwing;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Spawns;
import com.morecritters.fabric.ids.ShimmerwingIds;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;

/** The shimmerwing: an End moth, the worm it grows from, the chrysalis the worm hatches from, and End's Blessing. */
public final class ShimmerwingModule implements Module {
	public static EntityType<ShimmerwormEntity> SHIMMERWORM;
	public static EntityType<ShimmerwingEntity> SHIMMERWING;
	public static Holder<MobEffect> ENDS_BLESSING;
	public static SimpleParticleType SHIMMER, HEAL_SHIMMER;

	static SoundEvent WORM_HURT_SOUND, IDLE_SOUND, HURT_SOUND, DEATH_SOUND, GIFT_SOUND, SHED_SOUND;
	static Item endDust, shimmerwormItem;

	@Override
	public void register() {
		WORM_HURT_SOUND = Registration.sound(ShimmerwingIds.Sounds.ENTITY_SHIMMERWORM_HURT);
		IDLE_SOUND = Registration.sound(ShimmerwingIds.Sounds.ENTITY_SHIMMERWING_IDLE);
		HURT_SOUND = Registration.sound(ShimmerwingIds.Sounds.ENTITY_SHIMMERWING_HURT);
		DEATH_SOUND = Registration.sound(ShimmerwingIds.Sounds.ENTITY_SHIMMERWING_DEATH);
		GIFT_SOUND = Registration.sound(ShimmerwingIds.Sounds.ENTITY_SHIMMERWING_GIFT);
		SHED_SOUND = Registration.sound(ShimmerwingIds.Sounds.SHIMMERWING_SHED);
		Registration.sound(ShimmerwingIds.Sounds.ENTITY_SNEEZE); // played by the end dust bunny, another module's item

		SHIMMER = Particles.simple(ShimmerwingIds.Particles.SHIMMER, true);
		HEAL_SHIMMER = Particles.simple(ShimmerwingIds.Particles.HEAL_SHIMMER, true);

		ENDS_BLESSING = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, ShimmerwingIds.Effects.ENDS_BLESSING, new EndsBlessingEffect());
		Registry.register(BuiltInRegistries.POTION, ShimmerwingIds.Potions.ENDS_BLESSING_POTION,
			new Potion(ShimmerwingIds.Potions.ENDS_BLESSING_POTION.getPath(), new MobEffectInstance(ENDS_BLESSING, 2400, 0, false, true)));

		// Both are monsters in the original, so they count against the monster cap.
		SHIMMERWORM = Registration.livingEntity(ShimmerwingIds.Entities.SHIMMERWORM,
			EntityType.Builder.of(ShimmerwormEntity::new, MobCategory.MONSTER).sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(3),
			ShimmerwormEntity.createAttributes());
		SHIMMERWING = Registration.livingEntity(ShimmerwingIds.Entities.SHIMMERWING,
			EntityType.Builder.of(ShimmerwingEntity::new, MobCategory.MONSTER).sized(0.6F, 0.6F).clientTrackingRange(8).updateInterval(3),
			ShimmerwingEntity.createAttributes());
		Registration.spawnEgg(ShimmerwingIds.Items.SHIMMERWING_SPAWN_EGG, SHIMMERWING);

		endDust = Registration.item(ShimmerwingIds.Items.END_DUST, new Item.Properties());
		shimmerwormItem = Registration.item(ShimmerwingIds.Items.SHIMMERWORM_ITEM, ShimmerwormItem::new, new Item.Properties()
			.stacksTo(1)
			.food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.1F).build(),
				Consumables.defaultFood().consumeSeconds(1.0F).build()));

		Registration.blockWithItem(ShimmerwingIds.Blocks.SHIMMERING_CHRYSALIS, ShimmeringChrysalisBlock::new,
			BlockBehaviour.Properties.of()
				.sound(SoundType.HANGING_ROOTS)
				.instabreak()
				.noOcclusion()
				.randomTicks()
				.emissiveRendering(state -> true)
				.isRedstoneConductor((state, level, pos) -> false));

		registerSpawns();
		registerChorusFeatures();
	}

	/** Shimmerwings spawn in the dark on the outer End islands, like monsters. */
	private static void registerSpawns() {
		SpawnPlacements.register(SHIMMERWING, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> level.getDifficulty() != Difficulty.PEACEFUL
				&& Monster.isDarkEnoughToSpawn(level, pos, random)
				&& Mob.checkMobSpawnRules(type, level, reason, pos, random));
		Spawns.inBiomeTag(SHIMMERWING, MobCategory.MONSTER, 1, 1, 1, "more_critters:spawns/shimmerwing");
	}

	/** Chorus plants hung with chrysalises, in the End highlands and midlands. */
	private static void registerChorusFeatures() {
		for (String feature : new String[] {"shimmer_chorus_1", "shimmer_chorus_2", "shimmer_chorus_3"}) {
			BiomeModifications.addFeature(BiomeSelectors.includeByKey(Biomes.END_HIGHLANDS, Biomes.END_MIDLANDS),
				GenerationStep.Decoration.SURFACE_STRUCTURES, ResourceKey.create(Registries.PLACED_FEATURE, MoreCritters.id(feature)));
		}
	}
}
