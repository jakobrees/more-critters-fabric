package com.morecritters.fabric.module.dripper;

import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.ids.MightshroomIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;

/**
 * A dripstone club. Hitting a creature while crouching releases a {@link SlashEffectEntity}
 * from the wielder's feet, makes the victim tremble for a second, and costs three extra
 * durability; the move then cools down for five seconds.
 */
public class SlashklubItem extends Item {
	/** Durability 200, mining speed 4, +2 damage, enchantability 2. Repaired with dripper remains (set on the properties). */
	static final ToolMaterial MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 200, 4.0F, 2.0F, 2, ItemTags.WOODEN_TOOL_MATERIALS);
	static final float ATTACK_DAMAGE = 3.0F, ATTACK_SPEED = -3.0F;

	private static final int SLASH_COOLDOWN = 100;
	private static final int TREMBLE_TICKS = 20;
	private static final int SLASH_DURABILITY_COST = 3;

	public SlashklubItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.hurtEnemy(stack, target, attacker);
		if (!(target.level() instanceof ServerLevel level) || !attacker.isShiftKeyDown()) return;
		if (attacker instanceof Player player) {
			if (player.getCooldowns().isOnCooldown(stack)) return;
			player.getCooldowns().addCooldown(stack, SLASH_COOLDOWN);
		}
		releaseSlash(level, target, attacker);
		stack.hurtAndBreak(SLASH_DURABILITY_COST, attacker, EquipmentSlot.MAINHAND);
	}

	private static void releaseSlash(ServerLevel level, LivingEntity target, LivingEntity attacker) {
		// Tremble belongs to the mightshroom module; without it the slash still fires.
		BuiltInRegistries.MOB_EFFECT.get(MightshroomIds.Effects.TREMBLE).ifPresent(tremble ->
			target.addEffect(new MobEffectInstance(tremble, TREMBLE_TICKS, 0, false, false)));

		SlashEffectEntity slash = DripperModule.SLASH_EFFECT.spawn(level, attacker.blockPosition(), EntitySpawnReason.MOB_SUMMONED);
		if (slash != null) {
			slash.setYRot(attacker.getYRot());
			slash.setYBodyRot(attacker.getYRot());
			slash.setYHeadRot(attacker.getYRot());
			slash.setDeltaMovement(0.0, 0.0, 0.0);
		}
		Sounds.playAt(target, DripperModule.SLASHKLUB_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
	}
}
