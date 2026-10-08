package com.morecritters.fabric.module.corpse_gear;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.CorpseGearIds;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.equipment.ArmorType;

/**
 * The pirate crew's gear: the tank's flying pearls, the quartermaster's healing rum, soul rum that raises a
 * crew member where it shatters, hardtack to throw (plain or infested, both stun), the cutlass and pirate
 * armour, a few loot items, and the stunned and crew's rum effects.
 */
public final class CorpseGearModule implements Module {
	public static EntityType<FlyingPearlProjectile> FLYING_PEARL;
	public static EntityType<HealingRumProjectile> HEALING_RUM_PROJECTILE;
	public static EntityType<SoulRumProjectile> SOUL_RUM_PROJECTILE;
	public static EntityType<ThrownHardtack> THROWN_HARDTACK, THROWN_INFESTED_HARDTACK;

	public static Item PEARL, HEALING_RUM, SOUL_RUM, HARDTACK, INFESTED_HARDTACK;

	public static Holder<MobEffect> STUNNED, CREWS_RUM;

	static SoundEvent HEALING_RUM_BREAK, SOUL_RUM_BREAK;

	/** The hardtack piece is eaten in half the usual time (10 ticks). */
	private static final float HARDTACK_PIECE_EAT_SECONDS = 0.5F;
	/** The cutlass's attack damage and speed on top of its material: 6 damage, attack speed 2 in all. */
	private static final float CUTLASS_DAMAGE = 3.0F;
	private static final float CUTLASS_SPEED = -2.0F;

	@Override
	public void register() {
		registerSounds();
		STUNNED = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, CorpseGearIds.Effects.STUNNED, new StunnedEffect());
		CREWS_RUM = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, CorpseGearIds.Effects.CREWS_RUM, new CrewsRumEffect());
		registerItems();
		registerEntities();
	}

	private static void registerSounds() {
		HEALING_RUM_BREAK = Registration.sound(CorpseGearIds.Sounds.ENTITY_HEALING_RUM_BREAK);
		SOUL_RUM_BREAK = Registration.sound(CorpseGearIds.Sounds.ITEM_SOUL_RUM_BREAK);
		// Played by other modules' content (the rum bottle block, infused cannon balls, imminent death), by id.
		Registration.sound(CorpseGearIds.Sounds.BLOCK_RUM_SLIP);
		Registration.sound(CorpseGearIds.Sounds.BLOCK_RUM_BIG_SLIP);
		Registration.sound(CorpseGearIds.Sounds.ENTITY_CANNON_BALL_COLD_HIT);
		Registration.sound(CorpseGearIds.Sounds.ENTITY_CANNON_BALL_FIRE_HIT);
		Registration.sound(CorpseGearIds.Sounds.ENTITY_CANNON_BALL_SLIME_HIT);
		Registration.sound(CorpseGearIds.Sounds.AMBIENT_REAPER_GRUMBLE);
	}

	private static void registerItems() {
		PEARL = Registration.item(CorpseGearIds.Items.PEARL, new Item.Properties().rarity(Rarity.UNCOMMON));
		HEALING_RUM = Registration.item(CorpseGearIds.Items.HEALING_RUM, Tooltips.describe(new Item.Properties().stacksTo(16), "healing_rum", 1));
		SOUL_RUM = Registration.item(CorpseGearIds.Items.SOUL_RUM, Tooltips.describe(new Item.Properties().stacksTo(16), "soul_rum", 1));
		HARDTACK = Registration.item(CorpseGearIds.Items.HARDTACK, properties -> new HardtackItem(properties, false),
			Tooltips.describe(new Item.Properties(), "hardtack", 1));
		INFESTED_HARDTACK = Registration.item(CorpseGearIds.Items.INFESTED_HARDTACK, properties -> new HardtackItem(properties, true),
			Tooltips.describe(new Item.Properties(), "infested_hardtack", 1));
		Registration.item(CorpseGearIds.Items.HARDTACK_PIECE, new Item.Properties().food(
			new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build(),
			Consumables.defaultFood().consumeSeconds(HARDTACK_PIECE_EAT_SECONDS).build()));
		Registration.item(CorpseGearIds.Items.MYSTERIOUS_VIRUS_BOTTLE, new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
		Registration.item(CorpseGearIds.Items.PURGATORIAL_MIXTURE, new Item.Properties().stacksTo(1));

		Registration.item(CorpseGearIds.Items.CUTLASS, new Item.Properties().sword(PirateArmor.CUTLASS_MATERIAL, CUTLASS_DAMAGE, CUTLASS_SPEED));
		Registration.item(CorpseGearIds.Items.PIRATE_HELMET, new Item.Properties().humanoidArmor(PirateArmor.MATERIAL, ArmorType.HELMET));
		Registration.item(CorpseGearIds.Items.PIRATE_CHESTPLATE, new Item.Properties().humanoidArmor(PirateArmor.MATERIAL, ArmorType.CHESTPLATE));
		Registration.item(CorpseGearIds.Items.PIRATE_LEGGINGS, new Item.Properties().humanoidArmor(PirateArmor.MATERIAL, ArmorType.LEGGINGS));
		Registration.item(CorpseGearIds.Items.PIRATE_BOOTS, new Item.Properties().humanoidArmor(PirateArmor.MATERIAL, ArmorType.BOOTS));
	}

	private static void registerEntities() {
		FLYING_PEARL = Registration.entity(CorpseGearIds.Entities.FLYING_PEARL, projectile(FlyingPearlProjectile::new, 0.5F));
		HEALING_RUM_PROJECTILE = Registration.entity(CorpseGearIds.Entities.HEALING_RUM_PROJECTILE, projectile(HealingRumProjectile::new, 0.3F));
		SOUL_RUM_PROJECTILE = Registration.entity(CorpseGearIds.Entities.SOUL_RUM_PROJECTILE, projectile(SoulRumProjectile::new, 0.5F));
		THROWN_HARDTACK = Registration.entity(CorpseGearIds.Entities.THROWN_HARDTACK, projectile(ThrownHardtack::new, 0.5F));
		THROWN_INFESTED_HARDTACK = Registration.entity(CorpseGearIds.Entities.THROWN_INFESTED_HARDTACK, projectile(ThrownHardtack::new, 0.5F));
	}

	private static <T extends GearProjectile> EntityType.Builder<T> projectile(EntityType.EntityFactory<T> factory, float size) {
		return EntityType.Builder.of(factory, MobCategory.MISC).sized(size, size).clientTrackingRange(8).updateInterval(1);
	}
}
