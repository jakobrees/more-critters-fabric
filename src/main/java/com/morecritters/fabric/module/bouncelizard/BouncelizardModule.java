package com.morecritters.fabric.module.bouncelizard;

import com.morecritters.fabric.ids.CritterlingSystemIds;
import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Spawns;
import com.morecritters.fabric.ids.BouncelizardIds;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/** The bouncelizard: a jungle trampoline that naps on bounceberries and lays eggs; its bushes, food and the Leaper effect. */
public final class BouncelizardModule implements Module {
	public static EntityType<BouncelizardEntity> BOUNCELIZARD;
	public static BouncelizardEggBlock EGG;
	public static BlockEntityType<BouncelizardEggBlockEntity> EGG_BLOCK_ENTITY;
	public static BounceberryBushBlock BUSH;
	public static BounceberryBushBlock EMPTY_BUSH;
	public static MobEffect LEAPER;

	static SoundEvent IDLE_SOUND, HURT_SOUND, DEATH_SOUND, SNORE_SOUND, SNORE_MIMIMI_SOUND, BOUNCE_SOUND, BIG_BOUNCE_SOUND;

	@Override
	public void register() {
		IDLE_SOUND = Registration.sound(BouncelizardIds.Sounds.ENTITY_BOUNCELIZARD_IDLE);
		HURT_SOUND = Registration.sound(BouncelizardIds.Sounds.ENTITY_BOUNCELIZARD_HURT);
		DEATH_SOUND = Registration.sound(BouncelizardIds.Sounds.ENTITY_BOUNCELIZARD_DEATH);
		SNORE_SOUND = Registration.sound(BouncelizardIds.Sounds.ENTITY_BOUNCELIZARD_SNORE);
		SNORE_MIMIMI_SOUND = Registration.sound(BouncelizardIds.Sounds.ENTITY_BOUNCELIZARD_SNORE_MIMIMI);
		BOUNCE_SOUND = Registration.sound(BouncelizardIds.Sounds.ENTITY_BOUNCELIZARD_BOUNCE);
		BIG_BOUNCE_SOUND = Registration.sound(BouncelizardIds.Sounds.ENTITY_BOUNCELIZARD_BIG_BOUNCE);

		BOUNCELIZARD = Registration.livingEntity(BouncelizardIds.Entities.BOUNCELIZARD,
			EntityType.Builder.of(BouncelizardEntity::new, MobCategory.CREATURE).sized(1.0F, 0.3F).clientTrackingRange(8).updateInterval(3),
			BouncelizardEntity.createAttributes());
		Registration.spawnEgg(BouncelizardIds.Items.BOUNCELIZARD_SPAWN_EGG, BOUNCELIZARD);

		EGG = Registration.blockWithItem(BouncelizardIds.Blocks.BOUNCELIZARD_EGG, BouncelizardEggBlock::new,
			BlockBehaviour.Properties.of().sound(SoundType.STONE).strength(0.5F).noOcclusion().isRedstoneConductor((state, level, pos) -> false));
		EGG_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, BouncelizardIds.BlockEntities.BOUNCELIZARD_EGG,
			new BlockEntityType<>(BouncelizardEggBlockEntity::new, Set.of(EGG)));

		BUSH = Registration.blockWithItem(BouncelizardIds.Blocks.BOUNCEBERRY_BUSH,
			properties -> new BounceberryBushBlock(properties, true), bushProperties());
		EMPTY_BUSH = Registration.blockWithItem(BouncelizardIds.Blocks.BOUNCEBERRY_BUSH_EMPTY,
			properties -> new BounceberryBushBlock(properties, false), bushProperties().randomTicks());

		BouncelizardItems.register();
		LEAPER = Registry.register(BuiltInRegistries.MOB_EFFECT, BouncelizardIds.Effects.LEAPER, new LeaperEffect());

		SpawnPlacements.register(BOUNCELIZARD, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BouncelizardEntity::canSpawnAt);
		Spawns.inBiomeTag(BOUNCELIZARD, MobCategory.CREATURE, 15, 2, 5, "more_critters:spawns/bouncelizard");
	}

	private static BlockBehaviour.Properties bushProperties() {
		return BlockBehaviour.Properties.of().sound(SoundType.GRASS).strength(0.1F).noOcclusion().isRedstoneConductor((state, level, pos) -> false);
	}

	// Particles the critterlings module owns, looked up by name so this module works on its own; null while it is a stub.
	static @Nullable ParticleOptions boostParticle() { return particle(CritterlingSystemIds.Particles.BOOST); }
	static @Nullable ParticleOptions sleepParticle() { return particle(CritterlingSystemIds.Particles.ZZZ); }

	private static @Nullable ParticleOptions particle(Identifier id) {
		ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.getValue(id);
		return type instanceof ParticleOptions options ? options : null;
	}
}
