package com.morecritters.fabric.module.shriekbat;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.ShriekbatIds;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The shriekbat: a cave bat that hangs from ceilings and sends out test shrieks; a player moving
 * near one alarms the nearest bat, which then shrieks to turn the monsters around on that player.
 * Also the shriek bomb, the echoes, and Shriek Resistance (soup and potion), which hides you from test shrieks.
 */
public final class ShriekbatModule implements Module {
	private static final int SHRIEK_RESISTANCE_COLOUR = -6998189;
	private static final ResourceKey<Biome> THE_VOID = ResourceKey.create(Registries.BIOME, Identifier.withDefaultNamespace("the_void"));

	public static EntityType<ShriekbatEntity> SHRIEKBAT;
	public static EntityType<EchoEntity> ECHO, LARGE_ECHO;
	public static EntityType<TesterShriekEntity> TESTER_SHRIEK;
	public static EntityType<ShriekbombProjectile> SHRIEKBOMB_PROJECTILE;
	public static Holder<MobEffect> SHRIEK_RESISTANCE;
	public static SimpleParticleType SHRIEK_PARTICLE;
	public static Item SHRIEK_BOMB;

	static SoundEvent IDLE_SOUND, HURT_SOUND, DEATH_SOUND, FLAP_SOUND, SHRIEK_SOUND, BOMB_SHRIEK_SOUND, TEST_SHRIEK_SOUND;

	@Override
	public void register() {
		IDLE_SOUND = Registration.sound(ShriekbatIds.Sounds.ENTITY_SHRIEKBAT_IDLE);
		HURT_SOUND = Registration.sound(ShriekbatIds.Sounds.ENTITY_SHRIEKBAT_HURT);
		DEATH_SOUND = Registration.sound(ShriekbatIds.Sounds.ENTITY_SHRIEKBAT_DEATH);
		FLAP_SOUND = Registration.sound(ShriekbatIds.Sounds.ENTITY_SHRIEKBAT_FLAP);
		SHRIEK_SOUND = Registration.sound(ShriekbatIds.Sounds.ENTITY_SHRIEKBAT_SHRIEK);
		BOMB_SHRIEK_SOUND = Registration.sound(ShriekbatIds.Sounds.ENTITY_SHRIEK_BOMB_SHRIEK);
		TEST_SHRIEK_SOUND = Registration.sound(ShriekbatIds.Sounds.ENTITY_SHRIEKBAT_TEST_SHRIEK);

		SHRIEK_PARTICLE = Particles.simple(ShriekbatIds.Particles.SHRIEKBAT_SHRIEK, true);

		// The effect does nothing by itself; test shrieks ignore whoever has it.
		SHRIEK_RESISTANCE = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, ShriekbatIds.Effects.SHRIEK_RESISTANCE,
			new MobEffect(MobEffectCategory.NEUTRAL, SHRIEK_RESISTANCE_COLOUR) {});
		Registry.register(BuiltInRegistries.POTION, ShriekbatIds.Potions.SHRIEK_RESISTANCE_POTION,
			new Potion(ShriekbatIds.Potions.SHRIEK_RESISTANCE_POTION.getPath(), new MobEffectInstance(SHRIEK_RESISTANCE, 3600, 0, false, true)));

		SHRIEKBAT = Registration.livingEntity(ShriekbatIds.Entities.SHRIEKBAT,
			EntityType.Builder.of(ShriekbatEntity::new, MobCategory.MONSTER).sized(1.0F, 1.5F).clientTrackingRange(8).updateInterval(3),
			ShriekbatEntity.createAttributes());
		// The echo and the large echo are the same stationary effect; the large one is drawn twice the size.
		ECHO = Registration.livingEntity(ShriekbatIds.Entities.ECHO,
			EntityType.Builder.<EchoEntity>of(EchoEntity::new, MobCategory.MONSTER).fireImmune().sized(1.0F, 1.0F).clientTrackingRange(8).updateInterval(3),
			EchoEntity.createAttributes());
		LARGE_ECHO = Registration.livingEntity(ShriekbatIds.Entities.LARGE_ECHO,
			EntityType.Builder.<EchoEntity>of(EchoEntity::new, MobCategory.MONSTER).fireImmune().sized(1.0F, 1.0F).clientTrackingRange(8).updateInterval(3),
			EchoEntity.createAttributes());
		TESTER_SHRIEK = Registration.livingEntity(ShriekbatIds.Entities.TESTER_SHRIEK,
			EntityType.Builder.of(TesterShriekEntity::new, MobCategory.MONSTER).fireImmune().sized(1.0F, 1.0F).clientTrackingRange(8).updateInterval(3),
			TesterShriekEntity.createAttributes());
		SHRIEKBOMB_PROJECTILE = Registration.entity(ShriekbatIds.Entities.SHRIEKBOMB_PROJECTILE,
			EntityType.Builder.<ShriekbombProjectile>of(ShriekbombProjectile::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(1));
		Registration.spawnEgg(ShriekbatIds.Items.SHRIEKBAT_SPAWN_EGG, SHRIEKBAT);

		Registration.item(ShriekbatIds.Items.SHRIEKBAT_WING, new Item.Properties());
		SHRIEK_BOMB = Registration.item(ShriekbatIds.Items.SHRIEK_BOMB, ShriekBombItem::new, new Item.Properties().stacksTo(1));
		Registration.item(ShriekbatIds.Items.SHRIEKBAT_SOUP, ShriekbatSoupItem::new, Tooltips.describe(new Item.Properties()
			.stacksTo(1)
			.usingConvertsTo(Items.BOWL)
			.food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.6F).alwaysEdible().build()), "shriekbat_soup", 1));

		registerSpawns();
	}

	/** In any biome, but only in the Overworld below y=0 out of sight of the sky: deep caves. */
	private static void registerSpawns() {
		SpawnPlacements.register(SHRIEKBAT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> level.getLevel().dimension() == Level.OVERWORLD
				&& pos.getY() <= 0
				&& !level.getBiome(pos).is(THE_VOID)
				&& !level.canSeeSkyFromBelowWater(pos));
		BiomeModifications.addSpawn(BiomeSelectors.all(), MobCategory.MONSTER, SHRIEKBAT, 4, 1, 1);
	}
}
