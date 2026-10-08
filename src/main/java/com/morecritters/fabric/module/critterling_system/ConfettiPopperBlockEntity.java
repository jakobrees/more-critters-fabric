package com.morecritters.fabric.module.critterling_system;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Animates the confetti popper: idle ("0") until the block pops, then the pop ("1"), held while it stays powered. */
public class ConfettiPopperBlockEntity extends BlockEntity implements GeoBlockEntity {
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("0");
	private static final RawAnimation POP = RawAnimation.begin().thenPlay("1");

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);

	public ConfettiPopperBlockEntity(BlockPos pos, BlockState state) {
		super(CritterlingSystemModule.CONFETTI_POPPER_BLOCK_ENTITY, pos, state);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<ConfettiPopperBlockEntity>("popper", 0,
			test -> test.setAndContinue(getBlockState().getValue(ConfettiPopperBlock.POPPED) ? POP : IDLE)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
