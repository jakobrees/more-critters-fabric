package com.morecritters.fabric.module.corpse_gear;

import com.morecritters.fabric.ids.FossilsIds;
import com.morecritters.fabric.ids.MightshroomIds;
import com.morecritters.fabric.ids.NightshroomIds;
import com.morecritters.fabric.ids.ShockCubeIds;
import com.morecritters.fabric.ids.ShriekbatIds;
import com.morecritters.fabric.ids.SnowflakeSpiderIds;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * A thrown hardtack biscuit, plain or infested (two entity types, one class). It crumbles on whatever it hits;
 * a creature it hits is stunned for three seconds: always by infested hardtack, half the time by plain.
 * Effect clouds, echoes and similar non-creatures just swallow it.
 */
public class ThrownHardtack extends GearProjectile {
	/** The original's throw power: the hardtack flies at twice it, and the twang's pitch rises by half of it. */
	private static final float POWER = 1.0F;
	private static final double DAMAGE = 1.0;
	private static final int KNOCKBACK = 2;
	private static final int STUN_TICKS = 60;
	/** Plain hardtack stuns when a roll of 1-10 comes up 5 or less. */
	private static final int PLAIN_STUN_ROLLS = 5;
	private static final int CRUMBS = 5;

	/** Other modules' entities the original exempted: helper entities that are not really creatures. */
	private static final Set<Identifier> SWALLOWERS = Set.of(NightshroomIds.Entities.ANCIENT_SKELETON, ShriekbatIds.Entities.ECHO,
		MightshroomIds.Entities.HEAL_ECHO, ShriekbatIds.Entities.LARGE_ECHO, NightshroomIds.Entities.MORI_ROOTS, ShockCubeIds.Entities.SHOCK_CUBE,
		ShockCubeIds.Entities.SHOCK_CUBE_SMALL, SnowflakeSpiderIds.Entities.WEB_ENTITY, ShriekbatIds.Entities.SHRIEKBOMB_PROJECTILE,
		FossilsIds.Entities.ANCIENT_SKELETON_EXHIBIT);

	public ThrownHardtack(EntityType<? extends ThrownHardtack> type, Level level) {
		super(type, level);
	}

	private ThrownHardtack(EntityType<? extends ThrownHardtack> type, Player thrower, ItemStack item) {
		super(type, thrower, thrower.level(), item);
	}

	/** Thrown from the player's eyes along their view, with a bow's twang. */
	static void throwFrom(Player thrower, boolean infested) {
		EntityType<ThrownHardtack> type = infested ? CorpseGearModule.THROWN_INFESTED_HARDTACK : CorpseGearModule.THROWN_HARDTACK;
		Level level = thrower.level();
		ThrownHardtack hardtack = new ThrownHardtack(type, thrower, new ItemStack(infested ? CorpseGearModule.INFESTED_HARDTACK : CorpseGearModule.HARDTACK));
		Vec3 view = thrower.getViewVector(1.0F);
		hardtack.shoot(view.x, view.y, view.z, POWER * 2.0F, 0.0F);
		hardtack.setSilent(true);
		hardtack.setCritArrow(false);
		hardtack.setBaseDamage(DAMAGE);
		if (thrower.hasInfiniteMaterials()) hardtack.pickup = Pickup.CREATIVE_ONLY;
		level.addFreshEntity(hardtack);
		float pitch = 1.0F / (level.getRandom().nextFloat() * 0.5F + 1.0F) + POWER / 2.0F;
		level.playSound(null, thrower.getX(), thrower.getY(), thrower.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, pitch);
	}

	private boolean isInfested() {
		return this.getType() == CorpseGearModule.THROWN_INFESTED_HARDTACK;
	}

	@Override
	protected Item item() {
		return isInfested() ? CorpseGearModule.INFESTED_HARDTACK : CorpseGearModule.HARDTACK;
	}

	@Override
	protected int knockback() {
		return KNOCKBACK;
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (!(this.level() instanceof ServerLevel level)) return;
		Entity target = hit.getEntity();
		if (SWALLOWERS.contains(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()))) {
			this.discard();
			return;
		}
		// The original crumbled plain hardtack here for both kinds.
		crumble(level, this.position(), CorpseGearModule.HARDTACK);
		if (target instanceof LivingEntity creature && stuns()) {
			creature.addEffect(new MobEffectInstance(CorpseGearModule.STUNNED, STUN_TICKS, 0, false, false));
		}
	}

	private boolean stuns() {
		return isInfested() || this.random.nextInt(10) < PLAIN_STUN_ROLLS;
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (this.level() instanceof ServerLevel level) crumble(level, cornerOf(hit.getBlockPos()), item());
	}

	/** Crumbs of the biscuit a block above the spot. */
	private static void crumble(ServerLevel level, Vec3 spot, Item biscuit) {
		level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, biscuit), true, false, spot.x, spot.y + 1.0, spot.z, CRUMBS, 0.2, 0.2, 0.2, 0.01);
	}
}
