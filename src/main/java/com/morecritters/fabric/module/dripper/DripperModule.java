package com.morecritters.fabric.module.dripper;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Spawns;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.DripperIds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;

/** The dripper: a leaping dripstone-caves monster, the club made from its remains, its slash, and a wall mask. */
public final class DripperModule implements Module {
	public static EntityType<DripperEntity> DRIPPER;
	public static EntityType<SlashEffectEntity> SLASH_EFFECT;
	public static Item DRIPPER_REMAINS;
	public static Item SLASHKLUB;
	public static DripstoneWallMaskBlock DRIPSTONE_WALL_MASK;

	static SoundEvent HURT_SOUND, DEATH_SOUND, STEP_SOUND, JUMP_SOUND, SLASHKLUB_SOUND;

	@Override
	public void register() {
		HURT_SOUND = Registration.sound(DripperIds.Sounds.ENTITY_DRIPPER_HURT);
		DEATH_SOUND = Registration.sound(DripperIds.Sounds.ENTITY_DRIPPER_DEATH);
		STEP_SOUND = Registration.sound(DripperIds.Sounds.ENTITY_DRIPPER_STEP);
		JUMP_SOUND = Registration.sound(DripperIds.Sounds.ENTITY_DRIPPER_JUMP);
		SLASHKLUB_SOUND = Registration.sound(DripperIds.Sounds.ITEM_SLASHKLUB_ATTACK);
		// The dripper's own code never played these in the original; registered for other users (the kelpire's bite uses attack).
		Registration.sound(DripperIds.Sounds.ENTITY_DRIPPER_ATTACK);
		Registration.sound(DripperIds.Sounds.ENTITY_DRIPPER_LAND);

		DRIPPER = Registration.livingEntity(DripperIds.Entities.DRIPPER,
			EntityType.Builder.of(DripperEntity::new, MobCategory.MONSTER).sized(0.8F, 1.3F).clientTrackingRange(8).updateInterval(3),
			DripperEntity.createAttributes());
		SLASH_EFFECT = Registration.livingEntity(DripperIds.Entities.SLASH_EFFECT,
			EntityType.Builder.of(SlashEffectEntity::new, MobCategory.MONSTER).sized(0.4F, 0.4F).fireImmune().clientTrackingRange(8).updateInterval(3),
			SlashEffectEntity.createAttributes());

		Registration.spawnEgg(DripperIds.Items.DRIPPER_SPAWN_EGG, DRIPPER);
		DRIPPER_REMAINS = Registration.item(DripperIds.Items.DRIPPER_REMAINS, new Item.Properties());
		SLASHKLUB = Registration.item(DripperIds.Items.SLASHKLUB, SlashklubItem::new,
			Tooltips.describe(new Item.Properties()
				.sword(SlashklubItem.MATERIAL, SlashklubItem.ATTACK_DAMAGE, SlashklubItem.ATTACK_SPEED)
				.repairable(DRIPPER_REMAINS), "slashklub", 1));
		DRIPSTONE_WALL_MASK = Registration.blockWithItem(DripperIds.Blocks.DRIPSTONE_WALL_MASK, DripstoneWallMaskBlock::new,
			BlockBehaviour.Properties.of().sound(SoundType.DRIPSTONE_BLOCK).instabreak().noOcclusion().isRedstoneConductor((state, level, pos) -> false));

		SpawnPlacements.register(DRIPPER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
		Spawns.inBiomes(DRIPPER, MobCategory.MONSTER, 26, 2, 6, "minecraft:dripstone_caves");
	}
}
