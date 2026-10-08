package com.morecritters.fabric.module.armossillo;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.ids.MiscIds;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.List;
import java.util.Optional;

/**
 * A shield made of sturdy shells that bites back: whenever it blocks a hit, it snaps at the
 * attacker for 2 to 4 damage (multiplied by its Sharp Teeth level), with a bite sound and a
 * bite mark over the attacker. Otherwise it blocks like a vanilla shield, but lasts longer and
 * does not burn.
 */
public class BitingShieldItem extends ShieldItem {
	private static final int DURABILITY = 764;
	private static final int BITE_COOLDOWN = 3;
	private static final double BITE_MIN = 2.0;
	private static final double BITE_MAX = 4.0;
	private static final ResourceKey<Enchantment> SHARP_TEETH = ResourceKey.create(Registries.ENCHANTMENT, MoreCritters.id("sharp_teeth"));

	public BitingShieldItem(Item.Properties properties) {
		super(properties);
	}

	/** Vanilla shield blocking with the original's durability, fire resistance and sturdy-shell repairs. */
	static Item.Properties properties(Item repairItem) {
		return new Item.Properties()
			.durability(DURABILITY)
			.fireResistant()
			.repairable(repairItem)
			.equippableUnswappable(EquipmentSlot.OFFHAND)
			.delayedComponent(DataComponents.BLOCKS_ATTACKS, context -> new BlocksAttacks(
				0.25F,
				1.0F,
				List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)),
				new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
				Optional.of(context.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
				Optional.of(SoundEvents.SHIELD_BLOCK),
				Optional.of(SoundEvents.SHIELD_BREAK)))
			.component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK);
	}

	static void registerBite() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register(BitingShieldItem::afterDamage);
	}

	private static void afterDamage(LivingEntity defender, DamageSource source, float baseDamage, float damageTaken, boolean blocked) {
		if (!blocked || !(defender.level() instanceof ServerLevel level)) return;
		Entity attacker = source.getEntity();
		ItemStack shield = defender.getItemBlockingWith();
		if (attacker == null || shield == null || !shield.is(ArmossilloItems.BITING_SHIELD)) return;
		bite(level, defender, attacker, shield);
	}

	private static void bite(ServerLevel level, LivingEntity defender, Entity attacker, ItemStack shield) {
		if (defender instanceof Player player) {
			player.getCooldowns().addCooldown(shield, BITE_COOLDOWN);
		}
		Sounds.playAt(defender, ArmossilloItems.BITING_SHIELD_BITE_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
		showBiteMark(level, attacker);
		float damage = (float) Mth.nextDouble(defender.getRandom(), BITE_MIN, BITE_MAX);
		int sharpTeeth = sharpTeethLevel(level, shield);
		if (sharpTeeth > 0) damage *= sharpTeeth;
		attacker.hurtServer(level, level.damageSources().generic(), damage);
	}

	/** The misc module's bite particle, a block above the attacker's feet; skipped if that module is not loaded. */
	private static void showBiteMark(ServerLevel level, Entity attacker) {
		ParticleType<?> bite = BuiltInRegistries.PARTICLE_TYPE.getValue(MiscIds.Particles.BITE);
		if (bite instanceof SimpleParticleType particle) {
			level.sendParticles(particle, true, false, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), 1, 0.0, 0.0, 0.0, 1.0);
		}
	}

	private static int sharpTeethLevel(ServerLevel level, ItemStack shield) {
		Optional<Holder.Reference<Enchantment>> sharpTeeth = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(SHARP_TEETH);
		return sharpTeeth.map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, shield)).orElse(0);
	}
}
