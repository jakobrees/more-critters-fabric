package com.morecritters.fabric.module.critterling_system;

import com.morecritters.fabric.ids.WanderingCollectorIds;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRule;

/**
 * With the {@code gamblemode} game rule on (registered by the wandering collector module), every
 * creature that dies, other than a critterling, drops a closed critterling sack.
 */
final class GambleMode {
	private static final TagKey<EntityType<?>> CRITTERLINGS = TagKey.create(Registries.ENTITY_TYPE, Identifier.withDefaultNamespace("critterling"));
	private static final int PICK_UP_DELAY = 10;

	static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity.level() instanceof ServerLevel level && isOn(level) && !entity.is(CRITTERLINGS)) {
				dropClosedSack(level, entity);
			}
		});
	}

	@SuppressWarnings("unchecked")
	private static boolean isOn(ServerLevel level) {
		GameRule<?> rule = BuiltInRegistries.GAME_RULE.getValue(WanderingCollectorIds.GameRules.GAMBLEMODE);
		return rule != null && Boolean.TRUE.equals(level.getGameRules().get((GameRule<Object>) rule));
	}

	private static void dropClosedSack(ServerLevel level, LivingEntity entity) {
		ItemEntity drop = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), new ItemStack(CritterlingSystemModule.CLOSED_CRITTERLING_SACK));
		drop.setPickUpDelay(PICK_UP_DELAY);
		level.addFreshEntity(drop);
	}

	private GambleMode() {}
}
