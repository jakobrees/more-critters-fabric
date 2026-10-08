package com.morecritters.fabric.module.kelpire;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Spawns;
import com.morecritters.fabric.ids.KelpireIds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;

/** The kelpire: a tameable kelp-lurking sea predator, and the kelpire rolls made from it. */
public final class KelpireModule implements Module {
	public static EntityType<KelpireEntity> KELPIRE;
	public static KelpireRollsBlock ROLLS;
	public static Item ROLL_PIECE;

	static SoundEvent IDLE_SOUND, HURT_SOUND, DEATH_SOUND, BITE_SOUND, BURP_SOUND;

	@Override
	public void register() {
		IDLE_SOUND = Registration.sound(KelpireIds.Sounds.ENTITY_KELPIRE_IDLE);
		HURT_SOUND = Registration.sound(KelpireIds.Sounds.ENTITY_KELPIRE_HURT);
		DEATH_SOUND = Registration.sound(KelpireIds.Sounds.ENTITY_KELPIRE_DEATH);
		BITE_SOUND = Registration.sound(KelpireIds.Sounds.ENTITY_KELPIRE_BITE);
		BURP_SOUND = Registration.sound(KelpireIds.Sounds.ENTITY_KELPIRE_BURP);

		KELPIRE = Registration.livingEntity(KelpireIds.Entities.KELPIRE,
			EntityType.Builder.of(KelpireEntity::new, MobCategory.WATER_CREATURE).sized(1.5F, 1.3F).clientTrackingRange(8).updateInterval(3),
			KelpireEntity.createAttributes());
		Registration.spawnEgg(KelpireIds.Items.KELPIRE_SPAWN_EGG, KELPIRE);

		ROLL_PIECE = Registration.item(KelpireIds.Items.KELPIRE_ROLL_PIECE,
			new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.3F).build()));
		ROLLS = Registration.blockWithItem(KelpireIds.Blocks.KELPIRE_ROLLS, KelpireRollsBlock::new,
			BlockBehaviour.Properties.of().sound(SoundType.MOSS).instabreak().noOcclusion().isRedstoneConductor((state, level, pos) -> false));

		// Spawns only with water at its feet and above its head.
		SpawnPlacements.register(KELPIRE, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> level.getBlockState(pos).is(Blocks.WATER) && level.getBlockState(pos.above()).is(Blocks.WATER));
		Spawns.inBiomes(KELPIRE, MobCategory.WATER_CREATURE, 10, 1, 1, "minecraft:deep_lukewarm_ocean", "minecraft:lukewarm_ocean");
	}
}
