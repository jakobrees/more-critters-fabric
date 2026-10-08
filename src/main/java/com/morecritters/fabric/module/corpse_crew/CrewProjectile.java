package com.morecritters.fabric.module.corpse_crew;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Something the crew flings: an arrow-like shot with its own knockback that does not stay stuck
 * in its victim and vanishes once it lands.
 */
public abstract class CrewProjectile extends AbstractArrow implements ItemSupplier {
	private int knockback;

	protected CrewProjectile(EntityType<? extends CrewProjectile> type, Level level) {
		super(type, level);
	}

	public void setKnockback(int knockback) {
		this.knockback = knockback;
	}

	@Override
	protected void doKnockback(LivingEntity victim, DamageSource source) {
		super.doKnockback(victim, source);
		if (this.knockback > 0) {
			double resistance = Math.max(0.0, 1.0 - victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
			Vec3 push = this.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize().scale(this.knockback * 0.6 * resistance);
			if (push.lengthSqr() > 0.0) {
				victim.push(push.x, 0.1, push.z);
			}
		}
	}

	/** Takes back the arrow vanilla would leave sticking out of the victim. */
	@Override
	protected void doPostHurtEffects(LivingEntity victim) {
		super.doPostHurtEffects(victim);
		victim.setArrowCount(victim.getArrowCount() - 1);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.isInGround()) {
			this.discard();
		}
	}
}
