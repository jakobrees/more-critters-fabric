package com.morecritters.fabric.module.critterling_system;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * Which critterlings a player has ever held in a sack: the original's per-player {@code HadCubefrog},
 * {@code HadDunger}, ... flags, keyed here by the critterling's entity type id. The critter atlas
 * shows a critterling's page only once it is collected, so the list is synced to its owner's client.
 * Kept through death, as the original copied its player variables on respawn.
 */
public final class CritterlingCollection {
	private static final Identifier OBTAIN_ADVANCEMENT = MoreCritters.id("obtain_critterling");

	private static AttachmentType<List<Identifier>> COLLECTED;

	static void register() {
		COLLECTED = AttachmentRegistry.create(MoreCritters.id("critterlings_collected"), builder -> builder
			.persistent(Identifier.CODEC.listOf())
			.copyOnDeath()
			.syncWith(Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), AttachmentSyncPredicate.targetOnly()));
	}

	/** Whether the player has had this critterling (by entity type id) in a sack. Works on both sides. */
	public static boolean hasCollected(Player player, Identifier critterling) {
		return player.getAttachedOrElse(COLLECTED, List.of()).contains(critterling);
	}

	/** Records a critterling as collected and awards "obtain_critterling". Server side. */
	public static void collect(Player player, Identifier critterling) {
		List<Identifier> collected = player.getAttachedOrElse(COLLECTED, List.of());
		if (!collected.contains(critterling)) {
			List<Identifier> updated = new ArrayList<>(collected);
			updated.add(critterling);
			player.setAttached(COLLECTED, List.copyOf(updated));
		}
		Advancements.award(player, OBTAIN_ADVANCEMENT);
	}

	private CritterlingCollection() {}
}
