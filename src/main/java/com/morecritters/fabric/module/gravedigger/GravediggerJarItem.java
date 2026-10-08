package com.morecritters.fabric.module.gravedigger;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.GeoItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

/** The jar in hand and in the inventory: drawn from the same geo model as the placed block, floating. */
public final class GravediggerJarItem extends BlockItem implements GeoItem {
	private static final RawAnimation FLOAT = RawAnimation.begin().thenLoop("0");

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);

	GravediggerJarItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>("float", 0, test -> test.setAndContinue(FLOAT)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animations;
	}

	@Override
	public Object getRenderProvider() {
		return GeoItems.providerFor(this);
	}
}
