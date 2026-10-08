package com.morecritters.fabric.module.mightshroom;

import com.morecritters.fabric.ids.CorpseGearIds;
import com.morecritters.fabric.ids.NightshroomIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The ancient skeleton branches of the original's right-click-mob procedure that concern this module: a stew
 * poured on a bare skeleton chooses what will rise from it, and purgatorial mixture (corpse_gear) starts the
 * Spawn Mightshroom effect that raises it. The other skeleton branch (sneak-picking it up) is the nightshroom
 * module's.
 */
final class RaisingRitual {
	private static final int RITUAL_TICKS = 110;

	/** Registered as a {@code UseEntityCallback}; acts on the main hand, like the original. */
	static InteractionResult interact(Player player, Level level, InteractionHand hand, Entity target, @Nullable EntityHitResult hit) {
		if (hand != InteractionHand.MAIN_HAND || !SpawnMightshroomEffect.isRaisable(target) || !(target instanceof LivingEntity body)) {
			return InteractionResult.PASS;
		}
		ItemStack held = player.getMainHandItem();
		if (held.is(MightshroomModule.deathStew)) {
			return pourSoup(player, level, body, ShroomRaisable.Soup.DEATH);
		}
		if (held.is(MightshroomModule.lifeStew)) {
			return pourSoup(player, level, body, ShroomRaisable.Soup.LIFE);
		}
		if (isPurgatorialMixture(held) && !body.hasEffect(MightshroomModule.SPAWN_MIGHTSHROOM)) {
			return startRitual(player, level, body);
		}
		return InteractionResult.PASS;
	}

	private static boolean isPurgatorialMixture(ItemStack stack) {
		return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(CorpseGearIds.Items.PURGATORIAL_MIXTURE);
	}

	/** Only a skeleton that has had no stew yet takes one; the bowl comes back. */
	private static InteractionResult pourSoup(Player player, Level level, LivingEntity body, ShroomRaisable.Soup soup) {
		if (!(body instanceof ShroomRaisable raisable) || raisable.soup() != ShroomRaisable.Soup.NONE) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel server) {
			OtherModules.sound(server, body.blockPosition(), NightshroomIds.Sounds.ENTITY_ANCIENT_SKELETON_POUR_SOUP, SoundSource.NEUTRAL);
			player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
			raisable.pourSoup(soup);
			if (!player.hasInfiniteMaterials()) {
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOWL));
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** The skeleton stirs; the bottle comes back and the ritual runs for five and a half seconds. */
	private static InteractionResult startRitual(Player player, Level level, LivingEntity body) {
		if (level instanceof ServerLevel server) {
			OtherModules.sound(server, body.blockPosition(), NightshroomIds.Sounds.ENTITY_ANCIENT_SKELETON_RISE, SoundSource.NEUTRAL);
			player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
			if (!player.hasInfiniteMaterials()) {
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
			}
			body.addEffect(new MobEffectInstance(MightshroomModule.SPAWN_MIGHTSHROOM, RITUAL_TICKS, 0, false, false));
		}
		return InteractionResult.SUCCESS;
	}

	private RaisingRitual() {}
}
