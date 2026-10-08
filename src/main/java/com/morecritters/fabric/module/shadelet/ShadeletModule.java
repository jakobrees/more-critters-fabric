package com.morecritters.fabric.module.shadelet;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Spawns;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.ShadeletIds;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;

/** The shadelet, its Spooked scream, and the tooth items: syringe, chattering teeth, tooth melter, shark tooth. */
public final class ShadeletModule implements Module {
	public static EntityType<ShadeletEntity> SHADELET;
	public static EntityType<ChatteringTeethEntity> CHATTERING_TEETH;
	public static Item TOOTH_MELTER, SHARK_TOOTH, TOOTH_SYRINGE, CHATTERING_TEETH_ITEM;
	public static Holder<MobEffect> SPOOKED;
	static SoundEvent IDLE_SOUND, HURT_SOUND, DEATH_SOUND, SCREAM_SOUND, SYRINGE_SOUND, TEETH_START_SOUND, TEETH_END_SOUND, TEETH_STEP_SOUND;

	@Override
	public void register() {
		IDLE_SOUND = Registration.sound(ShadeletIds.Sounds.ENTITY_SHADELET_IDLE);
		HURT_SOUND = Registration.sound(ShadeletIds.Sounds.ENTITY_SHADELET_HURT);
		DEATH_SOUND = Registration.sound(ShadeletIds.Sounds.ENTITY_SHADELET_DEATH);
		SCREAM_SOUND = Registration.sound(ShadeletIds.Sounds.ENTITY_SHADELET_SCREAM);
		SYRINGE_SOUND = Registration.sound(ShadeletIds.Sounds.ITEM_TOOTH_SYRINGE_USE);
		TEETH_START_SOUND = Registration.sound(ShadeletIds.Sounds.ENTITY_CHATTERING_TEETH_START);
		TEETH_END_SOUND = Registration.sound(ShadeletIds.Sounds.ENTITY_CHATTERING_TEETH_END);
		TEETH_STEP_SOUND = Registration.sound(ShadeletIds.Sounds.ENTITY_CHATTERING_TEETH_STEP);

		SPOOKED = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, ShadeletIds.Effects.SPOOKED, new SpookedEffect());

		SHADELET = Registration.livingEntity(ShadeletIds.Entities.SHADELET,
			EntityType.Builder.of(ShadeletEntity::new, MobCategory.MONSTER).sized(0.6F, 1.0F).fireImmune().clientTrackingRange(8).updateInterval(3),
			ShadeletEntity.createAttributes());
		CHATTERING_TEETH = Registration.livingEntity(ShadeletIds.Entities.CHATTERING_TEETH,
			EntityType.Builder.of(ChatteringTeethEntity::new, MobCategory.MONSTER).sized(0.7F, 0.7F).fireImmune().clientTrackingRange(8).updateInterval(3),
			ChatteringTeethEntity.createAttributes());

		Registration.spawnEgg(ShadeletIds.Items.SHADELET_SPAWN_EGG, SHADELET);
		SHARK_TOOTH = Registration.item(ShadeletIds.Items.SHARK_TOOTH, new Item.Properties());
		TOOTH_MELTER = Registration.item(ShadeletIds.Items.TOOTH_MELTER, new Item.Properties().stacksTo(1)
			.food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.3F).build()).usingConvertsTo(Items.BOWL));
		TOOTH_SYRINGE = Registration.item(ShadeletIds.Items.TOOTH_SYRINGE, ShadeletItems.ToothSyringeItem::new,
			Tooltips.describe(new Item.Properties().stacksTo(16), "tooth_syringe", 2));
		CHATTERING_TEETH_ITEM = Registration.item(ShadeletIds.Items.CHATTERING_TEETH_ITEM, ShadeletItems.ChatteringTeethItem::new,
			new Item.Properties().stacksTo(2));

		// Dark-forest monster rules, but the shadelet is not a Monster subclass, so the rule is spelled out.
		SpawnPlacements.register(SHADELET, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> level.getDifficulty() != Difficulty.PEACEFUL
				&& Monster.isDarkEnoughToSpawn(level, pos, random)
				&& Mob.checkMobSpawnRules(type, level, reason, pos, random));
		Spawns.inBiomeTag(SHADELET, MobCategory.MONSTER, 15, 1, 1, "more_critters:spawns/shadelet");
	}
}
