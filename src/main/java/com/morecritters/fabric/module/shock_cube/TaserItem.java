package com.morecritters.fabric.module.shock_cube;

import com.morecritters.fabric.ids.CritterlingsBIds;
import com.morecritters.fabric.ids.CritterlingsAIds;
import com.morecritters.fabric.ids.NightshroomIds;
import com.morecritters.fabric.ids.BalloonRatIds;
import com.morecritters.fabric.ids.FossilsIds;
import com.morecritters.fabric.ids.MightshroomIds;
import com.morecritters.fabric.ids.ShockCubeIds;
import com.morecritters.fabric.ids.ShriekbatIds;
import com.morecritters.fabric.ids.SnowflakeSpiderIds;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * The taser: right-click to shock every living thing within six blocks for 5–9 damage, apart
 * from your own pets and a list of critters it leaves alone. And whenever something gets hurt
 * near a player holding a taser, a line of sparks runs from the player to it.
 */
public class TaserItem extends ElectricToolItem {
	private static final double TASE_RANGE = 6.0;
	/** The original only tases when there is a living thing in a 12-block box around the player. */
	private static final double PRESENCE_BOX = 12.0;
	private static final int COOLDOWN = 20;
	private static final int SPARK_STEPS = 20;
	private static final double SPARK_STEP = 0.05;

	/** Critters the taser does not touch. */
	private static final Set<Identifier> SPARED = Set.of(
		ShriekbatIds.Entities.ECHO, ShriekbatIds.Entities.LARGE_ECHO, BalloonRatIds.Entities.PINK_MONSTER,
		SnowflakeSpiderIds.Entities.WEB_ENTITY, ShockCubeIds.Entities.SHOCK_CUBE, ShockCubeIds.Entities.SHOCK_CUBE_SMALL,
		NightshroomIds.Entities.ANCIENT_SKELETON, MightshroomIds.Entities.HEAL_ECHO, MightshroomIds.Entities.SMALL_HEAL_ECHO,
		MightshroomIds.Entities.MIGHTSHROOM_ECHO, CritterlingsAIds.Entities.CUBEFROG, CritterlingsBIds.Entities.ROLLBALL,
		CritterlingsBIds.Entities.SCOWL, CritterlingsAIds.Entities.PLAINSWYRM, CritterlingsAIds.Entities.DUNGER,
		CritterlingsAIds.Entities.SNEK, CritterlingsBIds.Entities.OPALCRAB, FossilsIds.Entities.ANCIENT_SKELETON_EXHIBIT
	);

	TaserItem(Properties properties) {
		super(properties, "taze");
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack taser = player.getItemInHand(hand);
		if (level instanceof ServerLevel serverLevel && !player.getCooldowns().isOnCooldown(taser)) {
			tase(serverLevel, player, taser);
		}
		return super.use(level, player, hand);
	}

	private void tase(ServerLevel level, Player player, ItemStack taser) {
		if (level.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(player.position(), PRESENCE_BOX, PRESENCE_BOX, PRESENCE_BOX)).isEmpty()) return;
		level.playSound(null, player.blockPosition(), ShockCubeModule.TASER_TASE_SOUND, SoundSource.PLAYERS, 2.0F, 1.0F);
		AABB reach = new AABB(player.position(), player.position()).inflate(TASE_RANGE);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, reach, target -> canTase(player, target))) {
			playAnimation(player, taser, "taze");
			player.getCooldowns().addCooldown(taser, COOLDOWN);
			if (!player.hasInfiniteMaterials()) {
				taser.hurtAndBreak(1, level, null, broken -> {});
			}
			target.hurtServer(level, level.damageSources().generic(), (float) Mth.nextDouble(level.getRandom(), 5.0, 9.0));
			level.sendParticles(ShockCubeModule.ZAP, target.getX(), target.getY(), target.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
		}
	}

	private static boolean canTase(Player player, LivingEntity target) {
		if (target == player) return false;
		if (target instanceof TamableAnimal pet && pet.isOwnedBy(player)) return false;
		return !SPARED.contains(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()));
	}

	static void registerEvents() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) -> {
			drawSparkLine(victim);
			return true;
		});
	}

	/** When something is hurt and the nearest player holds a taser, sparks trace the line between them. */
	private static void drawSparkLine(LivingEntity victim) {
		if (!(victim.level() instanceof ServerLevel level)) return;
		List<Player> players = level.getEntitiesOfClass(Player.class, AABB.ofSize(victim.position(), PRESENCE_BOX, PRESENCE_BOX, PRESENCE_BOX));
		Player nearest = players.stream().min(Comparator.comparingDouble(victim::distanceToSqr)).orElse(null);
		if (nearest == null || !nearest.getMainHandItem().is(ShockCubeModule.TASER)) return;
		double trackX = nearest.getX() - victim.getX();
		double trackY = nearest.getY() - victim.getY() - victim.getBbHeight() * 0.75;
		double trackZ = nearest.getZ() - victim.getZ();
		double startY = nearest.getY() + nearest.getBbHeight() * 0.75;
		for (int step = 0; step < SPARK_STEPS; step++) {
			double along = -step * SPARK_STEP;
			level.sendParticles(ShockCubeModule.ZAP_SPARK, nearest.getX() + trackX * along, startY + trackY * along,
				nearest.getZ() + trackZ * along, 5, 0.05, 0.05, 0.05, 0.0);
		}
	}
}
