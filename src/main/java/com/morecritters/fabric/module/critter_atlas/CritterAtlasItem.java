package com.morecritters.fabric.module.critter_atlas;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import java.util.function.Consumer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The critter atlas: a book about every critter. Using it opens the atlas at its contents page with
 * a page-turn sound and awards "open_critter_atlas"; using it while sneaking shows the closed book.
 * The pages are drawn and turned entirely on the client.
 */
public class CritterAtlasItem extends Item {
	/**
	 * Opens the atlas screen; installed by the client module (the item cannot name client classes).
	 * The argument is whether the player is sneaking, i.e. wants to see the cover.
	 */
	public static Consumer<Boolean> screenOpener = sneaking -> {};

	public CritterAtlasItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		boolean sneaking = player.isShiftKeyDown();
		if (level.isClientSide()) {
			screenOpener.accept(sneaking);
		} else if (!sneaking) {
			level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
			Advancements.award(player, MoreCritters.id("open_critter_atlas"));
		}
		return InteractionResult.SUCCESS;
	}
}
