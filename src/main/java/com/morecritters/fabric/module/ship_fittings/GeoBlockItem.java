package com.morecritters.fabric.module.ship_fittings;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.GeoItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

/** The ship wheel and jolly roger in hand: drawn from the block's geo model, looping its idle animation "0". */
public final class GeoBlockItem extends BlockItem implements GeoItem {
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("0");

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);

	GeoBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>("idle", 0, test -> test.setAndContinue(IDLE)));
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
