package com.morecritters.fabric.module.critterlings_a;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.ids.CritterlingsAIds;
import com.morecritters.fabric.module.critterling_system.CritterlingRarity;
import com.morecritters.fabric.module.critterling_system.CritterlingSackItem;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Rarity;

/**
 * Four critterlings: the hopping cubefrog, the plainswyrm, the dunger and the snek, each with a
 * normal, rare and epic critterling sack. None spawns naturally; they come out of sacks.
 */
public final class CritterlingsAModule implements Module {
	public static EntityType<CubefrogEntity> CUBEFROG;
	public static EntityType<PlainswyrmEntity> PLAINSWYRM;
	public static EntityType<DungerEntity> DUNGER;
	public static EntityType<SnekEntity> SNEK;

	static SoundEvent CUBEFROG_HURT, CUBEFROG_IDLE, CUBEFROG_JUMP, DUNGER_HURT, PLAINSWYRM_HURT, SNEK_HURT, SNEK_IDLE;

	@Override
	public void register() {
		CUBEFROG_HURT = Registration.sound(CritterlingsAIds.Sounds.ENTITY_CUBEFROG_HURT);
		CUBEFROG_IDLE = Registration.sound(CritterlingsAIds.Sounds.ENTITY_CUBEFROG_IDLE);
		CUBEFROG_JUMP = Registration.sound(CritterlingsAIds.Sounds.ENTITY_CUBEFROG_JUMP);
		DUNGER_HURT = Registration.sound(CritterlingsAIds.Sounds.ENTITY_DUNGER_HURT);
		PLAINSWYRM_HURT = Registration.sound(CritterlingsAIds.Sounds.ENTITY_PLAINSWYRM_HURT);
		SNEK_HURT = Registration.sound(CritterlingsAIds.Sounds.ENTITY_SNEK_HURT);
		SNEK_IDLE = Registration.sound(CritterlingsAIds.Sounds.ENTITY_SNEK_IDLE);

		CUBEFROG = critterling(CritterlingsAIds.Entities.CUBEFROG, EntityType.Builder.of(CubefrogEntity::new, MobCategory.MONSTER));
		PLAINSWYRM = critterling(CritterlingsAIds.Entities.PLAINSWYRM, EntityType.Builder.of(PlainswyrmEntity::new, MobCategory.MONSTER));
		DUNGER = critterling(CritterlingsAIds.Entities.DUNGER, EntityType.Builder.of(DungerEntity::new, MobCategory.MONSTER));
		SNEK = critterling(CritterlingsAIds.Entities.SNEK, EntityType.Builder.of(SnekEntity::new, MobCategory.MONSTER));

		// All twelve sacks use the plain uncommon / rare / epic item rarities.
		sacks(CUBEFROG, CritterlingsAIds.Items.CRITTERLING_SACK_CUBEFROG,
			CritterlingsAIds.Items.CRITTERLING_SACK_CUBEFROG_RARE, CritterlingsAIds.Items.CRITTERLING_SACK_CUBEFROG_EPIC);
		sacks(PLAINSWYRM, CritterlingsAIds.Items.CRITTERLING_SACK_PLAINSWYRM,
			CritterlingsAIds.Items.CRITTERLING_SACK_PLAINSWYRM_RARE, CritterlingsAIds.Items.CRITTERLING_SACK_PLAINSWYRM_EPIC);
		sacks(DUNGER, CritterlingsAIds.Items.CRITTERLING_SACK_DUNGER,
			CritterlingsAIds.Items.CRITTERLING_SACK_DUNGER_RARE, CritterlingsAIds.Items.CRITTERLING_SACK_DUNGER_EPIC);
		sacks(SNEK, CritterlingsAIds.Items.CRITTERLING_SACK_SNEK,
			CritterlingsAIds.Items.CRITTERLING_SACK_SNEK_RARE, CritterlingsAIds.Items.CRITTERLING_SACK_SNEK_EPIC);
	}

	/** All four are 0.4 x 0.4 monsters with the same stats. */
	private static <T extends Mob> EntityType<T> critterling(Identifier id, EntityType.Builder<T> builder) {
		return Registration.livingEntity(id, builder.sized(0.4F, 0.4F).clientTrackingRange(8).updateInterval(3), attributes());
	}

	private static AttributeSupplier.Builder attributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 3.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	private static void sacks(EntityType<? extends Mob> type, Identifier normal, Identifier rare, Identifier epic) {
		CritterlingSackItem.register(normal, type, CritterlingRarity.NORMAL, Rarity.UNCOMMON);
		CritterlingSackItem.register(rare, type, CritterlingRarity.RARE, Rarity.RARE);
		CritterlingSackItem.register(epic, type, CritterlingRarity.EPIC, Rarity.EPIC);
	}
}
