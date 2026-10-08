package com.morecritters.fabric.module.critterlings_c;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.ids.CritterlingsCIds;
import com.morecritters.fabric.module.critterling_system.CritterlingRarity;
import com.morecritters.fabric.module.critterling_system.CritterlingSackItem;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Rarity;

/**
 * Four critterlings: the mothkid (hops into short flights), the gillmunch, dominic and olmer,
 * each with its normal, rare and epic sack. None spawns naturally; they come out of sacks.
 */
public final class CritterlingsCModule implements Module {
	public static EntityType<MothkidEntity> MOTHKID;
	public static EntityType<GillmunchEntity> GILLMUNCH;
	public static EntityType<DominicEntity> DOMINIC;
	public static EntityType<OlmerEntity> OLMER;

	static SoundEvent MOTHKID_IDLE, MOTHKID_HURT, MOTHKID_DEATH, MOTHKID_FLY;
	static SoundEvent GILLMUNCH_IDLE, GILLMUNCH_HURT, GILLMUNCH_DEATH;
	static SoundEvent DOMINIC_IDLE, DOMINIC_HURT;
	static SoundEvent OLMER_HURT;

	@Override
	public void register() {
		MOTHKID_IDLE = Registration.sound(CritterlingsCIds.Sounds.ENTITY_MOTHKID_IDLE);
		MOTHKID_HURT = Registration.sound(CritterlingsCIds.Sounds.ENTITY_MOTHKID_HURT);
		MOTHKID_DEATH = Registration.sound(CritterlingsCIds.Sounds.ENTITY_MOTHKID_DEATH);
		MOTHKID_FLY = Registration.sound(CritterlingsCIds.Sounds.ENTITY_MOTHKID_FLY);
		GILLMUNCH_IDLE = Registration.sound(CritterlingsCIds.Sounds.ENTITY_GILLMUNCH_IDLE);
		GILLMUNCH_HURT = Registration.sound(CritterlingsCIds.Sounds.ENTITY_GILLMUNCH_HURT);
		GILLMUNCH_DEATH = Registration.sound(CritterlingsCIds.Sounds.ENTITY_GILLMUNCH_DEATH);
		DOMINIC_IDLE = Registration.sound(CritterlingsCIds.Sounds.ENTITY_DOMINIC_IDLE);
		DOMINIC_HURT = Registration.sound(CritterlingsCIds.Sounds.ENTITY_DOMINIC_HURT);
		OLMER_HURT = Registration.sound(CritterlingsCIds.Sounds.ENTITY_OLMER_HURT);

		MOTHKID = Registration.livingEntity(CritterlingsCIds.Entities.MOTHKID,
			EntityType.Builder.of(MothkidEntity::new, MobCategory.MONSTER).sized(0.6F, 0.6F).clientTrackingRange(8).updateInterval(3),
			MothkidEntity.createAttributes());
		GILLMUNCH = Registration.livingEntity(CritterlingsCIds.Entities.GILLMUNCH,
			EntityType.Builder.of(GillmunchEntity::new, MobCategory.MONSTER).sized(0.6F, 0.6F).clientTrackingRange(8).updateInterval(3),
			GillmunchEntity.createAttributes());
		DOMINIC = Registration.livingEntity(CritterlingsCIds.Entities.DOMINIC,
			EntityType.Builder.of(DominicEntity::new, MobCategory.MONSTER).sized(0.6F, 0.6F).clientTrackingRange(8).updateInterval(3),
			DominicEntity.createAttributes());
		OLMER = Registration.livingEntity(CritterlingsCIds.Entities.OLMER,
			EntityType.Builder.of(OlmerEntity::new, MobCategory.MONSTER).sized(0.6F, 0.6F).clientTrackingRange(8).updateInterval(3),
			OlmerEntity.createAttributes());

		// Item rarities as in the original's sack classes (here the usual uncommon/rare/epic).
		CritterlingSackItem.register(CritterlingsCIds.Items.CRITTERLING_SACK_MOTHKID, MOTHKID, CritterlingRarity.NORMAL, Rarity.UNCOMMON);
		CritterlingSackItem.register(CritterlingsCIds.Items.CRITTERLING_SACK_MOTHKID_RARE, MOTHKID, CritterlingRarity.RARE, Rarity.RARE);
		CritterlingSackItem.register(CritterlingsCIds.Items.CRITTERLING_SACK_MOTHKID_EPIC, MOTHKID, CritterlingRarity.EPIC, Rarity.EPIC);
		CritterlingSackItem.register(CritterlingsCIds.Items.CRITTERLING_SACK_GILLMUNCH, GILLMUNCH, CritterlingRarity.NORMAL, Rarity.UNCOMMON);
		CritterlingSackItem.register(CritterlingsCIds.Items.CRITTERLING_SACK_GILLMUNCH_RARE, GILLMUNCH, CritterlingRarity.RARE, Rarity.RARE);
		CritterlingSackItem.register(CritterlingsCIds.Items.CRITTERLING_SACK_GILLMUNCH_EPIC, GILLMUNCH, CritterlingRarity.EPIC, Rarity.EPIC);
		CritterlingSackItem.register(CritterlingsCIds.Items.CRITTERLING_SACK_DOMINIC, DOMINIC, CritterlingRarity.NORMAL, Rarity.UNCOMMON);
		CritterlingSackItem.register(CritterlingsCIds.Items.CRITTERLING_SACK_DOMINIC_RARE, DOMINIC, CritterlingRarity.RARE, Rarity.RARE);
		CritterlingSackItem.register(CritterlingsCIds.Items.CRITTERLING_SACK_DOMINIC_EPIC, DOMINIC, CritterlingRarity.EPIC, Rarity.EPIC);
		CritterlingSackItem.register(CritterlingsCIds.Items.CRITTERLING_SACK_OLMER, OLMER, CritterlingRarity.NORMAL, Rarity.UNCOMMON);
		CritterlingSackItem.register(CritterlingsCIds.Items.CRITTERLING_SACK_OLMER_RARE, OLMER, CritterlingRarity.RARE, Rarity.RARE);
		CritterlingSackItem.register(CritterlingsCIds.Items.CRITTERLING_SACK_OLMER_EPIC, OLMER, CritterlingRarity.EPIC, Rarity.EPIC);
	}
}
