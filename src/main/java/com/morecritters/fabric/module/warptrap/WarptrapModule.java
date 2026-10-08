package com.morecritters.fabric.module.warptrap;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Spawns;
import com.morecritters.fabric.ids.WarptrapIds;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;

/** The warptrap: a nylium-burrowing ambusher of the warped forest that preys on Endermen. */
public final class WarptrapModule implements Module {
	public static EntityType<WarptrapEntity> WARPTRAP;
	public static Holder<MobEffect> DIGGER;

	static SoundEvent IDLE_SOUND, HURT_SOUND, DEATH_SOUND, BITE_SOUND, DIG_SOUND;

	@Override
	public void register() {
		IDLE_SOUND = Registration.sound(WarptrapIds.Sounds.ENTITY_WARPTRAP_IDLE);
		HURT_SOUND = Registration.sound(WarptrapIds.Sounds.ENTITY_WARPTRAP_HURT);
		DEATH_SOUND = Registration.sound(WarptrapIds.Sounds.ENTITY_WARPTRAP_DEATH);
		BITE_SOUND = Registration.sound(WarptrapIds.Sounds.ENTITY_WARPTRAP_BITE);
		DIG_SOUND = Registration.sound(WarptrapIds.Sounds.ENTITY_WARPTRAP_DIG);

		DIGGER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, WarptrapIds.Effects.WARPTRAP_DIGGER, new WarptrapDiggerEffect());

		WARPTRAP = Registration.livingEntity(WarptrapIds.Entities.WARPTRAP,
			EntityType.Builder.of(WarptrapEntity::new, MobCategory.MONSTER).fireImmune().sized(1.3F, 0.6F).clientTrackingRange(8).updateInterval(3),
			WarptrapEntity.createAttributes());
		Registration.spawnEgg(WarptrapIds.Items.WARPTRAP_SPAWN_EGG, WARPTRAP);

		// On the ground, in the dark, never on peaceful.
		SpawnPlacements.register(WARPTRAP, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
		Spawns.inBiomeTag(WARPTRAP, MobCategory.MONSTER, 10, 2, 3, "more_critters:spawns/warptrap");
	}
}
