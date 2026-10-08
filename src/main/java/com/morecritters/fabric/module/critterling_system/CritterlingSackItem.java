package com.morecritters.fabric.module.critterling_system;

import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Tooltips;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import org.jspecify.annotations.Nullable;

/**
 * A critterling sack with a creature inside. Used on a block it lets the creature out next to the
 * clicked face, in the form it was caught in, and leaves an empty {@code critterling_sack} in hand.
 * While it sits in a player's inventory the creature counts as collected
 * ({@link CritterlingCollection}).
 *
 * <p>Every sack registers itself under its creature type and rarity, which is how
 * {@link CritterlingCatching} knows which sack a creature goes into.
 */
public class CritterlingSackItem extends Item {
	private static final Map<EntityType<?>, Map<CritterlingRarity, CritterlingSackItem>> SACKS = new HashMap<>();

	private final EntityType<? extends Mob> creature;
	private final CritterlingRarity rarity;

	/**
	 * @param creature the creature inside
	 * @param rarity   the form it is let out in; creatures that are not {@link Critterling}s use NORMAL
	 */
	public CritterlingSackItem(EntityType<? extends Mob> creature, CritterlingRarity rarity, Properties properties) {
		super(properties);
		this.creature = creature;
		this.rarity = rarity;
		SACKS.computeIfAbsent(creature, type -> new HashMap<>()).put(rarity, this);
	}

	/**
	 * Registers the sack for one form of a critterling the way the original defined them all:
	 * unstackable, with the item rarity given, and the language file's description lines
	 * (one for a normal sack: the creature's name; two for rare and epic: name and rarity).
	 */
	public static CritterlingSackItem register(Identifier id, EntityType<? extends Mob> creature, CritterlingRarity rarity, Rarity itemRarity) {
		int descriptionLines = rarity == CritterlingRarity.NORMAL ? 1 : 2;
		Properties properties = Tooltips.describe(new Properties().stacksTo(1).rarity(itemRarity), id.getPath(), descriptionLines);
		return (CritterlingSackItem) Registration.item(id, p -> new CritterlingSackItem(creature, rarity, p), properties);
	}

	/** The sack a creature goes into when caught, or null if it cannot be caught. */
	public static @Nullable CritterlingSackItem sackFor(Entity entity) {
		Map<CritterlingRarity, CritterlingSackItem> forms = SACKS.get(entity.getType());
		if (forms == null) return null;
		CritterlingRarity rarity = entity instanceof Critterling critterling ? critterling.rarity() : CritterlingRarity.NORMAL;
		return forms.get(rarity);
	}

	public EntityType<? extends Mob> creature() {
		return this.creature;
	}

	public CritterlingRarity rarity() {
		return this.rarity;
	}

	/** Lets the creature out beside the clicked face. As in the original, only from the main hand. */
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null || context.getHand() != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
		if (context.getLevel() instanceof ServerLevel level) {
			BlockPos at = context.getClickedPos().relative(context.getClickedFace());
			Mob released = this.creature.spawn(level, at, EntitySpawnReason.MOB_SUMMONED);
			if (released instanceof Critterling critterling) {
				critterling.setRarity(this.rarity);
			}
			level.playSound(null, context.getClickedPos(), CritterlingSystemModule.SACK_PUT_DOWN_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CritterlingSystemModule.CRITTERLING_SACK));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		if (owner instanceof Player player) {
			CritterlingCollection.collect(player, BuiltInRegistries.ENTITY_TYPE.getKey(this.creature));
		}
	}
}
