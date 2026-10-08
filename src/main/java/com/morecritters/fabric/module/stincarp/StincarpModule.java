package com.morecritters.fabric.module.stincarp;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Spawns;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.StincarpIds;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;

/**
 * The stincarp: an electric river fish that only turns up during thunderstorms, and the
 * asphyxiation poison made from its toxin bladder.
 */
public final class StincarpModule implements Module {
	public static EntityType<StincarpEntity> STINCARP;
	public static Holder<MobEffect> ASPHYXIATION;

	static SoundEvent HURT_SOUND, DEATH_SOUND;
	static Item bucket;

	private static final int POTION_DURATION = 400;

	@Override
	public void register() {
		HURT_SOUND = Registration.sound(StincarpIds.Sounds.ENTITY_STINCARP_HURT);
		DEATH_SOUND = Registration.sound(StincarpIds.Sounds.ENTITY_STINCARP_DEATH);

		STINCARP = Registration.livingEntity(StincarpIds.Entities.STINCARP,
			EntityType.Builder.of(StincarpEntity::new, MobCategory.WATER_CREATURE).sized(2.0F, 0.6F).clientTrackingRange(8).updateInterval(3),
			StincarpEntity.createAttributes());
		Registration.spawnEgg(StincarpIds.Items.STINCARP_SPAWN_EGG, STINCARP);
		// Stands in for the original's "bucket fluid": a vanilla fish bucket that releases a stincarp.
		bucket = Registration.item(StincarpIds.Items.STINARP_BUCKET_BUCKET,
			properties -> new MobBucketItem(STINCARP, Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties),
			new Item.Properties().stacksTo(1));

		ASPHYXIATION = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, StincarpIds.Effects.ASPHYXIATION, new AsphyxiationEffect());
		Registry.register(BuiltInRegistries.POTION, StincarpIds.Potions.ASPHYXIATION_POTION,
			new Potion(StincarpIds.Potions.ASPHYXIATION_POTION.getPath(), new MobEffectInstance(ASPHYXIATION, POTION_DURATION, 0, false, true)));

		Registration.item(StincarpIds.Items.TOXIN_BLADDER_ASPHYXIATION,
			Tooltips.describe(new Item.Properties(), StincarpIds.Items.TOXIN_BLADDER_ASPHYXIATION.getPath(), 1));
		Registration.item(StincarpIds.Items.CUPCAKE_ASPHYXIATION, PoisonedCupcakeItem::new,
			Tooltips.describe(new Item.Properties().stacksTo(16)
				.food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.3F).build()),
				StincarpIds.Items.CUPCAKE_ASPHYXIATION.getPath(), 1));

		// Rivers and swamps, and only while it thunders: the original replaced the placement check with that alone.
		SpawnPlacements.register(STINCARP, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> level.getLevel().isThundering());
		Spawns.inBiomeTag(STINCARP, MobCategory.WATER_CREATURE, 1, 1, 1, "more_critters:spawns/stincarp");
	}
}
