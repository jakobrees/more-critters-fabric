package com.morecritters.fabric.module.bunbug;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

/** Cracked against a block face, a clutch of eggs hatches one baby bunbug on that side. */
final class BunbugEggsItem extends Item {
	private static final int SHELL_PARTICLES = 25;

	BunbugEggsItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null) return InteractionResult.PASS;
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		context.getItemInHand().shrink(1);

		if (context.getLevel() instanceof ServerLevel level) {
			BlockPos hatchPos = context.getClickedPos().relative(context.getClickedFace());
			level.playSound(null, context.getClickedPos(), SoundEvents.TURTLE_EGG_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
			BabyBunbugEntity baby = BunbugModule.BABY_BUNBUG.spawn(level, hatchPos, EntitySpawnReason.SPAWN_ITEM_USE);
			if (baby != null) {
				baby.setYRot(level.getRandom().nextFloat() * 360.0F);
				baby.setDeltaMovement(Vec3.ZERO);
			}
			Vec3 centre = Vec3.atCenterOf(hatchPos);
			level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, this), centre.x, centre.y, centre.z, SHELL_PARTICLES, 0.1, 0.1, 0.1, 0.05);
		}
		return InteractionResult.SUCCESS;
	}
}
