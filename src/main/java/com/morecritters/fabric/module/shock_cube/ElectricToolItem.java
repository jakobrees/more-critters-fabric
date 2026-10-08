package com.morecritters.fabric.module.shock_cube;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.SingletonGeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.GeoItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The taser and the tazegun: GeckoLib-drawn handhelds that idle on animation "0" and play a
 * one-shot animation when used (the original's {@code geckoAnim} custom data).
 */
abstract class ElectricToolItem extends Item implements GeoItem {
	private static final String IDLE_CONTROLLER = "idle";
	private static final String USE_CONTROLLER = "use";
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("0");

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private final String[] useAnimations;

	ElectricToolItem(Properties properties, String... useAnimations) {
		super(properties);
		this.useAnimations = useAnimations;
		SingletonGeoAnimatable.registerSyncedAnimatable(this);
	}

	/** Plays one of this tool's use animations in {@code player}'s hand. */
	protected void playAnimation(Player player, ItemStack stack, String name) {
		if (player.level() instanceof ServerLevel level) {
			triggerAnim(player, GeoItem.getOrAssignId(stack, level), USE_CONTROLLER, name);
		}
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(IDLE_CONTROLLER, 0, test -> test.setAndContinue(IDLE)));
		AnimationController<ElectricToolItem> use = new AnimationController<>(USE_CONTROLLER, 0, test -> PlayState.STOP);
		for (String name : useAnimations) {
			use.triggerableAnim(name, RawAnimation.begin().thenPlay(name));
		}
		controllers.add(use);
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
