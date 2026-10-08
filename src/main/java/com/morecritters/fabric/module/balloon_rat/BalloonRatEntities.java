package com.morecritters.fabric.module.balloon_rat;

import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Spawns;
import com.morecritters.fabric.ids.BalloonRatIds;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** The balloon rat and pink monster entity types, the balloon rat's spawn egg and its badlands spawns. */
public final class BalloonRatEntities {
	public static EntityType<BalloonRatEntity> BALLOON_RAT;
	public static EntityType<PinkMonsterEntity> PINK_MONSTER;

	static void register() {
		BALLOON_RAT = Registration.livingEntity(BalloonRatIds.Entities.BALLOON_RAT,
				EntityType.Builder.of(BalloonRatEntity::new, MobCategory.AMBIENT).sized(0.8F, 0.8F).clientTrackingRange(8).updateInterval(3),
				BalloonRatEntity.createAttributes());
		PINK_MONSTER = Registration.livingEntity(BalloonRatIds.Entities.PINK_MONSTER,
				EntityType.Builder.of(PinkMonsterEntity::new, MobCategory.MONSTER).sized(1.0F, 1.0F).fireImmune().clientTrackingRange(8).updateInterval(3),
				PinkMonsterEntity.createAttributes());
		Registration.spawnEgg(BalloonRatIds.Items.BALLOON_RAT_SPAWN_EGG, BALLOON_RAT);

		Spawns.inBiomeTag(BALLOON_RAT, MobCategory.AMBIENT, 10, 1, 1, "more_critters:spawns/balloon_rat");
		Spawns.onSurface(BALLOON_RAT);
	}

	private BalloonRatEntities() {}
}
