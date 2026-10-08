package com.morecritters.fabric.module.critterlings_b;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.ids.CritterlingsBIds;
import com.morecritters.fabric.module.critterling_system.CritterlingRarity;
import com.morecritters.fabric.module.critterling_system.CritterlingSackItem;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Rarity;

/** Four critterlings: the floating expy, the hopping scowl, the rolling rollball and the opalcrab, each with three sacks. */
public final class CritterlingsBModule implements Module {
	public static SoundEvent EXPY_HURT, EXPY_DEATH;
	public static SoundEvent SCOWL_IDLE, SCOWL_HURT, SCOWL_FLY;
	public static SoundEvent ROLLBALL_HURT, ROLLBALL_ROLL, ROLLBALL_UNROLL;
	public static SoundEvent OPALCRAB_HURT;

	public static EntityType<ExpyEntity> EXPY;
	public static EntityType<ScowlEntity> SCOWL;
	public static EntityType<RollballEntity> ROLLBALL;
	public static EntityType<OpalcrabEntity> OPALCRAB;

	@Override
	public void register() {
		EXPY_HURT = Registration.sound(CritterlingsBIds.Sounds.ENTITY_EXPY_HURT);
		EXPY_DEATH = Registration.sound(CritterlingsBIds.Sounds.ENTITY_EXPY_DEATH);
		SCOWL_IDLE = Registration.sound(CritterlingsBIds.Sounds.ENTITY_SCOWL_IDLE);
		SCOWL_HURT = Registration.sound(CritterlingsBIds.Sounds.ENTITY_SCOWL_HURT);
		SCOWL_FLY = Registration.sound(CritterlingsBIds.Sounds.ENTITY_SCOWL_FLY);
		ROLLBALL_HURT = Registration.sound(CritterlingsBIds.Sounds.ENTITY_ROLLBALL_HURT);
		ROLLBALL_ROLL = Registration.sound(CritterlingsBIds.Sounds.ENTITY_ROLLBALL_ROLL);
		ROLLBALL_UNROLL = Registration.sound(CritterlingsBIds.Sounds.ENTITY_ROLLBALL_UNROLL);
		OPALCRAB_HURT = Registration.sound(CritterlingsBIds.Sounds.OPALCRAB_HURT);

		EXPY = Registration.livingEntity(CritterlingsBIds.Entities.EXPY, critterling(ExpyEntity::new), ExpyEntity.createAttributes());
		SCOWL = Registration.livingEntity(CritterlingsBIds.Entities.SCOWL, critterling(ScowlEntity::new), ScowlEntity.createAttributes());
		ROLLBALL = Registration.livingEntity(CritterlingsBIds.Entities.ROLLBALL, critterling(RollballEntity::new), RollballEntity.createAttributes());
		OPALCRAB = Registration.livingEntity(CritterlingsBIds.Entities.OPALCRAB, critterling(OpalcrabEntity::new), OpalcrabEntity.createAttributes());

		// All twelve sacks use the plain uncommon / rare / epic item rarities.
		sacks(EXPY, CritterlingsBIds.Items.CRITTERLING_SACK_EXPY, CritterlingsBIds.Items.CRITTERLING_SACK_EXPY_RARE, CritterlingsBIds.Items.CRITTERLING_SACK_EXPY_EPIC);
		sacks(SCOWL, CritterlingsBIds.Items.CRITTERLING_SACK_SCOWL, CritterlingsBIds.Items.CRITTERLING_SACK_SCOWL_RARE, CritterlingsBIds.Items.CRITTERLING_SACK_SCOWL_EPIC);
		sacks(ROLLBALL, CritterlingsBIds.Items.CRITTERLING_SACK_ROLLBALL, CritterlingsBIds.Items.CRITTERLING_SACK_ROLLBALL_RARE, CritterlingsBIds.Items.CRITTERLING_SACK_ROLLBALL_EPIC);
		sacks(OPALCRAB, CritterlingsBIds.Items.CRITTERLING_SACK_OPALCRAB, CritterlingsBIds.Items.CRITTERLING_SACK_OPALCRAB_RARE, CritterlingsBIds.Items.CRITTERLING_SACK_OPALCRAB_EPIC);
	}

	/** All four are 0.4 x 0.4 and in the monster category, as in the original (they never despawn). */
	private static <T extends Mob> EntityType.Builder<T> critterling(EntityType.EntityFactory<T> factory) {
		return EntityType.Builder.of(factory, MobCategory.MONSTER).sized(0.4F, 0.4F).clientTrackingRange(8).updateInterval(3);
	}

	private static void sacks(EntityType<? extends Mob> type, Identifier normal, Identifier rare, Identifier epic) {
		CritterlingSackItem.register(normal, type, CritterlingRarity.NORMAL, Rarity.UNCOMMON);
		CritterlingSackItem.register(rare, type, CritterlingRarity.RARE, Rarity.RARE);
		CritterlingSackItem.register(epic, type, CritterlingRarity.EPIC, Rarity.EPIC);
	}
}
