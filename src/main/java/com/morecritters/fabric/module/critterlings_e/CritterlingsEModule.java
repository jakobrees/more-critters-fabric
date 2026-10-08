package com.morecritters.fabric.module.critterlings_e;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.ids.CritterlingsEIds;
import com.morecritters.fabric.module.critterling_system.CritterlingRarity;
import com.morecritters.fabric.module.critterling_system.CritterlingSackItem;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Rarity;

/**
 * Three critterlings: the fresnoid and the cobble, each with its normal, rare and epic sack, and
 * the lightfly, the spark the nightshroom releases (no sack). None spawns naturally.
 */
public final class CritterlingsEModule implements Module {
	public static EntityType<FresnoidEntity> FRESNOID;
	public static EntityType<CobbleEntity> COBBLE;
	public static EntityType<LightflyEntity> LIGHTFLY;

	static SoundEvent FRESNOID_HURT_SOUND, FRESNOID_IDLE_SOUND, FRESNOID_YAWN_SOUND, FRESNOID_YAWN_GOOFY_SOUND, COBBLE_HURT_SOUND;

	@Override
	public void register() {
		FRESNOID_HURT_SOUND = Registration.sound(CritterlingsEIds.Sounds.ENTITY_FRESNOID_HURT);
		FRESNOID_IDLE_SOUND = Registration.sound(CritterlingsEIds.Sounds.ENTITY_FRESNOID_IDLE);
		FRESNOID_YAWN_SOUND = Registration.sound(CritterlingsEIds.Sounds.ENTITY_FRESNOID_YAWN);
		FRESNOID_YAWN_GOOFY_SOUND = Registration.sound(CritterlingsEIds.Sounds.ENTITY_FRESNOID_YAWN_GOOFY);
		COBBLE_HURT_SOUND = Registration.sound(CritterlingsEIds.Sounds.ENTITY_COBBLE_HURT);
		// Played by critterling_system (hit) and the nightshroom (target), which look them up by id.
		Registration.sound(CritterlingsEIds.Sounds.ENTITY_LIGHTFLY_HIT);
		Registration.sound(CritterlingsEIds.Sounds.ENTITY_LIGHTFLY_TARGET);

		FRESNOID = Registration.livingEntity(CritterlingsEIds.Entities.FRESNOID,
			EntityType.Builder.of(FresnoidEntity::new, MobCategory.MONSTER).sized(0.6F, 0.6F).clientTrackingRange(8).updateInterval(3),
			FresnoidEntity.createAttributes());
		COBBLE = Registration.livingEntity(CritterlingsEIds.Entities.COBBLE,
			EntityType.Builder.of(CobbleEntity::new, MobCategory.MONSTER).sized(0.6F, 0.6F).clientTrackingRange(8).updateInterval(3),
			CobbleEntity.createAttributes());
		LIGHTFLY = Registration.livingEntity(CritterlingsEIds.Entities.LIGHTFLY,
			EntityType.Builder.of(LightflyEntity::new, MobCategory.MONSTER).sized(0.5F, 0.5F).fireImmune().clientTrackingRange(8).updateInterval(3),
			LightflyEntity.createAttributes());

		// Item rarities as in the original: every fresnoid sack uncommon, every cobble sack epic.
		CritterlingSackItem.register(CritterlingsEIds.Items.CRITTERLING_SACK_FRESNOID, FRESNOID, CritterlingRarity.NORMAL, Rarity.UNCOMMON);
		CritterlingSackItem.register(CritterlingsEIds.Items.CRITTERLING_SACK_FRESNOID_RARE, FRESNOID, CritterlingRarity.RARE, Rarity.UNCOMMON);
		CritterlingSackItem.register(CritterlingsEIds.Items.CRITTERLING_SACK_FRESNOID_EPIC, FRESNOID, CritterlingRarity.EPIC, Rarity.UNCOMMON);
		CritterlingSackItem.register(CritterlingsEIds.Items.CRITTERLING_SACK_COBBLE, COBBLE, CritterlingRarity.NORMAL, Rarity.EPIC);
		CritterlingSackItem.register(CritterlingsEIds.Items.CRITTERLING_SACK_COBBLE_RARE, COBBLE, CritterlingRarity.RARE, Rarity.EPIC);
		CritterlingSackItem.register(CritterlingsEIds.Items.CRITTERLING_SACK_COBBLE_EPIC, COBBLE, CritterlingRarity.EPIC, Rarity.EPIC);
	}
}
