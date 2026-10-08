package com.morecritters.fabric.client.module.critter_atlas;

import java.util.List;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * One page (or two-page spread) of the critter atlas. All positions are relative to the page's
 * top-left corner, which the screen centres. The data comes from {@link AtlasPages}.
 *
 * @param pictures drawn in order: the open book, text panels, backgrounds, checkmarks
 * @param models critters drawn standing on the page
 * @param tooltips hover areas that name what is under them
 * @param buttons the arrows and icons that turn pages
 */
record AtlasPage(String name, int width, int height, List<Picture> pictures, List<Model> models,
		List<Tooltip> tooltips, List<Button> buttons) {

	/**
	 * An image drawn whole at its own size. A picture with a {@code collectedCritterling} is only
	 * drawn once the player has caught that critterling (the critterling page's checkmarks).
	 */
	record Picture(String texture, int x, int y, int width, int height, @Nullable Identifier collectedCritterling) {}

	/**
	 * An entity drawn with its feet at (x, y), {@code scale} pixels per block, turning to follow the
	 * mouse as seen from (x, lookY). A model with a {@code tab} only shows while that tab is picked
	 * (the corpse crew page's number buttons).
	 */
	record Model(Identifier entityType, int x, int y, int scale, int lookY, int tab) {
		static final int ALWAYS = -1;
	}

	/** Shows the translated {@code key} while the mouse is strictly inside the rectangle. */
	record Tooltip(String key, int x0, int y0, int x1, int y1) {}

	/** A two-state image button: the texture holds the normal look above the hovered one. */
	record Button(String texture, int x, int y, int width, int height, ButtonAction action) {}

	sealed interface ButtonAction {
		/** Turns to another page, with the book's page-turn sound. */
		record TurnTo(String page) implements ButtonAction {}

		/** Picks which of a page's tabbed models is shown. */
		record ShowTab(int tab) implements ButtonAction {}

		/** Plays a sound: the party hat in the contents page's corner plays the anniveteran's death sound. */
		record PlaySound(Identifier sound) implements ButtonAction {}
	}
}
