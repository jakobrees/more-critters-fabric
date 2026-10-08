package com.morecritters.fabric.module.bunbug;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Spawns;
import com.morecritters.fabric.ids.BlubberfishIds;
import com.morecritters.fabric.ids.BouncelizardIds;
import com.morecritters.fabric.ids.BunbugIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;

/** The bunbug: a decoratable desert bug, its grubs, and the food made from it. */
public final class BunbugModule implements Module {
	public static EntityType<BunbugEntity> BUNBUG;
	public static EntityType<BabyBunbugEntity> BABY_BUNBUG;

	static SoundEvent HURT_SOUND, DEATH_SOUND, STEP_SOUND, CREAM_SOUND, SPRINKLE_SOUND, BERRY_SOUND, DIG_SOUND, SHED_SOUND;

	@Override
	public void register() {
		HURT_SOUND = Registration.sound(BunbugIds.Sounds.ENTITY_BUNBUG_HURT);
		DEATH_SOUND = Registration.sound(BunbugIds.Sounds.ENTITY_BUNBUG_DEATH);
		STEP_SOUND = Registration.sound(BunbugIds.Sounds.ENTITY_BUNBUG_STEP);
		CREAM_SOUND = Registration.sound(BunbugIds.Sounds.ENTITY_BUNBUG_CREAM);
		SPRINKLE_SOUND = Registration.sound(BunbugIds.Sounds.ENTITY_BUNBUG_SPRINKLE);
		BERRY_SOUND = Registration.sound(BunbugIds.Sounds.ENTITY_BUNBUG_BERRY);
		DIG_SOUND = Registration.sound(BunbugIds.Sounds.ENTITY_BUNBUG_DIG);
		SHED_SOUND = Registration.sound(BunbugIds.Sounds.ENTITY_BUNBUG_SHED);

		BUNBUG = Registration.livingEntity(BunbugIds.Entities.BUNBUG,
			EntityType.Builder.of(BunbugEntity::new, MobCategory.AMBIENT).sized(1.0F, 0.7F).clientTrackingRange(8).updateInterval(3),
			BunbugEntity.createAttributes());
		// The original registers the grub as a monster; kept so it despawns and counts the same way.
		BABY_BUNBUG = Registration.livingEntity(BunbugIds.Entities.BABY_BUNBUG,
			EntityType.Builder.of(BabyBunbugEntity::new, MobCategory.MONSTER).sized(0.4F, 0.2F).clientTrackingRange(8).updateInterval(3),
			BabyBunbugEntity.createAttributes());

		Registration.spawnEgg(BunbugIds.Items.BUNBUG_SPAWN_EGG, BUNBUG);
		BunbugItems.register();

		Spawns.onSurface(BUNBUG);
		Spawns.inBiomeTag(BUNBUG, MobCategory.AMBIENT, 1, 1, 2, "more_critters:spawns/bunbug");
	}

	// Items other modules own, looked up by name so this module compiles on its own.
	static Item sprinklesItem() { return BuiltInRegistries.ITEM.getValue(BlubberfishIds.Items.SPRINKLES); }
	static Item bounceberryItem() { return BuiltInRegistries.ITEM.getValue(BouncelizardIds.Items.BOUNCEBERRY); }
	static Item rawMeatItem() { return BunbugItems.rawMeat; }
}
