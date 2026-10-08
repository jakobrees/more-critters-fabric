package com.morecritters.fabric.module.corpse_crew;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * A crew muster (what the corpse crew spawn egg places). On its first tick it vanishes and
 * leaves a full crew where it stood: one or two mates, a quartermaster, a tank, the captain and a
 * parrot, and one time in three a lookout; then it jostles everyone close by a little apart.
 */
public class CorpseCrewEntity extends Monster implements GeoEntity {
	private static final int LOOKOUT_ODDS = 3;
	private static final double JOSTLE = 0.1;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);

	public CorpseCrewEntity(EntityType<? extends CorpseCrewEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 10.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (this.level() instanceof ServerLevel level && !this.isRemoved()) {
			this.discard();
			muster(level);
		}
	}

	private void muster(ServerLevel level) {
		BlockPos pos = this.blockPosition();
		int mates = Mth.nextInt(this.random, 1, 2);
		for (int i = 0; i < mates; i++) {
			CorpseCrewModule.CORPSE_MATE.spawn(level, pos, EntitySpawnReason.MOB_SUMMONED);
		}
		CorpseCrewModule.CORPSE_QUARTERMASTER.spawn(level, pos, EntitySpawnReason.MOB_SUMMONED);
		CorpseCrewModule.CORPSE_TANK.spawn(level, pos, EntitySpawnReason.MOB_SUMMONED);
		CorpseCrewModule.CORPSE_CAPTAIN.spawn(level, pos, EntitySpawnReason.MOB_SUMMONED);
		CorpseCrewModule.CORPSE_PARROT.spawn(level, pos, EntitySpawnReason.MOB_SUMMONED);
		if (Mth.nextInt(this.random, 1, LOOKOUT_ODDS) == 1) {
			CorpseCrewModule.CORPSE_LOOKOUT.spawn(level, pos, EntitySpawnReason.MOB_SUMMONED);
		}
		for (LivingEntity crew : level.getEntitiesOfClass(LivingEntity.class, new AABB(this.position(), this.position()).inflate(2.0))) {
			crew.push(Mth.nextDouble(this.random, -JOSTLE, JOSTLE), 0.0, Mth.nextDouble(this.random, -JOSTLE, JOSTLE));
		}
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		RawAnimation idle = RawAnimation.begin().thenLoop("idle");
		controllers.add(new AnimationController<CorpseCrewEntity>(Animations.MOVEMENT, 4, test -> test.setAndContinue(idle)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
