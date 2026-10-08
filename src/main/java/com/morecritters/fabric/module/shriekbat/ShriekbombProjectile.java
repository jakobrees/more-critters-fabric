package com.morecritters.fabric.module.shriekbat;

import com.morecritters.fabric.ids.NightshroomIds;
import com.morecritters.fabric.ids.CustodianIds;
import com.morecritters.fabric.ids.FossilsIds;
import com.morecritters.fabric.ids.IropodIds;
import com.morecritters.fabric.ids.MightshroomIds;
import com.morecritters.fabric.ids.ShockCubeIds;
import com.morecritters.fabric.ids.SnowflakeSpiderIds;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A thrown shriek bomb. Where it lands it bursts into a large echo with a shriek, and every mob
 * within twenty blocks (bar shriekbats and a few constructs) runs to the spot. Hitting a creature
 * bursts it on that creature; hitting an echo or a similar effect entity just fizzles (any bomb within a block goes too).
 */
public class ShriekbombProjectile extends AbstractArrow implements ItemSupplier {
	private static final double LURE_REACH = 20.0, LURE_SPEED = 2.0;
	/** Effect-like entities of other modules that a bomb erases instead of bursting on. */
	private static final List<Identifier> ERASES = List.of(NightshroomIds.Entities.ANCIENT_SKELETON, MightshroomIds.Entities.HEAL_ECHO,
		NightshroomIds.Entities.MORI_ROOTS, ShockCubeIds.Entities.SHOCK_CUBE, ShockCubeIds.Entities.SHOCK_CUBE_SMALL,
		SnowflakeSpiderIds.Entities.WEB_ENTITY, FossilsIds.Entities.ANCIENT_SKELETON_EXHIBIT, CustodianIds.Entities.ANCIENT_CUSTODIAN,
		CustodianIds.Entities.CUSTODIAN, IropodIds.Entities.IROBALL, NightshroomIds.Entities.ROT_SPLASH);
	/** Mobs of other modules that ignore the lure. */
	private static final Set<Identifier> NOT_LURED = Set.of(NightshroomIds.Entities.ANCIENT_SKELETON, FossilsIds.Entities.ANCIENT_SKELETON_EXHIBIT,
		CustodianIds.Entities.ANCIENT_CUSTODIAN, CustodianIds.Entities.CUSTODIAN, NightshroomIds.Entities.ROT_SPLASH, IropodIds.Entities.IROBALL);

	public ShriekbombProjectile(EntityType<? extends ShriekbombProjectile> type, Level level) {
		super(type, level);
	}

	/** Thrown from the player's eyes along their view, like the original's 0.7 power, 2 damage arrow. */
	static void throwFrom(Player thrower) {
		ShriekbombProjectile bomb = new ShriekbombProjectile(ShriekbatModule.SHRIEKBOMB_PROJECTILE, thrower.level());
		bomb.setOwner(thrower);
		bomb.setPos(thrower.getX(), thrower.getEyeY() - 0.1, thrower.getZ());
		var view = thrower.getViewVector(1.0F);
		bomb.shoot(view.x, view.y, view.z, 1.4F, 0.0F);
		bomb.setSilent(true);
		bomb.setBaseDamage(2.0);
		if (thrower.hasInfiniteMaterials()) bomb.pickup = Pickup.CREATIVE_ONLY;
		thrower.level().addFreshEntity(bomb);
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		burst(hit.getEntity(), this.blockPosition());
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		burst(this.getOwner(), hit.getBlockPos());
	}

	/** The original decides by the creature hit, or by the thrower when it hits a block. */
	private void burst(@Nullable Entity cause, BlockPos at) {
		if (cause == null || !(this.level() instanceof ServerLevel level)) return;
		if (isErased(cause)) {
			// The original only takes the bombs here with it, never the echo or anything else nearby.
			level.getEntitiesOfClass(ShriekbombProjectile.class, new AABB(this.position(), this.position()).inflate(1.0)).forEach(Entity::discard);
			return;
		}
		ShriekbatModule.LARGE_ECHO.spawn(level, at.above(), EntitySpawnReason.MOB_SUMMONED);
		level.playSound(null, at, ShriekbatModule.BOMB_SHRIEK_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
		for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(at).inflate(LURE_REACH))) {
			if (!(mob instanceof ShriekbatEntity) && !NOT_LURED.contains(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()))) {
				mob.getNavigation().moveTo(at.getX(), at.getY(), at.getZ(), LURE_SPEED);
			}
		}
	}

	private static boolean isErased(Entity entity) {
		return entity.getType() == ShriekbatModule.ECHO || entity.getType() == ShriekbatModule.LARGE_ECHO
			|| entity instanceof ShriekbombProjectile
			|| ERASES.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
	}

	@Override
	public void tick() {
		super.tick();
		if (this.isInGround()) this.discard();
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		return new ItemStack(ShriekbatModule.SHRIEK_BOMB);
	}

	@Override
	public ItemStack getItem() {
		return new ItemStack(ShriekbatModule.SHRIEK_BOMB);
	}
}
