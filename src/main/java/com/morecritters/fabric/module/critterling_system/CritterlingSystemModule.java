package com.morecritters.fabric.module.critterling_system;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.CritterlingSystemIds;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The machinery all critterlings share: the critterling sacks (catching, letting out, the closed
 * mystery sack and gamble mode), the per-player collection of critterlings, the evolution table
 * and evolite, the evolite chandelier and its evolightened effect, the critter eater that hunts
 * critterlings, the evolutioner and his evolite maws, the critterling fossils, and the party
 * decorations (trophy, confetti popper, confetti trail). The critterlings themselves live in
 * critterlings_a..e and build on {@link Critterling} and {@link CritterlingSackItem}.
 */
public final class CritterlingSystemModule implements Module {
	public static Item CRITTERLING_SACK, CLOSED_CRITTERLING_SACK, EVOLITE;
	public static EvolutionTableBlock EVOLUTION_TABLE;
	public static MenuType<EvolutionTableMenu> EVOLUTION_TABLE_MENU;
	public static Holder<MobEffect> EVOLIGHTENED;
	public static EntityType<CritterEaterEntity> CRITTER_EATER;
	public static EntityType<EvolutionerEntity> EVOLUTIONER;
	public static EntityType<EvoliteMawEntity> EVOLITE_MAW;
	public static ConfettiPopperBlock CONFETTI_POPPER;
	public static Item CONFETTI_POPPER_ITEM;
	public static BlockEntityType<ConfettiPopperBlockEntity> CONFETTI_POPPER_BLOCK_ENTITY;
	public static SimpleParticleType BOOST_PARTICLE, CONFETTI_PARTICLE, EVOLIGHTENED_PARTICLE, ZZZ_PARTICLE;

	static SoundEvent SACK_PICK_UP_SOUND, SACK_PUT_DOWN_SOUND, JACKPOT_SOUND;
	static SoundEvent EVOLVE_RARE_SOUND, EVOLVE_EPIC_SOUND, CONFETTI_POP_SOUND;
	static SoundEvent CRITTER_EATER_IDLE_SOUND, CRITTER_EATER_HURT_SOUND, CRITTER_EATER_DEATH_SOUND, CRITTER_EATER_ATTACK_SOUND, CRITTER_EATER_MOVE_SOUND;
	static SoundEvent EVOLUTIONER_IDLE_SOUND, EVOLUTIONER_HURT_SOUND, EVOLUTIONER_DEATH_SOUND, EVOLUTIONER_CHANT_SOUND, EVOLUTIONER_TRANSFORM_SOUND;

	@Override
	public void register() {
		registerSounds();
		registerParticles();
		EVOLIGHTENED = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, CritterlingSystemIds.Effects.EVOLIGHTENED, new EvolightenedEffect());
		registerEntities();
		registerSacks();
		registerItems();
		registerBlocks();

		EVOLUTION_TABLE_MENU = Registry.register(BuiltInRegistries.MENU, CritterlingSystemIds.Menus.EVOLUTION_TABLE_GUI, EvolutionTableMenu.createType());

		CritterlingCollection.register();
		CritterlingCatching.register();
		GambleMode.register();
		EvolightenedEffect.registerInvulnerability();
		LightflyStrike.register();
	}

	private static void registerSounds() {
		SACK_PICK_UP_SOUND = Registration.sound(CritterlingSystemIds.Sounds.ITEM_CRITTERLING_SACK_PICK_UP);
		SACK_PUT_DOWN_SOUND = Registration.sound(CritterlingSystemIds.Sounds.ITEM_CRITTERLING_SACK_PUT_DOWN);
		JACKPOT_SOUND = Registration.sound(CritterlingSystemIds.Sounds.ITEM_CLOSED_CRITTERLING_SACK_JACKPOT);
		EVOLVE_RARE_SOUND = Registration.sound(CritterlingSystemIds.Sounds.BLOCK_EVOLUTION_TABLE_EVOLVE_RARE);
		EVOLVE_EPIC_SOUND = Registration.sound(CritterlingSystemIds.Sounds.BLOCK_EVOLUTION_TABLE_EVOLVE_EPIC);
		CONFETTI_POP_SOUND = Registration.sound(CritterlingSystemIds.Sounds.BLOCK_CONFETTI_POPPER_POP);
		CRITTER_EATER_IDLE_SOUND = Registration.sound(CritterlingSystemIds.Sounds.ENTITY_CRITTER_EATER_IDLE);
		CRITTER_EATER_HURT_SOUND = Registration.sound(CritterlingSystemIds.Sounds.ENTITY_CRITTER_EATER_HURT);
		CRITTER_EATER_DEATH_SOUND = Registration.sound(CritterlingSystemIds.Sounds.ENTITY_CRITTER_EATER_DEATH);
		CRITTER_EATER_ATTACK_SOUND = Registration.sound(CritterlingSystemIds.Sounds.ENTITY_CRITTER_EATER_ATTACK);
		CRITTER_EATER_MOVE_SOUND = Registration.sound(CritterlingSystemIds.Sounds.ENTITY_CRITTER_EATER_MOVE);
		EVOLUTIONER_IDLE_SOUND = Registration.sound(CritterlingSystemIds.Sounds.ENTITY_EVOLUTIONER_IDLE);
		EVOLUTIONER_HURT_SOUND = Registration.sound(CritterlingSystemIds.Sounds.ENTITY_EVOLUTIONER_HURT);
		EVOLUTIONER_DEATH_SOUND = Registration.sound(CritterlingSystemIds.Sounds.ENTITY_EVOLUTIONER_DEATH);
		EVOLUTIONER_CHANT_SOUND = Registration.sound(CritterlingSystemIds.Sounds.ENTITY_EVOLUTIONER_CHANT);
		EVOLUTIONER_TRANSFORM_SOUND = Registration.sound(CritterlingSystemIds.Sounds.ENTITY_EVOLUTIONER_TRANSFORM);
	}

	private static void registerParticles() {
		// boost and zzz are the original's, used by other modules' critters (e.g. the slablizard).
		BOOST_PARTICLE = Particles.simple(CritterlingSystemIds.Particles.BOOST, true);
		ZZZ_PARTICLE = Particles.simple(CritterlingSystemIds.Particles.ZZZ, true);
		CONFETTI_PARTICLE = Particles.simple(CritterlingSystemIds.Particles.CONFETTI, false);
		EVOLIGHTENED_PARTICLE = Particles.simple(CritterlingSystemIds.Particles.EVOLIGHTENED_PARTICLE, false);
	}

	private static void registerEntities() {
		CRITTER_EATER = Registration.livingEntity(CritterlingSystemIds.Entities.CRITTER_EATER,
			EntityType.Builder.of(CritterEaterEntity::new, MobCategory.MONSTER).sized(1.2F, 1.2F).clientTrackingRange(8).updateInterval(3),
			CritterEaterEntity.createAttributes());
		EVOLUTIONER = Registration.livingEntity(CritterlingSystemIds.Entities.EVOLUTIONER,
			EntityType.Builder.of(EvolutionerEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(8).updateInterval(3),
			EvolutionerEntity.createAttributes());
		EVOLITE_MAW = Registration.livingEntity(CritterlingSystemIds.Entities.EVOLITE_MAW,
			EntityType.Builder.of(EvoliteMawEntity::new, MobCategory.MONSTER).fireImmune().sized(0.6F, 1.0F).clientTrackingRange(8).updateInterval(3),
			EvoliteMawEntity.createAttributes());
		Registration.spawnEgg(CritterlingSystemIds.Items.EVOLUTIONER_SPAWN_EGG, EVOLUTIONER);
	}

	private static void registerSacks() {
		CRITTERLING_SACK = Registration.item(CritterlingSystemIds.Items.CRITTERLING_SACK, new Item.Properties().stacksTo(1));
		CLOSED_CRITTERLING_SACK = Registration.item(CritterlingSystemIds.Items.CLOSED_CRITTERLING_SACK, ClosedCritterlingSackItem::new,
			Tooltips.describe(new Item.Properties().stacksTo(1), "closed_critterling_sack", 1));
		CritterlingSackItem.register(CritterlingSystemIds.Items.CRITTERLING_SACK_CRITTER_EATER, CRITTER_EATER, CritterlingRarity.NORMAL, Rarity.EPIC);
	}

	private static void registerItems() {
		EVOLITE = Registration.item(CritterlingSystemIds.Items.EVOLITE, new Item.Properties().rarity(Rarity.EPIC));
		fossil(CritterlingSystemIds.Items.CRITTERLING_FOSSIL_1);
		fossil(CritterlingSystemIds.Items.CRITTERLING_FOSSIL_2);
		fossil(CritterlingSystemIds.Items.CRITTERLING_FOSSIL_3);
	}

	/** Found in fossil blocks and shown on fossil displays (the fossils module); two lines of lore each. */
	private static void fossil(Identifier id) {
		Registration.item(id, Tooltips.describe(new Item.Properties().rarity(Rarity.UNCOMMON), id.getPath(), 2));
	}

	private static void registerBlocks() {
		EVOLUTION_TABLE = Registration.blockWithItem(CritterlingSystemIds.Blocks.EVOLUTION_TABLE, EvolutionTableBlock::new,
			BlockBehaviour.Properties.of().sound(SoundType.WOOD).strength(1.0F).noOcclusion().isRedstoneConductor((state, level, pos) -> false));
		Registration.blockWithItem(CritterlingSystemIds.Blocks.EVOLITE_BLOCK, Block::new,
			BlockBehaviour.Properties.of().sound(SoundType.AMETHYST).strength(1.5F).requiresCorrectToolForDrops());
		blockWithLore(CritterlingSystemIds.Blocks.EVOLITE_CHANDELIER, EvoliteChandelierBlock::new,
			BlockBehaviour.Properties.of().sound(SoundType.AMETHYST).strength(1.0F).lightLevel(state -> 5).requiresCorrectToolForDrops()
				.noOcclusion().emissiveRendering(state -> true).isRedstoneConductor((state, level, pos) -> false));
		blockWithLore(CritterlingSystemIds.Blocks.TROPHY, TrophyBlock::new,
			BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.0F).requiresCorrectToolForDrops()
				.noOcclusion().emissiveRendering(state -> true).isRedstoneConductor((state, level, pos) -> false));
		Registration.blockWithItem(CritterlingSystemIds.Blocks.CONFETTI_TRAIL, ConfettiTrailBlock::new,
			BlockBehaviour.Properties.of().sound(SoundType.AZALEA).instabreak().noOcclusion().isRedstoneConductor((state, level, pos) -> false));

		CONFETTI_POPPER = Registration.block(CritterlingSystemIds.Blocks.CONFETTI_POPPER, ConfettiPopperBlock::new,
			BlockBehaviour.Properties.of().sound(SoundType.WOOD).strength(0.5F).noOcclusion().isRedstoneConductor((state, level, pos) -> false));
		// The item shares the block's name and is drawn by GeckoLib (see GeoItems).
		CONFETTI_POPPER_ITEM = Registration.item(CritterlingSystemIds.Blocks.CONFETTI_POPPER, p -> new ConfettiPopperItem(CONFETTI_POPPER, p),
			new Item.Properties().useBlockDescriptionPrefix());
		CONFETTI_POPPER_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, CritterlingSystemIds.BlockEntities.CONFETTI_POPPER,
			new BlockEntityType<>(ConfettiPopperBlockEntity::new, Set.of(CONFETTI_POPPER)));
	}

	/** A block and its item, with the one {@code block.more_critters.<name>.description_0} lore line the original showed. */
	private static <B extends Block> B blockWithLore(Identifier id, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties properties) {
		B block = Registration.block(id, factory, properties);
		List<Component> lore = new ArrayList<>();
		lore.add(Component.translatable("block.more_critters." + id.getPath() + ".description_0")
			.withStyle(style -> style.withItalic(false).withColor(ChatFormatting.WHITE)));
		Registration.item(id, p -> new BlockItem(block, p),
			new Item.Properties().useBlockDescriptionPrefix().component(DataComponents.LORE, new ItemLore(lore)));
		return block;
	}
}
