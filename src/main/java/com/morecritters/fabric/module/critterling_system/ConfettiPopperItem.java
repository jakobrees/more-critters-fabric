package com.morecritters.fabric.module.critterling_system;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.GeoItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

/** The confetti popper in hand: the block's geo model, standing still. */
public final class ConfettiPopperItem extends BlockItem implements GeoItem {
	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);

	ConfettiPopperItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}

	@Override
	public Object getRenderProvider() {
		return GeoItems.providerFor(this);
	}
}
