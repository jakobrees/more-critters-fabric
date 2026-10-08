package com.morecritters.fabric.module.custodian;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.CustodianIds;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;

/** The custodian, its dormant ancient form, the core that builds one, sculk essence and the under_control effect. */
public final class CustodianModule implements Module {
	public static EntityType<CustodianEntity> CUSTODIAN;
	public static EntityType<AncientCustodianEntity> ANCIENT_CUSTODIAN;
	public static SimpleParticleType LASER_PARTICLE;
	public static Holder<MobEffect> UNDER_CONTROL;
	static Item SCULK_ESSENCE;

	static SoundEvent HURT_SOUND, DEATH_SOUND, LASER_START_SOUND, LASER_SHOOT_WARDEN_SOUND, SHOOT_SOUND, CLOSE_SOUND, OPEN_SOUND, SPIN_SOUND;

	@Override
	public void register() {
		registerSounds();

		CUSTODIAN = Registration.livingEntity(CustodianIds.Entities.CUSTODIAN,
			EntityType.Builder.of(CustodianEntity::new, MobCategory.MONSTER).sized(1.7F, 3.0F).clientTrackingRange(8).updateInterval(3).fireImmune(),
			CustodianEntity.createAttributes());
		ANCIENT_CUSTODIAN = Registration.livingEntity(CustodianIds.Entities.ANCIENT_CUSTODIAN,
			EntityType.Builder.of(AncientCustodianEntity::new, MobCategory.MONSTER).sized(1.7F, 3.0F).clientTrackingRange(8).updateInterval(3).fireImmune(),
			CustodianEntity.createAttributes());

		Registration.spawnEgg(CustodianIds.Items.CUSTODIAN_SPAWN_EGG, CUSTODIAN);
		SCULK_ESSENCE = Registration.item(CustodianIds.Items.SCULK_ESSENCE, new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
		Registration.item(CustodianIds.Items.ANCIENT_CUSTODIAN_SPAWN_DOLL, AncientCustodianSpawnDollItem::new,
			Tooltips.describe(new Item.Properties(), "ancient_custodian_spawn_doll", 3));

		Registration.blockWithItem(CustodianIds.Blocks.CUSTODIAN_CORE, CustodianCoreBlock::new,
			BlockBehaviour.Properties.of().sound(SoundType.DEEPSLATE_BRICKS).strength(2.0F).requiresCorrectToolForDrops());

		LASER_PARTICLE = Particles.simple(CustodianIds.Particles.CUSTODIAN_LAZER, true);
		UNDER_CONTROL = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, CustodianIds.Effects.UNDER_CONTROL, new UnderControlEffect());

		// Custodians are not natural spawns: a ruined custodian base generates in the Deep Dark.
		BiomeModifications.addFeature(BiomeSelectors.includeByKey(Biomes.DEEP_DARK), GenerationStep.Decoration.TOP_LAYER_MODIFICATION,
			ResourceKey.create(Registries.PLACED_FEATURE, MoreCritters.id("custodian_base_spawn")));
	}

	private static void registerSounds() {
		HURT_SOUND = Registration.sound(CustodianIds.Sounds.ENTITY_CUSTODIAN_HURT);
		DEATH_SOUND = Registration.sound(CustodianIds.Sounds.ENTITY_CUSTODIAN_DEATH);
		LASER_START_SOUND = Registration.sound(CustodianIds.Sounds.ENTITY_CUSTODIAN_LASER_START);
		LASER_SHOOT_WARDEN_SOUND = Registration.sound(CustodianIds.Sounds.ENTITY_CUSTODIAN_LASER_SHOOT_WARDEN);
		SHOOT_SOUND = Registration.sound(CustodianIds.Sounds.ENTITY_CUSTODIAN_SHOOT);
		CLOSE_SOUND = Registration.sound(CustodianIds.Sounds.ENTITY_CUSTODIAN_CLOSE);
		OPEN_SOUND = Registration.sound(CustodianIds.Sounds.ENTITY_CUSTODIAN_OPEN);
		SPIN_SOUND = Registration.sound(CustodianIds.Sounds.ENTITY_CUSTODIAN_SPIN);
		// Registered as in the original, which never plays it.
		Registration.sound(CustodianIds.Sounds.ENTITY_CUSTODIAN_STEP);
	}
}
