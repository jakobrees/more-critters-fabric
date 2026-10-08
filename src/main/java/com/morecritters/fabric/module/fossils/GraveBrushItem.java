package com.morecritters.fabric.module.fossils;

import com.morecritters.fabric.ids.GravediggerIds;
import com.morecritters.fabric.ids.MiscIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Brushing the top of a dirt block digs something up: in the Overworld usually a zombie,
 * skeleton or spider (rarely a zombie holding the Waddle disc), one time in five something
 * rarer, sometimes an amalgam; in the Nether a zombified piglin, zoglin or wither skeleton.
 * Five-second cooldown, one durability per use.
 */
public class GraveBrushItem extends Item {
	private static final int COOLDOWN = 100;
	private static final double RISE = 0.7;
	private static final double SPIDER_RISE = 0.5;
	/**
	 * What the original's {@code #minecraft:dirt} held in 1.20.1 (dirt, coarse and rooted dirt, grass,
	 * podzol, mycelium, moss, mud, muddy mangrove roots). Since 26.x {@code #dirt} holds only the three
	 * dirts; the rest moved to the tags that {@code #substrate_overworld} gathers.
	 */
	private static final TagKey<Block> BRUSHABLE = BlockTags.SUBSTRATE_OVERWORLD;

	public GraveBrushItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		ItemStack brush = context.getItemInHand();
		BlockPos pos = context.getClickedPos();
		BlockState state = context.getLevel().getBlockState(pos);
		if (player == null || context.getClickedFace() != Direction.UP || !state.is(BRUSHABLE)
			|| player.getCooldowns().isOnCooldown(brush)) {
			return InteractionResult.SUCCESS;
		}
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;

		if (!player.hasInfiniteMaterials()) brush.hurtAndBreak(1, player, context.getHand());
		player.getCooldowns().addCooldown(brush, COOLDOWN);
		level.playSound(null, pos, FossilsModule.GRAVE_BRUSH_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), true, true,
			pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 5, 0.2, 0, 0.2, 1);

		if (level.dimension() == Level.OVERWORLD) {
			digUpOverworld(level, pos);
		} else if (level.dimension() == Level.NETHER) {
			switch (level.getRandom().nextInt(3)) {
				case 0 -> rise(level, EntityTypes.ZOMBIFIED_PIGLIN, pos.below(), RISE);
				case 1 -> rise(level, EntityTypes.ZOGLIN, pos.below(), RISE);
				default -> rise(level, EntityTypes.WITHER_SKELETON, pos.below(), RISE);
			}
		}
		return InteractionResult.SUCCESS;
	}

	private static void digUpOverworld(ServerLevel level, BlockPos pos) {
		RandomSource random = level.getRandom();
		if (random.nextInt(5) == 0) {
			if (random.nextInt(3) == 0) {
				rise(level, BuiltInRegistries.ENTITY_TYPE.getValue(GravediggerIds.Entities.AMALGAM), pos.below(), RISE);
				return;
			}
			switch (random.nextInt(4)) {
				case 0 -> rise(level, EntityTypes.ZOMBIE_VILLAGER, pos.below(), RISE);
				case 1 -> rise(level, EntityTypes.ZOMBIE_HORSE, pos.below(), RISE);
				case 2 -> rise(level, EntityTypes.ENDERMAN, pos.below(), RISE);
				default -> rise(level, EntityTypes.SKELETON_HORSE, pos.below(), RISE);
			}
			return;
		}
		switch (random.nextInt(4)) {
			case 0 -> {
				Entity zombie = rise(level, EntityTypes.ZOMBIE, pos.below(), RISE);
				if (zombie instanceof Zombie holder && random.nextInt(50) == 0) giveWaddleDisc(holder);
			}
			case 1 -> rise(level, EntityTypes.SKELETON, pos.below(), RISE);
			case 2 -> rise(level, EntityTypes.SPIDER, pos, SPIDER_RISE);
			default -> rise(level, EntityTypes.CAVE_SPIDER, pos, SPIDER_RISE);
		}
	}

	/** One zombie in fifty comes up holding the Waddle disc, which it always drops. */
	private static void giveWaddleDisc(Zombie zombie) {
		zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(BuiltInRegistries.ITEM.getValue(MiscIds.Items.MUSIC_DISC_WADDLE)));
		zombie.setDropChance(EquipmentSlot.MAINHAND, 1.0F);
	}

	private static Entity rise(ServerLevel level, EntityType<?> type, BlockPos at, double speed) {
		Entity entity = type.spawn(level, at, EntitySpawnReason.MOB_SUMMONED);
		if (entity != null) entity.setDeltaMovement(0.0, speed, 0.0);
		return entity;
	}
}
