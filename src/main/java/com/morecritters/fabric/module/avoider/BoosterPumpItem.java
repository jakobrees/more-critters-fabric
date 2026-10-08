package com.morecritters.fabric.module.avoider;

import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.ids.MiscIds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A pump made of avoider tails. Underwater it shoves the player backwards in a burst of bubbles;
 * on land (or while gliding) it launches them upwards.
 */
public class BoosterPumpItem extends Item {
	private static final int UNDERWATER_COOLDOWN = 100;
	private static final int GLIDING_COOLDOWN = 200;
	private static final int LAND_COOLDOWN = 50;
	private static final double UNDERWATER_PUSH = -2.0;
	private static final double LAUNCH_SPEED = 1.0;
	private static final double LAND_HORIZONTAL_BOOST = 3.0;
	private static final int DOLPHINS_GRACE_TICKS = 40;
	private static final int DOLPHINS_GRACE_LEVEL = 2;
	private static final int BUBBLE_TRAIL_TICKS = 5;

	public BoosterPumpItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack pump = player.getItemInHand(hand);
		if (player.isInWater()) {
			boostUnderwater(player, pump, hand);
		} else if (player.isFallFlying()) {
			launch(player, pump, hand, GLIDING_COOLDOWN, 1.0);
		} else {
			launch(player, pump, hand, LAND_COOLDOWN, LAND_HORIZONTAL_BOOST);
		}
		return InteractionResult.SUCCESS;
	}

	/** Pushes the player away from where they look, with dolphin's grace and a short trail of bubbles. */
	private void boostUnderwater(Player player, ItemStack pump, InteractionHand hand) {
		player.getCooldowns().addCooldown(pump, UNDERWATER_COOLDOWN);
		player.setDeltaMovement(player.getLookAngle().scale(UNDERWATER_PUSH));
		if (player.level().isClientSide()) return;
		pump.hurtAndBreak(1, player, slotOf(hand));
		player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, DOLPHINS_GRACE_TICKS, DOLPHINS_GRACE_LEVEL, false, false));
		playBoost(player, AvoiderModule.PUMP_UNDERWATER_SOUND);
		int bubblesPerTick = (int) Mth.nextDouble(player.getRandom(), 6.0, 10.0);
		for (int tick = 0; tick < BUBBLE_TRAIL_TICKS; tick++) {
			ServerScheduler.runLater(tick, () -> Particles.spawnAt(player, ParticleTypes.BUBBLE, bubblesPerTick, 0.3, 0.3, 0.3, 0.02));
		}
	}

	/** Throws the player upwards, multiplying their horizontal speed. */
	private void launch(Player player, ItemStack pump, InteractionHand hand, int cooldown, double horizontalBoost) {
		player.getCooldowns().addCooldown(pump, cooldown);
		Vec3 motion = player.getDeltaMovement();
		player.setDeltaMovement(motion.x * horizontalBoost, LAUNCH_SPEED, motion.z * horizontalBoost);
		if (player.level().isClientSide()) return;
		pump.hurtAndBreak(1, player, slotOf(hand));
		playBoost(player, AvoiderModule.PUMP_LAND_SOUND);
	}

	private static EquipmentSlot slotOf(InteractionHand hand) {
		return hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
	}

	private static void playBoost(Player player, SoundEvent sound) {
		Sounds.playAt(player, sound, SoundSource.PLAYERS, 1.0F, 1.0F);
		if (BuiltInRegistries.PARTICLE_TYPE.getValue(MiscIds.Particles.DASH) instanceof ParticleOptions dash) {
			Particles.spawnAt(player, dash, 1, 0, 0, 0, 0);
		}
	}
}
