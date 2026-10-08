package com.morecritters.fabric.module.nauticrawl;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Spawns;
import com.morecritters.fabric.ids.NauticrawlIds;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;

/** The nauticrawl and its zombie, the kelpire's blood bubbles, and the shells, ramen and gear made from nauticrawls. */
public final class NauticrawlModule implements Module {
	public static EntityType<NauticrawlEntity> NAUTICRAWL;
	public static EntityType<ZombieNauticrawlEntity> ZOMBIE_NAUTICRAWL;
	public static EntityType<BubbleEntity> BUBBLE;
	public static NauticrawlShellBlock SHELL, ZOMBIE_SHELL;
	public static NauticrawlRamenBlock RAMEN;
	public static Holder<MobEffect> SWIMMER;

	static SoundEvent IDLE_SOUND, HURT_SOUND, DEATH_SOUND, ATTACK_SOUND, ROLL_SOUND, UNROLL_SOUND, BUBBLE_POP_SOUND,
		RAMEN_EAT_SOUND, RAMEN_DRINK_SOUND, RAMEN_CRUNCH_SOUND;

	@Override
	public void register() {
		IDLE_SOUND = Registration.sound(NauticrawlIds.Sounds.ENTITY_NAUTICRAWL_IDLE);
		HURT_SOUND = Registration.sound(NauticrawlIds.Sounds.ENTITY_NAUTICRAWL_HURT);
		DEATH_SOUND = Registration.sound(NauticrawlIds.Sounds.ENTITY_NAUTICRAWL_DEATH);
		ATTACK_SOUND = Registration.sound(NauticrawlIds.Sounds.ENTITY_NAUTICRAWL_ATTACK);
		ROLL_SOUND = Registration.sound(NauticrawlIds.Sounds.ENTITY_NAUTICRAWL_ROLL);
		UNROLL_SOUND = Registration.sound(NauticrawlIds.Sounds.ENTITY_NAUTICRAWL_UNROLL);
		BUBBLE_POP_SOUND = Registration.sound(NauticrawlIds.Sounds.ENTITY_BLOOD_BUBBLE_POP);
		RAMEN_EAT_SOUND = Registration.sound(NauticrawlIds.Sounds.BLOCK_NAUTICRAWL_RAMEN_EAT);
		RAMEN_DRINK_SOUND = Registration.sound(NauticrawlIds.Sounds.BLOCK_NAUTICRAWL_RAMEN_DRINK);
		RAMEN_CRUNCH_SOUND = Registration.sound(NauticrawlIds.Sounds.BLOCK_NAUTICRAWL_RAMEN_CRUNCH);

		SWIMMER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, NauticrawlIds.Effects.SWIMMER, new SwimmerEffect());

		NAUTICRAWL = Registration.livingEntity(NauticrawlIds.Entities.NAUTICRAWL,
			EntityType.Builder.of(NauticrawlEntity::new, MobCategory.WATER_CREATURE).sized(1.2F, 2.16F).clientTrackingRange(8).updateInterval(3),
			NauticrawlEntity.createAttributes());
		ZOMBIE_NAUTICRAWL = Registration.livingEntity(NauticrawlIds.Entities.ZOMBIE_NAUTICRAWL,
			EntityType.Builder.of(ZombieNauticrawlEntity::new, MobCategory.WATER_CREATURE).sized(1.2F, 2.16F).clientTrackingRange(8).updateInterval(3),
			NauticrawlEntity.createAttributes());
		BUBBLE = Registration.livingEntity(NauticrawlIds.Entities.BUBBLE_ENTITY,
			EntityType.Builder.of(BubbleEntity::new, MobCategory.MONSTER).sized(0.5F, 0.5F).fireImmune().clientTrackingRange(8).updateInterval(3),
			BubbleEntity.createAttributes());
		Registration.spawnEgg(NauticrawlIds.Items.NAUTICRAWL_SPAWN_EGG, NAUTICRAWL);

		NauticrawlItems.register();

		SHELL = Registration.blockWithItem(NauticrawlIds.Blocks.NAUTICRAWL_SHELL, NauticrawlShellBlock::new, shellProperties());
		ZOMBIE_SHELL = Registration.blockWithItem(NauticrawlIds.Blocks.ZOMBIE_NAUTICRAWL_SHELL, NauticrawlShellBlock::new, shellProperties());
		RAMEN = Registration.blockWithItem(NauticrawlIds.Blocks.NAUTICRAWL_RAMEN, NauticrawlRamenBlock::new,
			BlockBehaviour.Properties.of().sound(SoundType.STONE).strength(0.3F).noOcclusion().isRedstoneConductor((state, level, pos) -> false));

		// Spawns only with water at its feet and above its head.
		SpawnPlacements.register(NAUTICRAWL, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> level.getBlockState(pos).is(Blocks.WATER) && level.getBlockState(pos.above()).is(Blocks.WATER));
		Spawns.inBiomeTag(NAUTICRAWL, MobCategory.WATER_CREATURE, 6, 1, 1, "more_critters:spawns/nauticrawl");
	}

	private static BlockBehaviour.Properties shellProperties() {
		return BlockBehaviour.Properties.of().sound(SoundType.STONE).strength(1.0F).requiresCorrectToolForDrops().noOcclusion()
			.isRedstoneConductor((state, level, pos) -> false);
	}
}
