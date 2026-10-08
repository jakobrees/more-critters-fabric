package com.morecritters.fabric.module.bouncelizard;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Holds a clutch's hatch countdown, a little over a Minecraft day from when the clutch was placed. */
public class BouncelizardEggBlockEntity extends BlockEntity {
	private int hatchTimer = -1;

	public BouncelizardEggBlockEntity(BlockPos pos, BlockState state) {
		super(BouncelizardModule.EGG_BLOCK_ENTITY, pos, state);
	}

	void startCountdown() {
		this.hatchTimer = Mth.nextInt(this.level.getRandom(), 23_000, 25_000);
		setChanged();
	}

	/** Counts down one tick; true when the eggs should hatch. */
	boolean tickReady() {
		if (this.hatchTimer < 0) startCountdown();
		this.hatchTimer--;
		setChanged();
		return this.hatchTimer < 0;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("HatchTimer", this.hatchTimer);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.hatchTimer = input.getIntOr("HatchTimer", -1);
	}
}
