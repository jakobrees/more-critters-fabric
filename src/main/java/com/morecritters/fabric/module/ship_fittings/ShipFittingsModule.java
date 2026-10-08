package com.morecritters.fabric.module.ship_fittings;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.ShipFittingsIds;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

/**
 * The ghost ship's working parts: the redstone cannon and its six kinds of cannon ball, the locked treasure chest
 * and its key, the ship wheel (a redstone dial), the tattered jolly roger and the ectometal screws.
 */
public final class ShipFittingsModule implements Module {
	public static CannonBlock CANNON;
	public static BlockEntityType<CannonBlockEntity> CANNON_BLOCK_ENTITY;
	public static MenuType<CannonMenu> CANNON_MENU;

	public static TreasureChestBlock TREASURE_CHEST;
	public static TreasureChestOpeningBlock TREASURE_CHEST_OPENING;
	public static TreasureChestOpenBlock TREASURE_CHEST_OPEN;
	public static BlockEntityType<TreasureChestBlockEntity> TREASURE_CHEST_BLOCK_ENTITY, TREASURE_CHEST_OPENING_BLOCK_ENTITY,
		TREASURE_CHEST_OPEN_BLOCK_ENTITY;
	public static MenuType<TreasureChestMenu> TREASURE_CHEST_MENU;
	static Item TREASURE_KEY;

	public static ShipWheelBlock SHIP_WHEEL;
	public static BlockEntityType<ShipWheelBlockEntity> SHIP_WHEEL_BLOCK_ENTITY;
	public static Item SHIP_WHEEL_ITEM;

	public static JollyRogerBlock TATTERED_JOLLY_ROGER;
	public static BlockEntityType<JollyRogerBlockEntity> TATTERED_JOLLY_ROGER_BLOCK_ENTITY;
	public static Item TATTERED_JOLLY_ROGER_ITEM;

	public static EctometalScrewBlock ECTOMETAL_SCREW;

	/** The ammunition item and the projectile entity of each kind of cannon ball. */
	public static final Map<Infusion, Item> CANNON_BALLS = new EnumMap<>(Infusion.class);
	public static final Map<Infusion, EntityType<CannonBallProjectile>> CANNON_BALL_PROJECTILES = new EnumMap<>(Infusion.class);

	static SoundEvent CANNON_FIRE_SOUND, SHIP_WHEEL_SPIN_SOUND;
	static SoundEvent CHEST_OPEN_SOUND, CHEST_REFUSE_SOUND, CHEST_UNLOCK_SOUND, CHEST_UNLID_SOUND, CHEST_DESTROY_SOUND;

	@Override
	public void register() {
		SoundType cannonSound = registerCannonSounds();
		SoundType chestSound = registerTreasureChestSounds();
		SHIP_WHEEL_SPIN_SOUND = Registration.sound(ShipFittingsIds.Sounds.BLOCK_SHIP_WHEEL_SPIN);

		registerCannonBalls();
		registerCannon(cannonSound);
		registerTreasureChest(chestSound);
		registerShipWheel();
		registerJollyRoger();

		ECTOMETAL_SCREW = Registration.blockWithItem(ShipFittingsIds.Blocks.ECTOMETAL_SCREW, EctometalScrewBlock::new, BlockBehaviour.Properties.of()
			.sound(SoundType.NETHERITE_BLOCK).strength(10.0F, 1200.0F).requiresCorrectToolForDrops().noOcclusion()
			.isRedstoneConductor((state, level, pos) -> false));
		// The thick screw is a plain full-block pillar, placed along the clicked face's axis.
		Registration.blockWithItem(ShipFittingsIds.Blocks.THICK_ECTOMETAL_SCREW, RotatedPillarBlock::new,
			BlockBehaviour.Properties.of().sound(SoundType.NETHERITE_BLOCK).strength(10.0F, 1200.0F).requiresCorrectToolForDrops());
	}

	private static SoundType registerCannonSounds() {
		CANNON_FIRE_SOUND = Registration.sound(ShipFittingsIds.Sounds.BLOCK_CANNON_FIRE);
		return new SoundType(1.0F, 1.0F, Registration.sound(ShipFittingsIds.Sounds.BLOCK_CANNON_BREAK), SoundEvents.METAL_STEP,
			Registration.sound(ShipFittingsIds.Sounds.BLOCK_CANNON_PLACE), Registration.sound(ShipFittingsIds.Sounds.BLOCK_CANNON_BREAKING),
			SoundEvents.METAL_FALL);
	}

	private static SoundType registerTreasureChestSounds() {
		CHEST_OPEN_SOUND = Registration.sound(ShipFittingsIds.Sounds.BLOCK_TREASURE_CHEST_OPEN);
		CHEST_REFUSE_SOUND = Registration.sound(ShipFittingsIds.Sounds.BLOCK_TREASURE_CHEST_REFUSE);
		CHEST_UNLOCK_SOUND = Registration.sound(ShipFittingsIds.Sounds.BLOCK_TREASURE_CHEST_UNLOCK);
		CHEST_UNLID_SOUND = Registration.sound(ShipFittingsIds.Sounds.BLOCK_TREASURE_CHEST_UNLID);
		CHEST_DESTROY_SOUND = Registration.sound(ShipFittingsIds.Sounds.BLOCK_TREASURE_CHEST_DESTROY);
		return new SoundType(1.0F, 1.0F, Registration.sound(ShipFittingsIds.Sounds.BLOCK_TREASURE_CHEST_BREAK), SoundEvents.METAL_STEP,
			Registration.sound(ShipFittingsIds.Sounds.BLOCK_TREASURE_CHEST_PLACE),
			Registration.sound(ShipFittingsIds.Sounds.BLOCK_TREASURE_CHEST_BREAKING), SoundEvents.METAL_FALL);
	}

	private static void registerCannonBalls() {
		CANNON_BALLS.put(Infusion.NONE, Registration.item(ShipFittingsIds.Items.CANNON_BALL, new Item.Properties().stacksTo(16)));
		CANNON_BALLS.put(Infusion.COLD, infusedBall(ShipFittingsIds.Items.INFUSED_CANNON_BALL_COLD));
		CANNON_BALLS.put(Infusion.FIRE, infusedBall(ShipFittingsIds.Items.INFUSED_CANNON_BALL_FIRE));
		CANNON_BALLS.put(Infusion.SLIME, infusedBall(ShipFittingsIds.Items.INFUSED_CANNON_BALL_SLIME));
		CANNON_BALLS.put(Infusion.ELECTRIC, infusedBall(ShipFittingsIds.Items.INFUSED_CANNON_BALL_ELECTRIC));
		CANNON_BALLS.put(Infusion.COMBUSTING, infusedBall(ShipFittingsIds.Items.INFUSED_CANNON_BALL_COMBUSTING));

		CANNON_BALL_PROJECTILES.put(Infusion.NONE, projectile(ShipFittingsIds.Entities.CANNON_BALL_PROJECTILE));
		CANNON_BALL_PROJECTILES.put(Infusion.COLD, projectile(ShipFittingsIds.Entities.COLD_CANNON_BALL_PROJECTILE));
		CANNON_BALL_PROJECTILES.put(Infusion.FIRE, projectile(ShipFittingsIds.Entities.FIRE_CANNON_BALL_PROJECTILE));
		CANNON_BALL_PROJECTILES.put(Infusion.SLIME, projectile(ShipFittingsIds.Entities.SLIME_CANNON_BALL_PROJECTILE));
		CANNON_BALL_PROJECTILES.put(Infusion.ELECTRIC, projectile(ShipFittingsIds.Entities.ELECTRIC_CANNON_BALL_PROJECTILE));
		CANNON_BALL_PROJECTILES.put(Infusion.COMBUSTING, projectile(ShipFittingsIds.Entities.COMBUSTING_CANNON_BALL_PROJECTILE));
	}

	/** Infused balls are rare, stack to 16 and have a two-line tooltip naming their effect. */
	private static Item infusedBall(Identifier id) {
		return Registration.item(id, Tooltips.describe(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), id.getPath(), 2));
	}

	private static EntityType<CannonBallProjectile> projectile(Identifier id) {
		return Registration.entity(id, EntityType.Builder.<CannonBallProjectile>of(CannonBallProjectile::new, MobCategory.MISC)
			.sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(1));
	}

	private static void registerCannon(SoundType sound) {
		CANNON = Registration.blockWithItem(ShipFittingsIds.Blocks.CANNON, CannonBlock::new,
			BlockBehaviour.Properties.of().sound(sound).strength(2.0F).requiresCorrectToolForDrops());
		CANNON_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ShipFittingsIds.BlockEntities.CANNON,
			new BlockEntityType<>(CannonBlockEntity::new, Set.of(CANNON)));
		CANNON_MENU = Registry.register(BuiltInRegistries.MENU, ShipFittingsIds.Menus.CANNON_GUI,
			new MenuType<>(CannonMenu::new, FeatureFlags.VANILLA_SET));
	}

	private static void registerTreasureChest(SoundType sound) {
		TREASURE_KEY = Registration.item(ShipFittingsIds.Items.TREASURE_KEY);

		TREASURE_CHEST = Registration.blockWithItem(ShipFittingsIds.Blocks.TREASURE_CHEST, TreasureChestBlock::new,
			treasureChest(sound).strength(25.0F, 10.0F));
		// The opening and open chests are unbreakable stages of the same chest.
		TREASURE_CHEST_OPENING = Registration.blockWithItem(ShipFittingsIds.Blocks.TREASURE_CHEST_OPENING, TreasureChestOpeningBlock::new,
			treasureChest(sound).strength(-1.0F, 3_600_000.0F));
		TREASURE_CHEST_OPEN = Registration.blockWithItem(ShipFittingsIds.Blocks.TREASURE_CHEST_OPEN, TreasureChestOpenBlock::new,
			treasureChest(sound).strength(-1.0F, 3_600_000.0F));

		TREASURE_CHEST_BLOCK_ENTITY = treasureChestEntity(ShipFittingsIds.BlockEntities.TREASURE_CHEST, TREASURE_CHEST);
		TREASURE_CHEST_OPENING_BLOCK_ENTITY = treasureChestEntity(ShipFittingsIds.BlockEntities.TREASURE_CHEST_OPENING, TREASURE_CHEST_OPENING);
		TREASURE_CHEST_OPEN_BLOCK_ENTITY = treasureChestEntity(ShipFittingsIds.BlockEntities.TREASURE_CHEST_OPEN, TREASURE_CHEST_OPEN);

		TREASURE_CHEST_MENU = Registry.register(BuiltInRegistries.MENU, ShipFittingsIds.Menus.TREASURE_CHEST_GUI,
			new MenuType<>(TreasureChestMenu::new, FeatureFlags.VANILLA_SET));
	}

	private static BlockBehaviour.Properties treasureChest(SoundType sound) {
		return BlockBehaviour.Properties.of().sound(sound).noOcclusion().isRedstoneConductor((state, level, pos) -> false);
	}

	private static BlockEntityType<TreasureChestBlockEntity> treasureChestEntity(Identifier id, Block block) {
		return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, new BlockEntityType<>(TreasureChestBlockEntity::new, Set.of(block)));
	}

	private static void registerShipWheel() {
		SHIP_WHEEL = Registration.block(ShipFittingsIds.Blocks.SHIP_WHEEL, ShipWheelBlock::new, BlockBehaviour.Properties.of()
			.ignitedByLava().instrument(NoteBlockInstrument.BASS).sound(SoundType.WOOD).strength(2.0F, 3.0F).noOcclusion()
			.isRedstoneConductor((state, level, pos) -> false));
		// The item shares the block's name and is drawn by GeckoLib (see GeoItems).
		SHIP_WHEEL_ITEM = Registration.item(ShipFittingsIds.Blocks.SHIP_WHEEL, p -> new GeoBlockItem(SHIP_WHEEL, p),
			new Item.Properties().useBlockDescriptionPrefix());
		SHIP_WHEEL_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ShipFittingsIds.BlockEntities.SHIP_WHEEL,
			new BlockEntityType<>(ShipWheelBlockEntity::new, Set.of(SHIP_WHEEL)));
	}

	private static void registerJollyRoger() {
		TATTERED_JOLLY_ROGER = Registration.block(ShipFittingsIds.Blocks.TATTERED_JOLLY_ROGER, JollyRogerBlock::new, BlockBehaviour.Properties.of()
			.sound(SoundType.WOOL).instabreak().noOcclusion().isRedstoneConductor((state, level, pos) -> false));
		TATTERED_JOLLY_ROGER_ITEM = Registration.item(ShipFittingsIds.Blocks.TATTERED_JOLLY_ROGER, p -> new GeoBlockItem(TATTERED_JOLLY_ROGER, p),
			new Item.Properties().useBlockDescriptionPrefix());
		TATTERED_JOLLY_ROGER_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ShipFittingsIds.BlockEntities.TATTERED_JOLLY_ROGER, new BlockEntityType<>(JollyRogerBlockEntity::new, Set.of(TATTERED_JOLLY_ROGER)));
	}
}
