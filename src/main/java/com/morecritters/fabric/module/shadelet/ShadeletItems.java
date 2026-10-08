package com.morecritters.fabric.module.shadelet;

import net.minecraft.world.item.component.SwingAnimation;
import com.morecritters.fabric.core.Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** The shadelet module's usable items. */
final class ShadeletItems {
	private ShadeletItems() {}

	/** Tooth syringe: a gamble, 30% Poison II for 3 s, otherwise Regeneration III for 3 s. */
	static class ToothSyringeItem extends Item {
		private static final int EFFECT_TICKS = 60;

		ToothSyringeItem(Properties properties) {
			super(properties);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
			if (!level.isClientSide()) {
				boolean unlucky = player.getRandom().nextInt(10) < 3;
				player.addEffect(unlucky
					? new MobEffectInstance(MobEffects.POISON, EFFECT_TICKS, 1, false, true)
					: new MobEffectInstance(MobEffects.REGENERATION, EFFECT_TICKS, 2, false, true));
				Sounds.playAt(player, ShadeletModule.SYRINGE_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
			}
			if (!player.hasInfiniteMaterials()) {
				player.getItemInHand(hand).shrink(1);
			}
			return InteractionResult.SUCCESS;
		}
	}

	/** Chattering teeth: wind them up and set them down against the clicked face, facing where you look. */
	static class ChatteringTeethItem extends Item {
		ChatteringTeethItem(Properties properties) {
			super(properties);
		}

		@Override
		public InteractionResult useOn(UseOnContext context) {
			ItemStack stack = context.getItemInHand();
			stack.shrink(1);
			if (context.getLevel() instanceof ServerLevel level && context.getPlayer() != null) {
				float yaw = context.getPlayer().getYRot();
				BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
				ChatteringTeethEntity teeth = ShadeletModule.CHATTERING_TEETH.spawn(level, pos, EntitySpawnReason.MOB_SUMMONED);
				if (teeth != null) {
					teeth.setYRot(yaw);
					teeth.setYBodyRot(yaw);
					teeth.setYHeadRot(yaw);
					Sounds.playAt(teeth, ShadeletModule.TEETH_START_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
				}
			}
			return InteractionResult.SUCCESS;
		}
	}
}
