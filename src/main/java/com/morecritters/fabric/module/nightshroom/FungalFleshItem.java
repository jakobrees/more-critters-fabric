package com.morecritters.fabric.module.nightshroom;

import com.morecritters.fabric.core.Sounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;

/**
 * Flesh overgrown with fungus. Fed to any zombie it turns it into a fungal zombie; fed to a cow,
 * into a mooshroom.
 */
public class FungalFleshItem extends Item {
	public FungalFleshItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND || !(target instanceof Zombie || target instanceof Cow)) return InteractionResult.PASS;
		if (!(player.level() instanceof ServerLevel level)) return InteractionResult.SUCCESS;

		player.swing(hand, SwingAnimation.DEFAULT, true);
		stack.consume(1, player);
		if (target instanceof Zombie) {
			level.sendParticles(Bursts.item(this), target.getX(), target.getY() + 1.5, target.getZ(), 7, 0.2, 0.2, 0.2, 0.05);
			Sounds.playAt(target, NightshroomModule.FUNGAL_ZOMBIE_TRANSFORM_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
			Transformations.replace(level, target, NightshroomModule.FUNGAL_ZOMBIE);
		} else {
			level.sendParticles(Bursts.item(this), target.getX(), target.getY() + 1.0, target.getZ(), 7, 0.2, 0.2, 0.2, 0.05);
			Transformations.replace(level, target, EntityTypes.MOOSHROOM);
		}
		return InteractionResult.SUCCESS;
	}
}
