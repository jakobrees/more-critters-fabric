package com.morecritters.fabric.module.critterlings_d;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.ids.CritterlingsDIds;
import com.morecritters.fabric.module.critterling_system.CritterlingRarity;
import com.morecritters.fabric.module.critterling_system.CritterlingSackItem;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Rarity;

/**
 * Four critterlings: the flarg (with its flames and the {@code flarg_flame} particle), the stalk,
 * the piranheed and the mangotrice, each with a normal, rare and epic critterling sack. None of
 * them spawns naturally (the original had no biome modifier for them).
 */
public final class CritterlingsDModule implements Module {
	public static EntityType<FlargEntity> FLARG;
	public static EntityType<StalkEntity> STALK;
	public static EntityType<PiranheedEntity> PIRANHEED;
	public static EntityType<MangotriceEntity> MANGOTRICE;

	public static SimpleParticleType FLARG_FLAME;

	static SoundEvent FLARG_IDLE_SOUND, FLARG_HURT_SOUND;
	static SoundEvent STALK_IDLE_SOUND, STALK_HURT_SOUND;
	static SoundEvent PIRANHEED_IDLE_SOUND, PIRANHEED_HURT_SOUND;
	static SoundEvent MANGOTRICE_IDLE_SOUND, MANGOTRICE_HURT_SOUND;

	@Override
	public void register() {
		registerSounds();
		FLARG_FLAME = Particles.simple(CritterlingsDIds.Particles.FLARG_FLAME, true);
		registerEntities();
		registerSacks();
	}

	private static void registerSounds() {
		FLARG_IDLE_SOUND = Registration.sound(CritterlingsDIds.Sounds.ENTITY_FLARG_IDLE);
		FLARG_HURT_SOUND = Registration.sound(CritterlingsDIds.Sounds.ENTITY_FLARG_HURT);
		STALK_IDLE_SOUND = Registration.sound(CritterlingsDIds.Sounds.ENTITY_STALK_IDLE);
		STALK_HURT_SOUND = Registration.sound(CritterlingsDIds.Sounds.ENTITY_STALK_HURT);
		PIRANHEED_IDLE_SOUND = Registration.sound(CritterlingsDIds.Sounds.ENTITY_PIRANHEED_IDLE);
		PIRANHEED_HURT_SOUND = Registration.sound(CritterlingsDIds.Sounds.ENTITY_PIRANHEED_HURT);
		MANGOTRICE_IDLE_SOUND = Registration.sound(CritterlingsDIds.Sounds.ENTITY_MANGOTRICE_IDLE);
		MANGOTRICE_HURT_SOUND = Registration.sound(CritterlingsDIds.Sounds.ENTITY_MANGOTRICE_HURT);
	}

	private static void registerEntities() {
		FLARG = Registration.livingEntity(CritterlingsDIds.Entities.FLARG,
			EntityType.Builder.of(FlargEntity::new, MobCategory.MONSTER).sized(0.6F, 0.6F).clientTrackingRange(8).updateInterval(3),
			FlargEntity.createAttributes());
		STALK = Registration.livingEntity(CritterlingsDIds.Entities.STALK,
			EntityType.Builder.of(StalkEntity::new, MobCategory.MONSTER).sized(0.6F, 0.8F).clientTrackingRange(8).updateInterval(3),
			StalkEntity.createAttributes());
		PIRANHEED = Registration.livingEntity(CritterlingsDIds.Entities.PIRANHEED,
			EntityType.Builder.of(PiranheedEntity::new, MobCategory.MONSTER).sized(0.6F, 0.6F).clientTrackingRange(8).updateInterval(3),
			PiranheedEntity.createAttributes());
		MANGOTRICE = Registration.livingEntity(CritterlingsDIds.Entities.MANGOTRICE,
			EntityType.Builder.of(MangotriceEntity::new, MobCategory.MONSTER).sized(0.6F, 0.6F).clientTrackingRange(8).updateInterval(3),
			MangotriceEntity.createAttributes());
	}

	/** Item rarities as the original's sack classes had them: all uncommon except the stalk's rare and epic sacks. */
	private static void registerSacks() {
		CritterlingSackItem.register(CritterlingsDIds.Items.CRITTERLING_SACK_FLARG, FLARG, CritterlingRarity.NORMAL, Rarity.UNCOMMON);
		CritterlingSackItem.register(CritterlingsDIds.Items.CRITTERLING_SACK_FLARG_RARE, FLARG, CritterlingRarity.RARE, Rarity.UNCOMMON);
		CritterlingSackItem.register(CritterlingsDIds.Items.CRITTERLING_SACK_FLARG_EPIC, FLARG, CritterlingRarity.EPIC, Rarity.UNCOMMON);

		CritterlingSackItem.register(CritterlingsDIds.Items.CRITTERLING_SACK_STALK, STALK, CritterlingRarity.NORMAL, Rarity.UNCOMMON);
		CritterlingSackItem.register(CritterlingsDIds.Items.CRITTERLING_SACK_STALK_RARE, STALK, CritterlingRarity.RARE, Rarity.RARE);
		CritterlingSackItem.register(CritterlingsDIds.Items.CRITTERLING_SACK_STALK_EPIC, STALK, CritterlingRarity.EPIC, Rarity.EPIC);

		CritterlingSackItem.register(CritterlingsDIds.Items.CRITTERLING_SACK_PIRANHEED, PIRANHEED, CritterlingRarity.NORMAL, Rarity.UNCOMMON);
		CritterlingSackItem.register(CritterlingsDIds.Items.CRITTERLING_SACK_PIRANHEED_RARE, PIRANHEED, CritterlingRarity.RARE, Rarity.UNCOMMON);
		CritterlingSackItem.register(CritterlingsDIds.Items.CRITTERLING_SACK_PIRANHEED_EPIC, PIRANHEED, CritterlingRarity.EPIC, Rarity.UNCOMMON);

		CritterlingSackItem.register(CritterlingsDIds.Items.CRITTERLING_SACK_MANGOTRICE, MANGOTRICE, CritterlingRarity.NORMAL, Rarity.UNCOMMON);
		CritterlingSackItem.register(CritterlingsDIds.Items.CRITTERLING_SACK_MANGOTRICE_RARE, MANGOTRICE, CritterlingRarity.RARE, Rarity.UNCOMMON);
		CritterlingSackItem.register(CritterlingsDIds.Items.CRITTERLING_SACK_MANGOTRICE_EPIC, MANGOTRICE, CritterlingRarity.EPIC, Rarity.UNCOMMON);
	}
}
