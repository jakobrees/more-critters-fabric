package com.morecritters.fabric.module.critter_atlas;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * A model that exists only to be drawn on an atlas page: the closed atlas itself, or a critter in the
 * pose the atlas shows it in. The screen creates one on the client and never adds it to a world;
 * one summoned into a world removes itself, as the original's did on spawn.
 */
public class AtlasDisplayModel extends Mob implements GeoEntity {
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private final boolean hasIdle;

	/** {@code hasIdle}: whether the model's animation file has an "idle" loop to play. */
	public AtlasDisplayModel(EntityType<? extends AtlasDisplayModel> type, Level level, boolean hasIdle) {
		super(type, level);
		this.hasIdle = hasIdle;
		setNoAi(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes();
	}

	@Override
	public void tick() {
		if (!level().isClientSide()) {
			discard();
			return;
		}
		super.tick();
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		if (this.hasIdle) {
			controllers.add(new AnimationController<>(Animations.MOVEMENT, 2, test -> test.setAndContinue(IDLE)));
		}
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
