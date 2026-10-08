package com.morecritters.fabric.module.shock_cube;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The tazegun's ball of lightning. It trails sparks, and wherever it hits (a mob or a wall) it
 * bursts: a shock cube appears, and everything alive within five blocks is struck and electrocuted.
 */
public class ThunderballProjectile extends AbstractArrow {
	private static final double BURST_RANGE = 5.0;
	private static final float BURST_DAMAGE = 3.0F;
	private static final int ELECTROCUTE_TICKS = 200;

	private int knockback;

	public ThunderballProjectile(EntityType<? extends ThunderballProjectile> type, Level level) {
		super(type, level);
	}

	/** Fired by {@code shooter} from just below its eyes, flying where it looks. */
	public static void fire(LivingEntity shooter, double damage, int knockback, float speed) {
		ThunderballProjectile ball = new ThunderballProjectile(ShockCubeModule.THUNDERBALL, shooter.level());
		ball.setOwner(shooter);
		ball.setBaseDamage(damage);
		ball.knockback = knockback;
		ball.setSilent(true);
		ball.setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
		Vec3 look = shooter.getLookAngle();
		ball.shoot(look.x, look.y, look.z, speed, 0.0F);
		shooter.level().addFreshEntity(ball);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel level) {
			level.sendParticles(ShockCubeModule.ZAP_SPARK, this.getX(), this.getY(), this.getZ(), 5, 0.2, 0.2, 0.2, 0.02);
			if (this.isInGround()) {
				this.discard();
			}
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		burst(this.blockPosition());
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		burst(hit.getBlockPos());
	}

	/** Leaves a shock cube, and strikes and electrocutes every living thing around. */
	private void burst(BlockPos pos) {
		if (!(this.level() instanceof ServerLevel level)) return;
		ShockCubeModule.SHOCK_CUBE.spawn(level, pos, EntitySpawnReason.MOB_SUMMONED);
		level.playSound(null, pos, ShockCubeModule.TAZEGUN_EXPLODE_SOUND, SoundSource.BLOCKS, 3.0F, 1.0F);
		Vec3 centre = Vec3.atLowerCornerOf(pos);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(BURST_RANGE))) {
			target.addEffect(new MobEffectInstance(ShockCubeModule.ELECTROCUTED, ELECTROCUTE_TICKS, 0, false, false));
			target.hurtServer(level, level.damageSources().lightningBolt(), BURST_DAMAGE);
		}
		this.discard();
	}

	/** Extra push away from the shot on top of the arrow's own, as the original's knockback setting did. */
	@Override
	protected void doKnockback(LivingEntity target, DamageSource source) {
		super.doKnockback(target, source);
		if (knockback <= 0) return;
		double resistance = Math.max(0.0, 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
		Vec3 push = this.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize().scale(knockback * 0.6 * resistance);
		if (push.lengthSqr() > 0.0) {
			target.push(push.x, 0.1, push.z);
		}
	}

	/** The ball does not stick in its victim like an arrow. */
	@Override
	protected void doPostHurtEffects(LivingEntity target) {
		super.doPostHurtEffects(target);
		target.setArrowCount(target.getArrowCount() - 1);
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		return ItemStack.EMPTY;
	}
}
