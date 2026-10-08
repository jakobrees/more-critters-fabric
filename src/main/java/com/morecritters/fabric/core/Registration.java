package com.morecritters.fabric.core;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

/**
 * Thin wrappers over {@link Registry#register} for the things every module registers.
 * Since 1.21.2 items and blocks must know their own registry key before construction,
 * which is what makes these worth having.
 */
public final class Registration {
	public static <T extends Entity> EntityType<T> entity(Identifier id, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	/** Registers a living entity type together with its default attributes. */
	public static <T extends LivingEntity> EntityType<T> livingEntity(Identifier id, EntityType.Builder<T> builder, AttributeSupplier.Builder attributes) {
		EntityType<T> type = entity(id, builder);
		FabricDefaultAttributeRegistry.register(type, attributes);
		return type;
	}

	public static Item item(Identifier id, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static Item item(Identifier id, Item.Properties properties) {
		return item(id, Item::new, properties);
	}

	public static Item item(Identifier id) {
		return item(id, new Item.Properties());
	}

	public static Item spawnEgg(Identifier id, EntityType<?> type) {
		return item(id, SpawnEggItem::new, new Item.Properties().spawnEgg(type));
	}

	public static <B extends Block> B block(Identifier id, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}

	/** Registers a block and the item that places it, under the same name. */
	public static <B extends Block> B blockWithItem(Identifier id, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties properties) {
		B block = block(id, factory, properties);
		item(id, p -> new BlockItem(block, p), new Item.Properties().useBlockDescriptionPrefix());
		return block;
	}

	public static SoundEvent sound(Identifier id) {
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	private Registration() {}
}
