package com.morecritters.fabric.module.wandering_collector;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.ids.WanderingCollectorIds;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

/**
 * The wandering collector, a travelling merchant who buys critter drops from every module, his
 * carrybugs (rideable pack beasts with a chest), the critter kebab, and the {@code gamblemode}
 * game rule.
 */
public final class WanderingCollectorModule implements Module {
	public static Item CRITTER_KEBAB, CRITTER_KEBAB_2, CRITTER_KEBAB_3;
	/** Read by the critterlings module (gamble mode drops critterling sacks). */
	public static GameRule<Boolean> GAMBLE_MODE;

	static SoundEvent COLLECTOR_IDLE_SOUND, COLLECTOR_HURT_SOUND, COLLECTOR_DEATH_SOUND, COLLECTOR_TRADE_SOUND;
	static SoundEvent CARRYBUG_IDLE_SOUND, CARRYBUG_HURT_SOUND, CARRYBUG_DEATH_SOUND, CARRYBUG_STEP_SOUND, CARRYBUG_KICK_SOUND;

	private static final FoodProperties KEBAB_FOOD = new FoodProperties.Builder().nutrition(6).saturationModifier(0.5F).build();

	@Override
	public void register() {
		COLLECTOR_IDLE_SOUND = Registration.sound(WanderingCollectorIds.Sounds.ENTITY_WANDERING_COLLECTOR_IDLE);
		COLLECTOR_HURT_SOUND = Registration.sound(WanderingCollectorIds.Sounds.ENTITY_WANDERING_COLLECTOR_HURT);
		COLLECTOR_DEATH_SOUND = Registration.sound(WanderingCollectorIds.Sounds.ENTITY_WANDERING_COLLECTOR_DEATH);
		COLLECTOR_TRADE_SOUND = Registration.sound(WanderingCollectorIds.Sounds.ENTITY_WANDERING_COLLECTOR_TRADE);
		CARRYBUG_IDLE_SOUND = Registration.sound(WanderingCollectorIds.Sounds.ENTITY_CARRYBUG_IDLE);
		CARRYBUG_HURT_SOUND = Registration.sound(WanderingCollectorIds.Sounds.ENTITY_CARRYBUG_HURT);
		CARRYBUG_DEATH_SOUND = Registration.sound(WanderingCollectorIds.Sounds.ENTITY_CARRYBUG_DEATH);
		CARRYBUG_STEP_SOUND = Registration.sound(WanderingCollectorIds.Sounds.ENTITY_CARRYBUG_STEP);
		CARRYBUG_KICK_SOUND = Registration.sound(WanderingCollectorIds.Sounds.ENTITY_CARRYBUG_KICK);

		// A kebab is eaten in three bites: each one leaves the next, the last leaves the stick.
		CRITTER_KEBAB_3 = Registration.item(WanderingCollectorIds.Items.CRITTER_KEBAB_3, kebab(Items.STICK));
		CRITTER_KEBAB_2 = Registration.item(WanderingCollectorIds.Items.CRITTER_KEBAB_2, kebab(CRITTER_KEBAB_3));
		CRITTER_KEBAB = Registration.item(WanderingCollectorIds.Items.CRITTER_KEBAB, kebab(CRITTER_KEBAB_2));

		GAMBLE_MODE = GameRuleBuilder.forBoolean(false).category(GameRuleCategory.MOBS)
			.buildAndRegister(WanderingCollectorIds.GameRules.GAMBLEMODE);

		WanderingCollector.register();

		CarrybugRegistry.register();
	}

	private static Item.Properties kebab(Item leftover) {
		return new Item.Properties().stacksTo(1).food(KEBAB_FOOD).usingConvertsTo(leftover);
	}
}
