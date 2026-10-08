package com.morecritters.fabric.module.ship_fittings;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Animates the flag: loops the waving animation named by the block's {@code animation} ("0", "1" or "2"). When the
 * flag switches between its small and large model the animation restarts.
 */
public class JollyRogerBlockEntity extends BlockEntity implements GeoBlockEntity {
	private static final RawAnimation[] WAVES = {
		RawAnimation.begin().thenLoop("0"), RawAnimation.begin().thenLoop("1"), RawAnimation.begin().thenLoop("2")};

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	/** The model last animated, client side, to notice a switch between small and large. */
	private boolean wasLarge;

	public JollyRogerBlockEntity(BlockPos pos, BlockState state) {
		super(ShipFittingsModule.TATTERED_JOLLY_ROGER_BLOCK_ENTITY, pos, state);
		this.wasLarge = isLarge(state);
	}

	public static boolean isLarge(BlockState state) {
		return state.getValue(JollyRogerBlock.LARGE) == 1;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>("wave", 0, test -> {
			BlockState state = this.getBlockState();
			if (this.wasLarge != isLarge(state)) {
				this.wasLarge = isLarge(state);
				test.controller().reset();
				return PlayState.STOP;
			}
			int wave = state.getValue(JollyRogerBlock.ANIMATION);
			return wave < WAVES.length ? test.setAndContinue(WAVES[wave]) : PlayState.STOP;
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
