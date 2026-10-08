package com.morecritters.fabric.module.armossillo;

import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.ArmossilloIds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;

/**
 * The armossillo's items: sturdy shells and the chestplate and biting shield made from them,
 * glowing ooze and the ooze rod, plus the rod's thrown form.
 */
public final class ArmossilloItems {
	public static Item STURDY_SHELLS, STURDY_CHESTPLATE, BITING_SHIELD, GLOWING_OOZE, OOZE_ROD;
	public static EntityType<FlyingOozeRodEntity> FLYING_OOZE_ROD;

	static SoundEvent STURDY_CHESTPLATE_NULLIFY_SOUND, BITING_SHIELD_BITE_SOUND;

	private static final int OOZE_ROD_STACK = 16;

	static void register() {
		STURDY_CHESTPLATE_NULLIFY_SOUND = Registration.sound(ArmossilloIds.Sounds.ITEM_ARMOR_STURDY_CHESTPLATE_NULLIFY);
		BITING_SHIELD_BITE_SOUND = Registration.sound(ArmossilloIds.Sounds.ITEM_BITING_SHIELD_BITE);

		FLYING_OOZE_ROD = Registration.livingEntity(ArmossilloIds.Entities.FLYING_OOZE_ROD,
			EntityType.Builder.of(FlyingOozeRodEntity::new, MobCategory.MONSTER).sized(0.5F, 0.5F).fireImmune().clientTrackingRange(8).updateInterval(3),
			FlyingOozeRodEntity.createAttributes());

		STURDY_SHELLS = Registration.item(ArmossilloIds.Items.STURDY_SHELLS, new Item.Properties());
		STURDY_CHESTPLATE = Registration.item(ArmossilloIds.Items.STURDY_CHESTPLATE, Tooltips.describe(new Item.Properties()
			.humanoidArmor(SturdyArmor.MATERIAL, ArmorType.CHESTPLATE)
			.repairable(STURDY_SHELLS)
			.fireResistant(), "sturdy_chestplate", 1));
		BITING_SHIELD = Registration.item(ArmossilloIds.Items.BITING_SHIELD, BitingShieldItem::new, BitingShieldItem.properties(STURDY_SHELLS));
		GLOWING_OOZE = Registration.item(ArmossilloIds.Items.GLOWING_OOZE, new Item.Properties());
		OOZE_ROD = Registration.item(ArmossilloIds.Items.OOZE_ROD, OozeRodItem::new, new Item.Properties().stacksTo(OOZE_ROD_STACK));

		SturdyArmor.register();
		BitingShieldItem.registerBite();
	}

	private ArmossilloItems() {}
}
