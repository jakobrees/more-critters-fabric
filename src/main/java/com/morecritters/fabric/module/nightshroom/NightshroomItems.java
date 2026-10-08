package com.morecritters.fabric.module.nightshroom;

import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.NightshroomIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;

/** The shrooms' drops, the packed-up ancient skeleton and the four creative spawn dolls. */
final class NightshroomItems {
	static Item ancientSkeleton;

	static void register() {
		Registration.item(NightshroomIds.Items.ANCIENT_BONE);
		Registration.item(NightshroomIds.Items.REGENERATIVE_FLESH);
		Registration.item(NightshroomIds.Items.FUNGAL_FLESH, FungalFleshItem::new, new Item.Properties());
		ancientSkeleton = Registration.item(NightshroomIds.Items.ANCIENT_SKELETON_ITEM, AncientSkeletonItem::new, new Item.Properties().stacksTo(1));

		spawnDoll(NightshroomIds.Items.NIGHTSHROOM_SPAWN_DOLL, "nightshroom_spawn_doll", NightshroomModule.NIGHTSHROOM);
		spawnDoll(NightshroomIds.Items.FRIGHTSHROOM_SPAWN_DOLL, "frightshroom_spawn_doll", NightshroomModule.FRIGHTSHROOM);
		spawnDoll(NightshroomIds.Items.FUNGAL_ZOMBIE_SPAWN_DOLL, "fungal_zombie_spawn_doll", NightshroomModule.FUNGAL_ZOMBIE);
		spawnDoll(NightshroomIds.Items.ROT_ZOMBIE_SPAWN_DOLL, "rot_zombie_spawn_doll", NightshroomModule.ROT_ZOMBIE);
	}

	private static void spawnDoll(Identifier id, String name, EntityType<? extends Mob> creature) {
		Registration.item(id, properties -> new SpawnDollItem(creature, properties), Tooltips.describe(new Item.Properties(), name, 3));
	}

	private NightshroomItems() {}
}
