package com.morecritters.fabric.module.mightshroom;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.ids.MiscIds;
import com.morecritters.fabric.ids.NightshroomIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Spawn Mightshroom: the ritual that raises an ancient skeleton (given purgatorial mixture) as a shroom.
 * While it lasts the bones shake in black and yellow stripes; twenty ticks before the end they come alive,
 * and when it wears off the skeleton bursts into flesh, mycelium and feathers and the shroom stands in
 * its place. Players nearby earn the advancement. A mightshroom given the effect plays its transform
 * animation and sound.
 */
public class SpawnMightshroomEffect extends MobEffect {
	private static final int COLOUR = -1;
	private static final int COME_ALIVE_AT = 20;
	private static final double WITNESS_RANGE = 15.0;
	private static final int FEATHER_BURSTS = 3, FEATHER_BURST_GAP = 2;

	public SpawnMightshroomEffect() {
		super(MobEffectCategory.NEUTRAL, COLOUR);
	}

	@Override
	public void onEffectStarted(LivingEntity entity, int amplifier) {
		if (entity.level().isClientSide()) {
			return;
		}
		if (entity instanceof ShroomRaisable body) {
			body.startRising();
		}
		if (entity instanceof MightshroomEntity mightshroom) {
			mightshroom.transform();
		}
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		if (!isRaisable(entity)) {
			return true;
		}
		int remaining = entity.getEffect(MightshroomModule.SPAWN_MIGHTSHROOM).getDuration();
		if (remaining == COME_ALIVE_AT && entity instanceof ShroomRaisable body) {
			body.comeAlive();
		}
		OtherModules.particles(level, MiscIds.Particles.BLACK_STRIPE, entity.getX(), entity.getY() + 1.0, entity.getZ(), 6, 1.0, 1.0, 1.0, 0.0);
		OtherModules.particles(level, MiscIds.Particles.YELLOW_STRIPE, entity.getX(), entity.getY() + 1.0, entity.getZ(), 6, 1.0, 1.0, 1.0, 0.0);
		if (remaining <= 1) {
			rise(level, entity);
			return false;
		}
		return true;
	}

	/** The ancient skeleton of the nightshroom module, by its interface or failing that by its id. */
	static boolean isRaisable(Entity entity) {
		return entity instanceof ShroomRaisable || OtherModules.isOfType(entity, NightshroomIds.Entities.ANCIENT_SKELETON);
	}

	private static void rise(ServerLevel level, LivingEntity body) {
		body.discard();
		AABB witnesses = new AABB(body.position(), body.position()).inflate(WITNESS_RANGE);
		for (Player player : level.getEntitiesOfClass(Player.class, witnesses)) {
			Advancements.award(player, MoreCritters.id("spawn_migthshroom"));
		}

		Identifier shroom = body instanceof ShroomRaisable raisable ? raisable.soup().shroom : ShroomRaisable.Soup.NONE.shroom;
		OtherModules.spawn(level, shroom, body.blockPosition()).ifPresent(risen -> faceLike(risen, body));

		double x = body.getX(), y = body.getY(), z = body.getZ();
		level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.ROTTEN_FLESH), true, false, x, y, z, 30, 0.5, 2.0, 0.5, 0.0);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MYCELIUM.defaultBlockState()), true, false, x, y, z, 30, 0.5, 2.0, 0.5, 0.0);
		Runnable feathers = () -> level.sendParticles(MightshroomModule.FEATHER, true, false, x, y + 2.0, z, 6, 0.7, 1.0, 0.7, 3.0);
		feathers.run();
		for (int burst = 1; burst < FEATHER_BURSTS; burst++) {
			ServerScheduler.runLater(burst * FEATHER_BURST_GAP, feathers);
		}
	}

	private static void faceLike(Entity risen, Entity body) {
		risen.setYRot(body.getYRot());
		risen.setYBodyRot(body.getYRot());
		risen.setYHeadRot(body.getYRot());
		risen.setDeltaMovement(0.0, 0.0, 0.0);
	}
}
