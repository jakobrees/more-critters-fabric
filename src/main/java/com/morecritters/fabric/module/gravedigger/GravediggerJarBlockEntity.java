package com.morecritters.fabric.module.gravedigger;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Animates the gravedigger in its jar with the looping animation "0". */
public class GravediggerJarBlockEntity extends BlockEntity implements GeoBlockEntity {
	private static final RawAnimation FLOAT = RawAnimation.begin().thenLoop("0");

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);

	public GravediggerJarBlockEntity(BlockPos pos, BlockState state) {
		super(GravediggerModule.JAR_BLOCK_ENTITY, pos, state);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>("float", 0, test -> test.setAndContinue(FLOAT)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
