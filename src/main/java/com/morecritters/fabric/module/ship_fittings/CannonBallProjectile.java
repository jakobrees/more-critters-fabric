package com.morecritters.fabric.module.ship_fittings;

import com.morecritters.fabric.core.ServerScheduler;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A cannon ball fired by a cannon. It flies straight and fast trailing smoke (slime for a slime ball), and wherever
 * it hits something, or a player touches it, it blows up with the strength of a TNT block and its infusion bursts.
 * The six entity types share this class; the type says which infusion the ball carries.
 */
public class CannonBallProjectile extends AbstractArrow implements ItemSupplier {
	private static final double DAMAGE = 5.0;
	private static final int KNOCKBACK = 2;
	private static final float SPEED = 2.0F;
	private static final float EXPLOSION_POWER = 4.0F;
	/** The trail appears where the ball was this many ticks ago. */
	private static final int TRAIL_DELAY = 5;

	public CannonBallProjectile(EntityType<? extends CannonBallProjectile> type, Level level) {
		super(type, level);
	}

	/** Fires a ball out of the cannon's muzzle, the block in front of it. */
	static void fireFrom(ServerLevel level, BlockPos cannon, Direction facing, Infusion infusion) {
		CannonBallProjectile ball = new CannonBallProjectile(ShipFittingsModule.CANNON_BALL_PROJECTILES.get(infusion), level);
		ball.setBaseDamage(DAMAGE);
		ball.setSilent(true);
		Vec3 muzzle = Vec3.atCenterOf(cannon.relative(facing));
		ball.setPos(muzzle);
		ball.shoot(facing.getStepX(), facing.getStepY(), facing.getStepZ(), SPEED, 0.0F);
		level.addFreshEntity(ball);
	}

	private Infusion infusion() {
		for (Map.Entry<Infusion, EntityType<CannonBallProjectile>> entry : ShipFittingsModule.CANNON_BALL_PROJECTILES.entrySet()) {
			if (entry.getValue() == this.getType()) return entry.getKey();
		}
		return Infusion.NONE;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel level) {
			Vec3 at = this.position();
			var trail = this.infusion().trail();
			ServerScheduler.runLater(TRAIL_DELAY, () -> level.sendParticles(trail, true, false, at.x, at.y, at.z, 4, 0.2, 0.2, 0.2, 0.01));
		}
		if (this.isInGround()) {
			this.discard();
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		this.blowUp(this.position());
	}

	/** Bursts at the corner of the block it struck, as in the original. */
	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		this.blowUp(Vec3.atLowerCornerOf(hit.getBlockPos()));
	}

	@Override
	public void playerTouch(Player player) {
		super.playerTouch(player);
		this.blowUp(this.position());
	}

	private void blowUp(Vec3 at) {
		if (!(this.level() instanceof ServerLevel level)) return;
		level.explode(null, at.x, at.y, at.z, EXPLOSION_POWER, Level.ExplosionInteraction.BLOCK);
		this.discard();
		this.infusion().burst(level, at);
	}

	/** Pushes what it hits back like a Punch II arrow. */
	@Override
	protected void doKnockback(LivingEntity target, DamageSource source) {
		super.doKnockback(target, source);
		double resistance = Math.max(0.0, 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
		Vec3 push = this.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize().scale(KNOCKBACK * 0.6 * resistance);
		if (push.lengthSqr() > 0.0) {
			target.push(push.x, 0.1, push.z);
		}
	}

	/** A cannon ball does not stay stuck in what it hits. */
	@Override
	protected void doPostHurtEffects(LivingEntity target) {
		super.doPostHurtEffects(target);
		target.setArrowCount(target.getArrowCount() - 1);
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		return new ItemStack(ShipFittingsModule.CANNON_BALLS.get(this.infusion()));
	}

	@Override
	public ItemStack getItem() {
		return this.getDefaultPickupItem();
	}
}
