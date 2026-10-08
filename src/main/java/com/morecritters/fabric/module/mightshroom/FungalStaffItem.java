package com.morecritters.fabric.module.mightshroom;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.ids.MiscIds;
import com.morecritters.fabric.ids.NightshroomIds;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A staff with the powers of both shrooms. Hitting a creature gives it Imminent Death (and now and then traps
 * it in mori roots); using it gives yourself the Gift of Life, and crouch-using it gives it to every player and
 * every pet of yours within twelve blocks. The Lovely Side enchantment favours healing, Deadly Side harm.
 */
public class FungalStaffItem extends Item {
	/** Durability 500, mining speed 1, -1 damage, enchantability 3, mines like diamond. The repair item (ancient bone) is set in the module. */
	static final ToolMaterial MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 500, 1.0F, -1.0F, 3, ItemTags.WOODEN_TOOL_MATERIALS);
	static final float ATTACK_DAMAGE = 3.0F, ATTACK_SPEED = -3.0F;

	private static final ResourceKey<Enchantment> LOVELY_SIDE = ResourceKey.create(Registries.ENCHANTMENT, MoreCritters.id("lovely_side"));
	private static final ResourceKey<Enchantment> DEADLY_SIDE = ResourceKey.create(Registries.ENCHANTMENT, MoreCritters.id("deadly_side"));

	private static final int HEAL_COOLDOWN = 200, BIG_HEAL_COOLDOWN = 500;
	private static final double BIG_HEAL_RANGE = 12.5;
	private static final int HIT_COOLDOWN = 20, ROOTS_COOLDOWN = 60;

	/** Which side of the staff its enchantment favours. */
	private enum Side {
		LOVELY(40, 10, 20), DEADLY(10, 40, 5), PLAIN(30, 20, 10);

		final int giftTicks;
		final int deathTicks;
		/** One hit in this many traps the victim in mori roots. */
		final int rootsChance;

		Side(int giftTicks, int deathTicks, int rootsChance) {
			this.giftTicks = giftTicks;
			this.deathTicks = deathTicks;
			this.rootsChance = rootsChance;
		}
	}

	public FungalStaffItem(Item.Properties properties) {
		super(properties);
	}

	private static Side side(Level level, ItemStack staff) {
		var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		if (enchantments.get(LOVELY_SIDE).map(lovely -> EnchantmentHelper.getItemEnchantmentLevel(lovely, staff)).orElse(0) != 0) {
			return Side.LOVELY;
		}
		if (enchantments.get(DEADLY_SIDE).map(deadly -> EnchantmentHelper.getItemEnchantmentLevel(deadly, staff)).orElse(0) != 0) {
			return Side.DEADLY;
		}
		return Side.PLAIN;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		ItemStack staff = player.getItemInHand(hand);
		Side side = side(level, staff);
		if (player.isShiftKeyDown()) {
			healAround(server, player, staff, side);
		} else {
			healSelf(server, player, staff, side);
		}
		return InteractionResult.SUCCESS;
	}

	/** A small heal echo, yellow stripes, and the Gift of Life for yourself. */
	private static void healSelf(ServerLevel level, Player player, ItemStack staff, Side side) {
		level.playSound(null, player.blockPosition(), MightshroomModule.STAFF_HEAL_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
		player.getCooldowns().addCooldown(staff, HEAL_COOLDOWN);
		summonEcho(level, MightshroomModule.SMALL_HEAL_ECHO, player.position());
		stripes(level, player, 0.5);
		giveGift(player, side);
	}

	/**
	 * Every player and every pet of the user within range gets the Gift of Life. As in the original, each one
	 * healed also brings its own heal echo and stripes around the user.
	 */
	private static void healAround(ServerLevel level, Player player, ItemStack staff, Side side) {
		level.playSound(null, player.blockPosition(), MightshroomModule.STAFF_BIG_HEAL_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
		Vec3 centre = player.position();
		List<LivingEntity> healed = level.getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(BIG_HEAL_RANGE),
				entity -> entity instanceof Player || entity instanceof TamableAnimal pet && pet.isOwnedBy(player))
			.stream().sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(centre))).toList();
		for (LivingEntity friend : healed) {
			player.getCooldowns().addCooldown(staff, BIG_HEAL_COOLDOWN);
			summonEcho(level, MightshroomModule.HEAL_ECHO, centre);
			stripes(level, player, 2.0);
			giveGift(friend, side);
		}
	}

	private static void giveGift(LivingEntity entity, Side side) {
		entity.addEffect(new MobEffectInstance(MightshroomModule.GIFT_OF_LIFE, side.giftTicks, 0, false, false));
	}

	/** Three or four bursts of eight yellow stripes around the user's feet. */
	private static void stripes(ServerLevel level, Player player, double spread) {
		int bursts = (int) Mth.nextDouble(player.getRandom(), 3.0, 5.0);
		for (int i = 0; i < bursts; i++) {
			OtherModules.particles(level, MiscIds.Particles.YELLOW_STRIPE, player.getX(), player.getY(), player.getZ(), 8, spread, 0.2, spread, 0.0);
		}
	}

	/** Like the original's {@code /summon}: at the exact position, facing south. */
	private static void summonEcho(ServerLevel level, EntityType<EchoEntity> type, Vec3 at) {
		EchoEntity echo = type.create(level, EntitySpawnReason.COMMAND);
		if (echo != null) {
			echo.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
			level.addFreshEntity(echo);
		}
	}

	/**
	 * Each hit off cooldown gives Imminent Death. Now and then (more often on the deadly side) the victim is
	 * instead trapped in mori roots with a longer Imminent Death, and the staff cools down for three seconds.
	 */
	@Override
	public void hurtEnemy(ItemStack staff, LivingEntity target, LivingEntity attacker) {
		super.hurtEnemy(staff, target, attacker);
		if (!(target.level() instanceof ServerLevel level) || OtherModules.isOfType(target, NightshroomIds.Entities.MORI_ROOTS)) {
			return;
		}
		if (attacker instanceof Player player && player.getCooldowns().isOnCooldown(staff)) {
			return;
		}
		Side side = side(level, staff);
		boolean roots = attacker.getRandom().nextInt(side.rootsChance) == 0;
		Holder<MobEffect> imminentDeath = MightshroomModule.IMMINENT_DEATH;
		if (!roots) {
			target.addEffect(new MobEffectInstance(imminentDeath, side.deathTicks, 0, false, false));
			if (attacker instanceof Player player) {
				player.getCooldowns().addCooldown(staff, HIT_COOLDOWN);
			}
			return;
		}
		target.addEffect(new MobEffectInstance(imminentDeath, 40, 0, false, false));
		if (attacker instanceof Player player) {
			player.getCooldowns().addCooldown(staff, ROOTS_COOLDOWN);
		}
		OtherModules.entityType(NightshroomIds.Entities.MORI_ROOTS).ifPresent(type -> {
			Entity trap = type.create(level, EntitySpawnReason.COMMAND);
			if (trap != null) {
				trap.snapTo(target.getX(), target.getY(), target.getZ(), 0.0F, 0.0F);
				if (trap instanceof Mob mob) {
					mob.finalizeSpawn(level, level.getCurrentDifficultyAt(trap.blockPosition()), EntitySpawnReason.COMMAND, null);
				}
				level.addFreshEntity(trap);
			}
		});
		OtherModules.sound(level, target.blockPosition(), NightshroomIds.Sounds.ENTITY_MORI_ROOTS_BITE, SoundSource.HOSTILE);
	}
}
