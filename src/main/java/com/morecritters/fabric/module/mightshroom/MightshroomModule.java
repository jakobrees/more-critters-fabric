package com.morecritters.fabric.module.mightshroom;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.MightshroomIds;
import com.morecritters.fabric.ids.NightshroomIds;
import java.util.List;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The mightshroom system: the mightshroom (raised from an ancient skeleton by the Spawn Mightshroom effect) and
 * its zapping echo; the vita and mori shrooms with their huge cap blocks and pots; the six chiseled mushroom
 * stems; the fungal staff and its heal echoes; the stews of life and death; mightshroom ribs; and the Tremble,
 * Gift of Life, Imminent Death and Spawn Mightshroom effects.
 */
public final class MightshroomModule implements Module {
	public static EntityType<MightshroomEntity> MIGHTSHROOM;
	public static EntityType<EchoEntity> MIGHTSHROOM_ECHO, HEAL_ECHO, SMALL_HEAL_ECHO;
	public static Holder<MobEffect> TREMBLE, GIFT_OF_LIFE, IMMINENT_DEATH, SPAWN_MIGHTSHROOM;
	public static SimpleParticleType FEATHER;

	static SoundEvent ANGEL_HEAL_SOUND, ATTACK_SOUND, CRASH_SOUND, DEATH_SOUND, HURT_SOUND, IDLE_SOUND, LEAP_SOUND, LEAP_READY_SOUND,
		SCREAM_SOUND, STEP_SOUND, STOMP_SOUND, TRANSFORM_SOUND, STAFF_BIG_HEAL_SOUND, STAFF_HEAL_SOUND;

	static Item deathStew, lifeStew;
	static Block vitaShroom, moriShroom, potVita, potMori;

	private static final float STEW_EAT_SECONDS = 2.5F;
	private static final int DEATH_STEW_TICKS = 100, LIFE_STEW_TICKS = 60, RIBS_POISON_TICKS = 100;

	@Override
	public void register() {
		registerSounds();
		FEATHER = Particles.simple(MightshroomIds.Particles.MIGHTSHROOM_FEATHER, false);
		TREMBLE = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, MightshroomIds.Effects.TREMBLE, new TrembleEffect());
		GIFT_OF_LIFE = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, MightshroomIds.Effects.GIFT_OF_LIFE, new GiftOfLifeEffect());
		IMMINENT_DEATH = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, MightshroomIds.Effects.IMMINENT_DEATH, new ImminentDeathEffect());
		SPAWN_MIGHTSHROOM = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, MightshroomIds.Effects.SPAWN_MIGHTSHROOM, new SpawnMightshroomEffect());

		registerEntities();
		registerItems();
		registerBlocks();

		UseBlockCallback.EVENT.register(MightshroomModule::potShroom);
		UseEntityCallback.EVENT.register(RaisingRitual::interact);
	}

	private static void registerSounds() {
		ANGEL_HEAL_SOUND = Registration.sound(MightshroomIds.Sounds.AMBIENT_ANGEL_HEAL);
		ATTACK_SOUND = Registration.sound(MightshroomIds.Sounds.ENTITY_MIGHTSHROOM_ATTACK);
		CRASH_SOUND = Registration.sound(MightshroomIds.Sounds.ENTITY_MIGHTSHROOM_CRASH);
		DEATH_SOUND = Registration.sound(MightshroomIds.Sounds.ENTITY_MIGHTSHROOM_DEATH);
		HURT_SOUND = Registration.sound(MightshroomIds.Sounds.ENTITY_MIGHTSHROOM_HURT);
		IDLE_SOUND = Registration.sound(MightshroomIds.Sounds.ENTITY_MIGHTSHROOM_IDLE);
		LEAP_SOUND = Registration.sound(MightshroomIds.Sounds.ENTITY_MIGHTSHROOM_LEAP);
		LEAP_READY_SOUND = Registration.sound(MightshroomIds.Sounds.ENTITY_MIGHTSHROOM_LEAP_READY);
		SCREAM_SOUND = Registration.sound(MightshroomIds.Sounds.ENTITY_MIGHTSHROOM_SCREAM);
		STEP_SOUND = Registration.sound(MightshroomIds.Sounds.ENTITY_MIGHTSHROOM_STEP);
		STOMP_SOUND = Registration.sound(MightshroomIds.Sounds.ENTITY_MIGHTSHROOM_STOMP);
		TRANSFORM_SOUND = Registration.sound(MightshroomIds.Sounds.ENTITY_MIGHTSHROOM_TRANSFORM);
		STAFF_BIG_HEAL_SOUND = Registration.sound(MightshroomIds.Sounds.ITEM_FUNGAL_STAFF_BIG_HEAL);
		STAFF_HEAL_SOUND = Registration.sound(MightshroomIds.Sounds.ITEM_FUNGAL_STAFF_HEAL);
		// The peck attack that would play these is unreachable in the original; registered so the names exist.
		Registration.sound(MightshroomIds.Sounds.ENTITY_MIGHTSHROOM_PECK_HIT);
		Registration.sound(MightshroomIds.Sounds.ENTITY_MIGHTSHROOM_PECK_MISS);
		Registration.sound(MightshroomIds.Sounds.ENTITY_MIGHTSHROOM_PECK_READY);
	}

	private static void registerEntities() {
		// A monster in the original; it has no natural spawns and only rises from ancient skeletons.
		MIGHTSHROOM = Registration.livingEntity(MightshroomIds.Entities.MIGHTSHROOM,
			EntityType.Builder.of(MightshroomEntity::new, MobCategory.MONSTER).sized(1.6F, 5.0F).clientTrackingRange(8).updateInterval(3),
			MightshroomEntity.createAttributes());
		MIGHTSHROOM_ECHO = echo(MightshroomIds.Entities.MIGHTSHROOM_ECHO, EchoEntity.Kind.MIGHTSHROOM);
		HEAL_ECHO = echo(MightshroomIds.Entities.HEAL_ECHO, EchoEntity.Kind.HEAL);
		SMALL_HEAL_ECHO = echo(MightshroomIds.Entities.SMALL_HEAL_ECHO, EchoEntity.Kind.HEAL);
		Registration.spawnEgg(MightshroomIds.Items.MIGHTSHROOM_SPAWN_EGG, MIGHTSHROOM);
	}

	private static EntityType<EchoEntity> echo(Identifier id, EchoEntity.Kind kind) {
		return Registration.livingEntity(id,
			EntityType.Builder.<EchoEntity>of((type, level) -> new EchoEntity(type, level, kind), MobCategory.MONSTER)
				.sized(1.0F, 1.0F).fireImmune().clientTrackingRange(8).updateInterval(3),
			EchoEntity.createAttributes());
	}

	private static void registerItems() {
		// The repair item belongs to the nightshroom module, so it is looked up when item components are built.
		Registration.item(MightshroomIds.Items.FUNGAL_STAFF, FungalStaffItem::new,
			Tooltips.describe(new Item.Properties()
				.sword(FungalStaffItem.MATERIAL, FungalStaffItem.ATTACK_DAMAGE, FungalStaffItem.ATTACK_SPEED)
				.delayedComponent(DataComponents.REPAIRABLE, context -> ancientBoneRepair()), "fungal_staff", 6));
		deathStew = Registration.item(MightshroomIds.Items.DEATH_STEW, stew("death_stew", new MobEffectInstance(IMMINENT_DEATH, DEATH_STEW_TICKS, 0, false, true)));
		lifeStew = Registration.item(MightshroomIds.Items.LIFE_STEW, stew("life_stew", new MobEffectInstance(GIFT_OF_LIFE, LIFE_STEW_TICKS, 0, false, true)));
		Registration.item(MightshroomIds.Items.MIGHTSHROOM_RIBS, new Item.Properties()
			.rarity(Rarity.EPIC)
			.food(new FoodProperties.Builder().nutrition(10).saturationModifier(0.7F).build(),
				Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(
					new MobEffectInstance(MobEffects.POISON, RIBS_POISON_TICKS, 1, false, true))).build()));
	}

	/** A stew: one per stack, always edible, eaten in 2.5 seconds, leaves the bowl. */
	private static Item.Properties stew(String name, MobEffectInstance effect) {
		Consumable eaten = Consumables.defaultFood().consumeSeconds(STEW_EAT_SECONDS).onConsume(new ApplyStatusEffectsConsumeEffect(effect)).build();
		return Tooltips.describe(new Item.Properties()
			.stacksTo(1)
			.rarity(Rarity.UNCOMMON)
			.usingConvertsTo(Items.BOWL)
			.food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.6F).alwaysEdible().build(), eaten), name, 1);
	}

	private static void registerBlocks() {
		potVita = Registration.blockWithItem(MightshroomIds.Blocks.POT_VITA, props -> new PottedShroomBlock(props, () -> vitaShroom), potProperties());
		potMori = Registration.blockWithItem(MightshroomIds.Blocks.POT_MORI, props -> new PottedShroomBlock(props, () -> moriShroom), potProperties());
		vitaShroom = Registration.blockWithItem(MightshroomIds.Blocks.VITA_SHROOM,
			props -> new ShroomBlock(props, () -> potVita, MoreCritters.id("vita_shroom2")), shroomProperties());
		moriShroom = Registration.blockWithItem(MightshroomIds.Blocks.MORI_SHROOM,
			props -> new ShroomBlock(props, () -> potMori, MoreCritters.id("mori_shroom2")), shroomProperties());
		Registration.blockWithItem(MightshroomIds.Blocks.VITA_SHROOM_BLOCK, props -> new ShroomCapBlock(props, () -> vitaShroom), capProperties());
		Registration.blockWithItem(MightshroomIds.Blocks.MORI_SHROOM_BLOCK, props -> new ShroomCapBlock(props, () -> moriShroom), capProperties());
		// Burn like vanilla flowers (the original's flammability 100, fire spread 60).
		FlammableBlockRegistry.getDefaultInstance().add(vitaShroom, 100, 60);
		FlammableBlockRegistry.getDefaultInstance().add(moriShroom, 100, 60);

		for (Identifier stem : List.of(
				MightshroomIds.Blocks.CHISELED_MUSHROOM_STEM_BIRD, MightshroomIds.Blocks.CHISELED_MUSHROOM_STEM_EYE,
				MightshroomIds.Blocks.CHISELED_MUSHROOM_STEM_FIRE, MightshroomIds.Blocks.CHISELED_MUSHROOM_STEM_MUSHROOM,
				MightshroomIds.Blocks.CHISELED_MUSHROOM_STEM_SPIRAL, MightshroomIds.Blocks.CHISELED_MUSHROOM_STEM_THING)) {
			registerChiseledStem(stem);
		}
	}

	private static BlockBehaviour.Properties shroomProperties() {
		return BlockBehaviour.Properties.of()
			.mapColor(MapColor.PLANT)
			.sound(SoundType.GRASS)
			.instabreak()
			.noCollision()
			.offsetType(BlockBehaviour.OffsetType.XZ)
			.pushReaction(PushReaction.POPPED);
	}

	private static BlockBehaviour.Properties capProperties() {
		return BlockBehaviour.Properties.of().sound(SoundType.STONE).strength(0.2F);
	}

	private static BlockBehaviour.Properties potProperties() {
		return BlockBehaviour.Properties.of()
			.sound(SoundType.STONE)
			.instabreak()
			.noOcclusion()
			.isRedstoneConductor((state, level, pos) -> false);
	}

	/** A decorative chiseled stem; its item carries the carving's name (a {@code block.} description line) as lore. */
	private static void registerChiseledStem(Identifier id) {
		Block stem = Registration.block(id, Block::new, BlockBehaviour.Properties.of().sound(SoundType.WOOD).strength(0.2F));
		Component carving = Component.translatable("block.more_critters." + id.getPath() + ".description_0")
			.withStyle(style -> style.withItalic(false).withColor(ChatFormatting.WHITE));
		Registration.item(id, props -> new BlockItem(stem, props),
			new Item.Properties().useBlockDescriptionPrefix().component(DataComponents.LORE, new ItemLore(List.of(carving))));
	}

	/** The staff is mended with the nightshroom module's ancient bone; without that module it cannot be repaired. */
	private static @Nullable Repairable ancientBoneRepair() {
		return BuiltInRegistries.ITEM.getOptional(NightshroomIds.Items.ANCIENT_BONE)
			.map(bone -> new Repairable(HolderSet.direct(bone.builtInRegistryHolder())))
			.orElse(null);
	}

	/** A vita or mori shroom used on an empty flower pot pots it. */
	private static InteractionResult potShroom(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		BlockPos pos = hit.getBlockPos();
		if (hand != InteractionHand.MAIN_HAND || !level.getBlockState(pos).is(Blocks.FLOWER_POT)) {
			return InteractionResult.PASS;
		}
		ItemStack held = player.getMainHandItem();
		Block potted;
		if (held.is(vitaShroom.asItem())) {
			potted = potVita;
		} else if (held.is(moriShroom.asItem())) {
			potted = potMori;
		} else {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			player.swing(hand, SwingAnimation.DEFAULT, true);
			level.setBlock(pos, potted.defaultBlockState(), Block.UPDATE_ALL);
			if (!player.hasInfiniteMaterials()) {
				held.shrink(1);
			}
		}
		return InteractionResult.SUCCESS;
	}
}
