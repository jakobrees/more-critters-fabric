package com.morecritters.fabric.module.nightshroom;

import com.morecritters.fabric.ids.MightshroomIds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A lump of rot thrown by a frightshroom or a rot splash. It trails rot, covers whatever it hits
 * in rot, and bursts where it lands; one in seven raises a rot zombie there. It never sticks in
 * the ground and cannot be picked up.
 */
public class RotPieceEntity extends AbstractArrow {
	private static final int ROT_ZOMBIE_CHANCE = 7;
	private static final int ROT_COVERED_TICKS = 60;

	/** Extra knockback on a hit, as a Punch level would give. */
	private int knockback;

	public RotPieceEntity(EntityType<? extends RotPieceEntity> type, Level level) {
		super(type, level);
	}

	/** A silent rot piece, not yet aimed or added to the level. */
	static RotPieceEntity create(Level level, @Nullable Entity owner, double damage, int knockback) {
		RotPieceEntity piece = new RotPieceEntity(NightshroomModule.ROT_PIECE, level);
		piece.setOwner(owner);
		piece.setBaseDamage(damage);
		piece.knockback = knockback;
		piece.setSilent(true);
		return piece;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.level().addParticle(NightshroomModule.ROT_PARTICLE,
				this.getX() + Mth.nextDouble(this.random, -0.2, 0.2),
				this.getY() + Mth.nextDouble(this.random, -0.2, 0.2),
				this.getZ() + Mth.nextDouble(this.random, -0.2, 0.2), 0.0, 0.0, 0.0);
		}
		if (this.isInGround()) this.discard();
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (!this.level().isClientSide() && hit.getEntity() instanceof LivingEntity victim) {
			victim.addEffect(new MobEffectInstance(NightshroomModule.ROT_COVERED, ROT_COVERED_TICKS, 0, false, false));
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (!(this.level() instanceof ServerLevel level)) return;
		BlockPos block = hit.getBlockPos();
		Vec3 above = Vec3.atLowerCornerOf(block).add(0.0, 1.0, 0.0);
		if (this.random.nextInt(ROT_ZOMBIE_CHANCE) == 0) raiseRotZombie(level, block, above);
		Bursts.rot(level, above, 25, 0.1, 0.5, 0.1, 0.05);
	}

	private void raiseRotZombie(ServerLevel level, BlockPos block, Vec3 above) {
		RotZombieEntity zombie = NightshroomModule.ROT_ZOMBIE.spawn(level, block.above(), EntitySpawnReason.MOB_SUMMONED);
		if (zombie != null) zombie.setDeltaMovement(0.0, 0.0, 0.0);
		level.playSound(null, block, NightshroomModule.ROT_ZOMBIE_SPAWN_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
		Bursts.rot(level, above, 14, 0.3, 0.4, 0.3, 0.06);
	}

	@Override
	protected void doKnockback(LivingEntity victim, DamageSource source) {
		super.doKnockback(victim, source);
		if (this.knockback <= 0) return;
		double resistance = Math.max(0.0, 1.0 - victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
		Vec3 push = this.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize().scale(this.knockback * 0.6 * resistance);
		if (push.lengthSqr() > 0.0) victim.push(push.x, 0.1, push.z);
	}

	/** Leaves no rot pieces stuck in whatever it hits. */
	@Override
	protected void doPostHurtEffects(LivingEntity victim) {
		super.doPostHurtEffects(victim);
		victim.setArrowCount(victim.getArrowCount() - 1);
	}

	/** It carries the look of a mori shroom block, as the original's did; nothing ever picks it up. */
	@Override
	protected ItemStack getDefaultPickupItem() {
		return new ItemStack(OtherModules.item(MightshroomIds.Blocks.MORI_SHROOM_BLOCK).orElse(Items.AIR));
	}
}
