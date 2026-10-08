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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Holds the wheel's redstone level and plays its idle and spin animations, chosen by the block's {@code animation}. */
public class ShipWheelBlockEntity extends BlockEntity implements GeoBlockEntity {
	private static final int MAX_POWER = 15;
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("0");
	private static final RawAnimation SPIN_UP = RawAnimation.begin().thenPlay("1");
	private static final RawAnimation SPIN_DOWN = RawAnimation.begin().thenPlay("2");

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private int power;

	public ShipWheelBlockEntity(BlockPos pos, BlockState state) {
		super(ShipFittingsModule.SHIP_WHEEL_BLOCK_ENTITY, pos, state);
	}

	int power() {
		return this.power;
	}

	/** Turns the wheel one notch up or down; false if it is already at the end. */
	boolean turn(int notches) {
		int turned = this.power + notches;
		if (turned < 0 || turned > MAX_POWER) return false;
		this.power = turned;
		this.setChanged();
		return true;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.power = input.getIntOr("level", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("level", this.power);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>("spin", 0, test -> switch (this.getBlockState().getValue(ShipWheelBlock.ANIMATION)) {
			case 0 -> test.setAndContinue(IDLE);
			case ShipWheelBlock.SPIN_UP -> test.setAndContinue(SPIN_UP);
			case ShipWheelBlock.SPIN_DOWN -> test.setAndContinue(SPIN_DOWN);
			default -> PlayState.STOP;
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
