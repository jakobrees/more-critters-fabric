package com.morecritters.fabric.module.shock_cube;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.ShockCubeIds;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumables;

/**
 * Electricity: shock cubes (lingering zaps left where something electric bursts), the taser,
 * the tazegun and its thunderballs, the bottle o' electricity and the electrocuted effect.
 */
public final class ShockCubeModule implements Module {
	public static EntityType<ShockCubeEntity> SHOCK_CUBE, SHOCK_CUBE_SMALL;
	public static EntityType<ThunderballProjectile> THUNDERBALL;
	public static Item BOTTLE_OF_ELECTRICITY, TASER, TAZEGUN;
	public static Holder<MobEffect> ELECTROCUTED;
	public static SimpleParticleType ZAP, ZAP_SPARK;
	static SoundEvent ELECTRIC_BLAST_SOUND, ELECTRIC_BOTTLE_DRINK_SOUND, TASER_TASE_SOUND, ELECTRIC_HUM_SOUND,
		ELECTRIC_TRANSFER_SOUND, TAZEGUN_SHOOT_SOUND, TAZEGUN_EXPLODE_SOUND, TAZEGUN_REFUSE_SOUND, TAZEGUN_RELOAD_SOUND;

	@Override
	public void register() {
		ELECTRIC_BLAST_SOUND = Registration.sound(ShockCubeIds.Sounds.ENTITY_ELECTRIC_BLAST);
		ELECTRIC_BOTTLE_DRINK_SOUND = Registration.sound(ShockCubeIds.Sounds.ITEM_ELECTRIC_BOTTLE_DRINK);
		TASER_TASE_SOUND = Registration.sound(ShockCubeIds.Sounds.ITEM_TASER_TASE);
		ELECTRIC_HUM_SOUND = Registration.sound(ShockCubeIds.Sounds.AMBIENT_ELECTRIC_HUM);
		ELECTRIC_TRANSFER_SOUND = Registration.sound(ShockCubeIds.Sounds.ENTITY_ELECTRIC_TRANSFER);
		TAZEGUN_SHOOT_SOUND = Registration.sound(ShockCubeIds.Sounds.ITEM_TAZEGUN_SHOOT);
		TAZEGUN_EXPLODE_SOUND = Registration.sound(ShockCubeIds.Sounds.ITEM_TAZEGUN_EXPLODE);
		TAZEGUN_REFUSE_SOUND = Registration.sound(ShockCubeIds.Sounds.ITEM_TAZEGUN_REFUSE);
		TAZEGUN_RELOAD_SOUND = Registration.sound(ShockCubeIds.Sounds.ITEM_TAZEGUN_RELOAD);

		ZAP = Particles.simple(ShockCubeIds.Particles.ZAP, false);
		ZAP_SPARK = Particles.simple(ShockCubeIds.Particles.ZAP_SPARK, false);
		ELECTROCUTED = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, ShockCubeIds.Effects.ELECTROCUTED, new ElectrocutedEffect());

		SHOCK_CUBE = Registration.livingEntity(ShockCubeIds.Entities.SHOCK_CUBE,
			EntityType.Builder.of(ShockCubeEntity::new, MobCategory.MONSTER).sized(4.0F, 4.0F).clientTrackingRange(8).updateInterval(3),
			ShockCubeEntity.createAttributes());
		SHOCK_CUBE_SMALL = Registration.livingEntity(ShockCubeIds.Entities.SHOCK_CUBE_SMALL,
			EntityType.Builder.of(ShockCubeEntity::new, MobCategory.MONSTER).sized(1.0F, 1.0F).clientTrackingRange(8).updateInterval(3),
			ShockCubeEntity.createAttributes());
		THUNDERBALL = Registration.entity(ShockCubeIds.Entities.THUNDERBALL_PROJECTILE,
			EntityType.Builder.<ThunderballProjectile>of(ThunderballProjectile::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(1));

		BOTTLE_OF_ELECTRICITY = Registration.item(ShockCubeIds.Items.BOTTLEO_ELECTRICITY, BottleOfElectricityItem::new,
			Tooltips.describe(new Item.Properties().stacksTo(1)
				.food(new FoodProperties.Builder().nutrition(0).saturationModifier(0.0F).alwaysEdible().build(),
					Consumables.defaultDrink().consumeSeconds(1.0F).build()),
				ShockCubeIds.Items.BOTTLEO_ELECTRICITY.getPath(), 1));
		TASER = Registration.item(ShockCubeIds.Items.TASER, TaserItem::new, new Item.Properties().durability(150).rarity(Rarity.UNCOMMON));
		TAZEGUN = Registration.item(ShockCubeIds.Items.TAZEGUN, TazegunItem::new, new Item.Properties().durability(250).rarity(Rarity.UNCOMMON));

		ElectrocutedEffect.registerEvents();
		TaserItem.registerEvents();
	}
}
