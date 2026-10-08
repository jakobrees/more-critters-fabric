package com.morecritters.fabric.module.armossillo;

import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Spawns;
import com.morecritters.fabric.ids.ArmossilloIds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/** The armossillo and its baby: entity types, sounds, spawn egg and spawns. */
public final class ArmossilloEntities {
	public static EntityType<ArmossilloEntity> ARMOSSILLO;
	public static EntityType<BabyArmossilloEntity> BABY_ARMOSSILLO;

	static SoundEvent IDLE_SOUND, HURT_SOUND, DEATH_SOUND, SIT_SOUND, RISE_SOUND, SNEEZE_READY_SOUND, SNEEZE_SOUND;
	static SoundEvent BABY_IDLE_SOUND, BABY_HURT_SOUND, BABY_DEATH_SOUND;

	static void register() {
		registerSounds();

		ARMOSSILLO = Registration.livingEntity(ArmossilloIds.Entities.ARMOSSILLO,
			EntityType.Builder.of(ArmossilloEntity::new, MobCategory.AMBIENT).sized(1.5F, 1.7F).clientTrackingRange(8).updateInterval(3),
			ArmossilloEntity.createAttributes());
		// The original registers the baby as a monster; kept so it counts the same way (it never despawns anyway).
		BABY_ARMOSSILLO = Registration.livingEntity(ArmossilloIds.Entities.BABY_ARMOSSILLO,
			EntityType.Builder.of(BabyArmossilloEntity::new, MobCategory.MONSTER).sized(0.6F, 0.6F).clientTrackingRange(8).updateInterval(3),
			BabyArmossilloEntity.createAttributes());

		Registration.spawnEgg(ArmossilloIds.Items.ARMOSSILLO_SPAWN_EGG, ARMOSSILLO);

		SpawnPlacements.register(ARMOSSILLO, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> isCaveSpawnSpot(level, pos));
		Spawns.inBiomeTag(ARMOSSILLO, MobCategory.AMBIENT, 4, 3, 4, "more_critters:spawns/armossillo");
	}

	private static void registerSounds() {
		IDLE_SOUND = Registration.sound(ArmossilloIds.Sounds.ENTITY_ARMOSSILLO_IDLE);
		HURT_SOUND = Registration.sound(ArmossilloIds.Sounds.ENTITY_ARMOSSILLO_HURT);
		DEATH_SOUND = Registration.sound(ArmossilloIds.Sounds.ENTITY_ARMOSSILLO_DEATH);
		SIT_SOUND = Registration.sound(ArmossilloIds.Sounds.ENTITY_ARMOSSILLO_SIT);
		RISE_SOUND = Registration.sound(ArmossilloIds.Sounds.ENTITY_ARMOSSILLO_RISE);
		SNEEZE_READY_SOUND = Registration.sound(ArmossilloIds.Sounds.ENTITY_ARMOSSILLO_SNEEZE_READY);
		SNEEZE_SOUND = Registration.sound(ArmossilloIds.Sounds.ENTITY_ARMOSSILLO_SNEEZE);
		BABY_IDLE_SOUND = Registration.sound(ArmossilloIds.Sounds.ENTITY_BABY_ARMOSSILLO_IDLE);
		BABY_HURT_SOUND = Registration.sound(ArmossilloIds.Sounds.ENTITY_BABY_ARMOSSILLO_HURT);
		BABY_DEATH_SOUND = Registration.sound(ArmossilloIds.Sounds.ENTITY_BABY_ARMOSSILLO_DEATH);
	}

	/** Armossillos spawn anywhere out of water where the sky cannot be seen: under ground, in the lush caves. */
	private static boolean isCaveSpawnSpot(LevelReader level, BlockPos pos) {
		return !level.getBlockState(pos).is(Blocks.WATER) && !level.canSeeSkyFromBelowWater(pos);
	}

	private ArmossilloEntities() {}
}
