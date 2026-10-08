package com.morecritters.fabric.module.treeplet;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Spawns;
import com.morecritters.fabric.ids.TreepletIds;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Map;

/**
 * The treeplet: a timid birch-forest stump that spits slowing resin as it flees and splits into three
 * treeplings when killed; its eerie birch, black resin building blocks, the eerie dart and birch snow cones.
 */
public final class TreepletModule implements Module {
	public static EntityType<TreepletEntity> TREEPLET;
	public static EntityType<TreeplingEntity> TREEPLING_TOP, TREEPLING_MIDDLE, TREEPLING_BOTTOM;
	public static EntityType<ResinPuddleEntity> RESIN_PUDDLE;
	public static EntityType<SplinterProjectile> SPLINTER;
	public static EntityType<ResinPieceProjectile> RESIN_PIECE;
	public static Item EERIE_DART, BLACK_RESIN_CLUMP;
	public static SimpleParticleType RESIN;

	static SoundEvent TREEPLET_IDLE, TREEPLET_HURT, TREEPLET_DEATH, TREEPLET_SPIN, TREEPLET_SPIT, TREEPLET_STEP;
	static SoundEvent TREEPLING_IDLE, TREEPLING_HURT, TREEPLING_DEATH, TREEPLING_STEP;
	static SoundEvent DART_SHOOT, DART_HIT;

	/** Placed features of the original's biome modifiers (data files; their names are not in TreepletIds). */
	private static final String[] BIRCH_FOREST_FEATURES = {"eerie_birch_tree", "fallen_eerie_birch_1", "fallen_eerie_birch_2"};

	/** Eerie birch has no stripped form of its own: under an axe it becomes plain stripped birch. */
	private static Map<Block, Block> strippedForms = Map.of();

	@Override
	public void register() {
		registerSounds();
		RESIN = Particles.simple(TreepletIds.Particles.RESIN, false);
		registerBlocks();
		registerEntities();
		registerItems();
		registerWorldgen();
		UseBlockCallback.EVENT.register(TreepletModule::stripEerieBirch);
	}

	private static void registerSounds() {
		TREEPLET_IDLE = Registration.sound(TreepletIds.Sounds.ENTITY_TREEPLET_IDLE);
		TREEPLET_HURT = Registration.sound(TreepletIds.Sounds.ENTITY_TREEPLET_HURT);
		TREEPLET_DEATH = Registration.sound(TreepletIds.Sounds.ENTITY_TREEPLET_DEATH);
		TREEPLET_SPIN = Registration.sound(TreepletIds.Sounds.ENTITY_TREEPLET_SPIN);
		TREEPLET_SPIT = Registration.sound(TreepletIds.Sounds.ENTITY_TREEPLET_SPIT);
		TREEPLET_STEP = Registration.sound(TreepletIds.Sounds.ENTITY_TREEPLET_STEP);
		TREEPLING_IDLE = Registration.sound(TreepletIds.Sounds.ENTITY_TREEPLING_IDLE);
		TREEPLING_HURT = Registration.sound(TreepletIds.Sounds.ENTITY_TREEPLING_HURT);
		TREEPLING_DEATH = Registration.sound(TreepletIds.Sounds.ENTITY_TREEPLING_DEATH);
		TREEPLING_STEP = Registration.sound(TreepletIds.Sounds.ENTITY_TREEPLING_STEP);
		DART_SHOOT = Registration.sound(TreepletIds.Sounds.ITEM_EERIE_DART_SHOOT);
		DART_HIT = Registration.sound(TreepletIds.Sounds.ITEM_EERIE_DART_HIT);
	}

	private static void registerBlocks() {
		SoundEvent resinBreak = Registration.sound(TreepletIds.Sounds.BLOCK_BLACK_RESIN_BREAK);
		SoundType resin = new SoundType(1.0F, 1.0F, resinBreak,
			Registration.sound(TreepletIds.Sounds.BLOCK_BLACK_RESIN_FOOTSTEPS),
			Registration.sound(TreepletIds.Sounds.BLOCK_BLACK_RESIN_PLACE),
			resinBreak,
			Registration.sound(TreepletIds.Sounds.BLOCK_BLACK_RESIN_FALL));
		SoundType bricks = new SoundType(1.0F, 1.0F,
			Registration.sound(TreepletIds.Sounds.BLOCK_BLACK_RESIN_BRICKS_BREAK),
			Registration.sound(TreepletIds.Sounds.BLOCK_BLACK_RESIN_BRICKS_FOOTSTEPS),
			Registration.sound(TreepletIds.Sounds.BLOCK_BLACK_RESIN_BRICKS_PLACE),
			Registration.sound(TreepletIds.Sounds.BLOCK_BLACK_RESIN_BRICKS_BREAKING),
			Registration.sound(TreepletIds.Sounds.BLOCK_BLACK_RESIN_BRICKS_FALL));

		Block log = Registration.blockWithItem(TreepletIds.Blocks.EERIE_BIRCH_LOG, RotatedPillarBlock::new, eerieBirch());
		Block wood = Registration.blockWithItem(TreepletIds.Blocks.EERIE_BIRCH_WOOD, RotatedPillarBlock::new, eerieBirch());
		strippedForms = Map.of(log, Blocks.STRIPPED_BIRCH_LOG, wood, Blocks.STRIPPED_BIRCH_WOOD);
		Registration.blockWithItem(TreepletIds.Blocks.BLACK_RESIN_BLOCK, Block::new, BlockBehaviour.Properties.of().sound(resin).instabreak());

		Block blackResinBricks = Registration.blockWithItem(TreepletIds.Blocks.BLACK_RESIN_BRICKS, Block::new, blackResinBricks(bricks));
		Registration.blockWithItem(TreepletIds.Blocks.CHISELED_BLACK_RESIN_BRICKS, Block::new, blackResinBricks(bricks));
		Registration.blockWithItem(TreepletIds.Blocks.BLACK_RESIN_BRICK_STAIRS,
			properties -> new StairBlock(blackResinBricks.defaultBlockState(), properties), blackResinBricks(bricks));
		Registration.blockWithItem(TreepletIds.Blocks.BLACK_RESIN_BRICK_SLAB, SlabBlock::new, blackResinBricks(bricks));
		Registration.blockWithItem(TreepletIds.Blocks.BLACK_RESIN_BRICK_WALL, WallBlock::new, blackResinBricks(bricks).forceSolidOn());
	}

	/**
	 * Right-clicking eerie birch log or wood with an axe in the main hand strips it (axis kept): strip
	 * sound, swing, 1 durability (none in creative). The original's right-click event; vanilla's axe
	 * transformer is a data registry that only a full override of {@code minecraft:axe} could extend.
	 */
	private static InteractionResult stripEerieBirch(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		ItemStack held = player.getMainHandItem();
		if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || !held.is(ItemTags.AXES)) {
			return InteractionResult.PASS;
		}
		BlockPos pos = hit.getBlockPos();
		BlockState state = level.getBlockState(pos);
		Block stripped = strippedForms.get(state.getBlock());
		if (stripped == null) {
			return InteractionResult.PASS;
		}
		level.playSound(player, pos, SoundEvents.AXE_STRIP.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
		if (!level.isClientSide()) {
			level.setBlock(pos, stripped.withPropertiesOf(state), Block.UPDATE_ALL);
			held.hurtAndBreak(1, player, hand);
		}
		return InteractionResult.SUCCESS;
	}

	private static BlockBehaviour.Properties eerieBirch() {
		return BlockBehaviour.Properties.of().sound(SoundType.WOOD).strength(2.0F);
	}

	private static BlockBehaviour.Properties blackResinBricks(SoundType sound) {
		return BlockBehaviour.Properties.of().sound(sound).strength(1.5F, 6.0F).requiresCorrectToolForDrops();
	}

	private static void registerEntities() {
		TREEPLET = Registration.livingEntity(TreepletIds.Entities.TREEPLET,
			EntityType.Builder.of(TreepletEntity::new, MobCategory.MONSTER).sized(0.7F, 2.5F).clientTrackingRange(8).updateInterval(3),
			TreepletEntity.createAttributes());
		TREEPLING_TOP = treepling(TreepletIds.Entities.TREEPLING_TOP);
		TREEPLING_MIDDLE = treepling(TreepletIds.Entities.TREEPLING_MIDDLE);
		TREEPLING_BOTTOM = treepling(TreepletIds.Entities.TREEPLING_BOTTOM);
		RESIN_PUDDLE = Registration.livingEntity(TreepletIds.Entities.RESIN_PUDDLE,
			EntityType.Builder.of(ResinPuddleEntity::new, MobCategory.MONSTER).sized(1.0F, 0.0F).clientTrackingRange(8).updateInterval(3),
			ResinPuddleEntity.createAttributes());
		SPLINTER = Registration.entity(TreepletIds.Entities.SPLINTER,
			EntityType.Builder.<SplinterProjectile>of(SplinterProjectile::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(1));
		RESIN_PIECE = Registration.entity(TreepletIds.Entities.RESIN_PIECE,
			EntityType.Builder.<ResinPieceProjectile>of(ResinPieceProjectile::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(1));

		// The original replaced the monster rules with just "sees the sky": treeplets spawn in daylight too.
		SpawnPlacements.register(TREEPLET, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> level.canSeeSkyFromBelowWater(pos));
		Spawns.inBiomeTag(TREEPLET, MobCategory.MONSTER, 20, 1, 1, "more_critters:spawns/treeplet");
	}

	private static EntityType<TreeplingEntity> treepling(Identifier id) {
		return Registration.livingEntity(id,
			EntityType.Builder.of(TreeplingEntity::new, MobCategory.MONSTER).sized(0.8F, 1.0F).clientTrackingRange(8).updateInterval(3),
			TreeplingEntity.createAttributes());
	}

	private static void registerItems() {
		Registration.spawnEgg(TreepletIds.Items.TREEPLET_SPAWN_EGG, TREEPLET);
		Registration.item(TreepletIds.Items.EERIE_BARK);
		Registration.item(TreepletIds.Items.BLACK_RESIN_BRICK);
		BLACK_RESIN_CLUMP = Registration.item(TreepletIds.Items.BLACK_RESIN_CLUMP);
		EERIE_DART = Registration.item(TreepletIds.Items.EERIE_DART, EerieDartItem::new, new Item.Properties());
		// Each bite of a birch snow cone leaves the next, smaller one: cone, 1, 2, then 3 is eaten up.
		Item cone3 = Registration.item(TreepletIds.Items.BIRCH_SNOW_CONE_3, snowCone(2));
		Item cone2 = Registration.item(TreepletIds.Items.BIRCH_SNOW_CONE_2, snowCone(6).usingConvertsTo(cone3));
		Item cone1 = Registration.item(TreepletIds.Items.BIRCH_SNOW_CONE_1, snowCone(6).usingConvertsTo(cone2));
		Registration.item(TreepletIds.Items.BIRCH_SNOW_CONE, snowCone(6).usingConvertsTo(cone1));
	}

	private static Item.Properties snowCone(int nutrition) {
		return new Item.Properties().stacksTo(1).food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(0.3F).build());
	}

	private static void registerWorldgen() {
		for (String feature : BIRCH_FOREST_FEATURES) {
			BiomeModifications.addFeature(BiomeSelectors.tag(TagKey.create(Registries.BIOME, MoreCritters.id("has_feature/eerie_birch_trees"))),
				GenerationStep.Decoration.SURFACE_STRUCTURES, ResourceKey.create(Registries.PLACED_FEATURE, MoreCritters.id(feature)));
		}
	}
}
