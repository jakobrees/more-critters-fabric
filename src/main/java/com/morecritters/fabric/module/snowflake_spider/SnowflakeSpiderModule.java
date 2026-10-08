package com.morecritters.fabric.module.snowflake_spider;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Spawns;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.SnowflakeSpiderIds;
import com.morecritters.fabric.ids.SnowflakeSpiderIds.Blocks;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The snowflake spider of the snowy biomes, the web sacks it is made into and the freezing webs
 * they leave, the frostbite, brittleness and webbed effects, and the coldstone building set.
 */
public final class SnowflakeSpiderModule implements Module {
	public static EntityType<SnowflakeSpiderEntity> SNOWFLAKE_SPIDER;
	public static EntityType<WebSackProjectile> WEB_SACK_PROJECTILE;
	public static EntityType<WebEntity> WEB_ENTITY;

	public static Holder<MobEffect> FROSTBITE, BRITTLENESS, WEBBED;
	public static SimpleParticleType BRITTLE_HEART;

	public static Block FREEZING_COBWEB;
	public static Item FREEZING_STRING, SACKOF_FREEZING, WEB_SACK;

	public static SoundEvent WEB_SACK_HIT_SOUND, EXTENSIVE_HURT_SOUND;
	static SoundEvent IDLE_SOUND, HURT_SOUND, DEATH_SOUND, STEP_SOUND;

	private static final int FROSTBITE_POTION_TICKS = 500;
	private static final int BRITTLENESS_POTION_TICKS = 2400;

	@Override
	public void register() {
		IDLE_SOUND = Registration.sound(SnowflakeSpiderIds.Sounds.ENTITY_SNOWFLAKE_SPIDER_IDLE);
		HURT_SOUND = Registration.sound(SnowflakeSpiderIds.Sounds.ENTITY_SNOWFLAKE_SPIDER_HURT);
		DEATH_SOUND = Registration.sound(SnowflakeSpiderIds.Sounds.ENTITY_SNOWFLAKE_SPIDER_DEATH);
		STEP_SOUND = Registration.sound(SnowflakeSpiderIds.Sounds.ENTITY_SNOWFLAKE_SPIDER_STEP);
		WEB_SACK_HIT_SOUND = Registration.sound(SnowflakeSpiderIds.Sounds.ENTITY_WEB_SACK_HIT);
		EXTENSIVE_HURT_SOUND = Registration.sound(SnowflakeSpiderIds.Sounds.ENTITY_EXTENSIVE_HURT);

		BRITTLE_HEART = Particles.simple(SnowflakeSpiderIds.Particles.BRITTLE_HEART, false);

		FROSTBITE = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, SnowflakeSpiderIds.Effects.FROSTBITE, new FrostbiteEffect());
		BRITTLENESS = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, SnowflakeSpiderIds.Effects.BRITTLENESS, new BrittlenessEffect());
		WEBBED = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, SnowflakeSpiderIds.Effects.WEBBED, new WebbedEffect());
		BrittlenessEffect.registerDamageHook();
		potion(SnowflakeSpiderIds.Potions.FROSTBITE_POTION, FROSTBITE, FROSTBITE_POTION_TICKS);
		potion(SnowflakeSpiderIds.Potions.BRITTLENESS_POTION, BRITTLENESS, BRITTLENESS_POTION_TICKS);

		registerEntities();
		registerItems();
		registerBlocks();

		SpawnPlacements.register(SNOWFLAKE_SPIDER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
		Spawns.inBiomeTag(SNOWFLAKE_SPIDER, MobCategory.MONSTER, 25, 1, 1, "more_critters:spawns/snowflake_spider");
	}

	private static void potion(Identifier id, Holder<MobEffect> effect, int ticks) {
		Registry.register(BuiltInRegistries.POTION, id, new Potion(id.getPath(), new MobEffectInstance(effect, ticks, 0, false, true)));
	}

	private static void registerEntities() {
		SNOWFLAKE_SPIDER = Registration.livingEntity(SnowflakeSpiderIds.Entities.SNOWFLAKE_SPIDER,
			EntityType.Builder.of(SnowflakeSpiderEntity::new, MobCategory.MONSTER).sized(1.0F, 1.0F).clientTrackingRange(8).updateInterval(3),
			SnowflakeSpiderEntity.createAttributes());
		WEB_ENTITY = Registration.livingEntity(SnowflakeSpiderIds.Entities.WEB_ENTITY,
			EntityType.Builder.of(WebEntity::new, MobCategory.MONSTER).sized(1.0F, 2.0F).fireImmune().clientTrackingRange(8).updateInterval(3),
			WebEntity.createAttributes());
		WEB_SACK_PROJECTILE = Registration.entity(SnowflakeSpiderIds.Entities.WEB_SACK_PROJECTILE,
			EntityType.Builder.<WebSackProjectile>of(WebSackProjectile::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(1));
	}

	private static void registerItems() {
		Registration.spawnEgg(SnowflakeSpiderIds.Items.SNOWFLAKE_SPIDER_SPAWN_EGG, SNOWFLAKE_SPIDER);
		SACKOF_FREEZING = Registration.item(SnowflakeSpiderIds.Items.SACKOF_FREEZING, new Item.Properties());
		FREEZING_STRING = Registration.item(SnowflakeSpiderIds.Items.FREEZING_STRING, new Item.Properties());
		WEB_SACK = Registration.item(SnowflakeSpiderIds.Items.WEB_SACK, WebSackItem::new, new Item.Properties().stacksTo(1));
		Registration.item(SnowflakeSpiderIds.Items.TOXIN_BLADDER_BRITTLENESS,
			Tooltips.describe(new Item.Properties(), SnowflakeSpiderIds.Items.TOXIN_BLADDER_BRITTLENESS.getPath(), 1));
		Registration.item(SnowflakeSpiderIds.Items.CUPCAKE_BRITTLENESS, BrittlenessCupcakeItem::new,
			Tooltips.describe(new Item.Properties().stacksTo(16)
				.food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.3F).build()),
				SnowflakeSpiderIds.Items.CUPCAKE_BRITTLENESS.getPath(), 1));
	}

	private static void registerBlocks() {
		FREEZING_COBWEB = Registration.blockWithItem(Blocks.FREEZING_COBWEB, FreezingCobwebBlock::new,
			BlockBehaviour.Properties.of().sound(SoundType.STONE).strength(0.5F, 4.0F).noCollision().noOcclusion()
				.isRedstoneConductor((state, level, pos) -> false));

		Block coldstone = Registration.blockWithItem(Blocks.COLDSTONE, ColdstoneBlock::new, coldstone());
		Block bricks = Registration.blockWithItem(Blocks.COLDSTONE_BRICKS, Block::new, coldstone());
		Block polished = Registration.blockWithItem(Blocks.POLISHED_COLDSTONE, Block::new, coldstone());
		Registration.blockWithItem(Blocks.CHISELED_COLDSTONE, Block::new, coldstone());
		stairsSlabWall(coldstone, Blocks.COLDSTONE_STAIRS, Blocks.COLDSTONE_SLAB, Blocks.COLDSTONE_WALL);
		stairsSlabWall(bricks, Blocks.COLDSTONE_BRICK_STAIRS, Blocks.COLDSTONE_BRICK_SLAB, Blocks.COLDSTONE_BRICK_WALL);
		stairsSlabWall(polished, Blocks.POLISHED_COLDSTONE_STAIRS, Blocks.POLISHED_COLDSTONE_SLAB, Blocks.POLISHED_COLDSTONE_WALL);
	}

	/** Every coldstone block is plain stone, mined with a pickaxe. */
	private static BlockBehaviour.Properties coldstone() {
		return BlockBehaviour.Properties.of().sound(SoundType.STONE).strength(1.5F, 6.0F).requiresCorrectToolForDrops();
	}

	private static void stairsSlabWall(Block base, Identifier stairs, Identifier slab, Identifier wall) {
		Registration.blockWithItem(stairs, properties -> new StairBlock(base.defaultBlockState(), properties), coldstone());
		Registration.blockWithItem(slab, SlabBlock::new, coldstone());
		Registration.blockWithItem(wall, WallBlock::new, coldstone().forceSolidOn());
	}
}
