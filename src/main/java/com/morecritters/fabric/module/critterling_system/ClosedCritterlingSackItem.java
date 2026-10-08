package com.morecritters.fabric.module.critterling_system;

import com.morecritters.fabric.core.Sounds;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;

/**
 * A mystery sack, dropped by every mob in gamble mode ({@link GambleMode}). Opening it is a coin
 * flip: heads, a jingle and a random critterling sack from {@code #minecraft:random_common}, with a
 * 1 in 20 chance of upgrading to {@code #random_rare} and from there 1 in 7 to {@code #random_epic};
 * tails, just an empty critterling sack.
 */
public class ClosedCritterlingSackItem extends Item {
	private static final TagKey<Item> COMMON = tag("random_common"), RARE = tag("random_rare"), EPIC = tag("random_epic");
	private static final int RARE_CHANCE = 20, EPIC_CHANCE = 7;

	public ClosedCritterlingSackItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!player.getMainHandItem().is(this)) return InteractionResult.PASS;
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		if (level instanceof ServerLevel serverLevel) {
			Sounds.playAt(player, CritterlingSystemModule.SACK_PUT_DOWN_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
			player.setItemInHand(InteractionHand.MAIN_HAND, open(serverLevel, player));
		}
		return InteractionResult.SUCCESS;
	}

	private static ItemStack open(ServerLevel level, Player player) {
		if (!level.getRandom().nextBoolean()) {
			return new ItemStack(CritterlingSystemModule.CRITTERLING_SACK);
		}
		Sounds.playAt(player, CritterlingSystemModule.JACKPOT_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
		TagKey<Item> prize = COMMON;
		if (level.getRandom().nextInt(RARE_CHANCE) == 0) {
			prize = level.getRandom().nextInt(EPIC_CHANCE) == 0 ? EPIC : RARE;
		}
		return BuiltInRegistries.ITEM.getRandomElementOf(prize, level.getRandom())
			.map(Holder::value)
			.map(ItemStack::new)
			.orElse(ItemStack.EMPTY);
	}

	private static TagKey<Item> tag(String name) {
		return TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace(name));
	}
}
