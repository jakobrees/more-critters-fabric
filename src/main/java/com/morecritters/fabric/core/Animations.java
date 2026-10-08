package com.morecritters.fabric.core;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;

/**
 * GeckoLib controllers in the two shapes every critter uses: a looping idle/walk
 * controller driven by movement, and a controller of one-shot actions that the
 * server triggers by name with {@code triggerAnim("actions", name)}.
 */
public final class Animations {
	public static final String MOVEMENT = "movement";
	public static final String ACTIONS = "actions";

	public static <T extends GeoAnimatable> AnimationController<T> idleWalk(T animatable, String idle, String walk) {
		RawAnimation idleLoop = RawAnimation.begin().thenLoop(idle);
		RawAnimation walkLoop = RawAnimation.begin().thenLoop(walk);
		return new AnimationController<>(MOVEMENT, 2, (AnimationTest<T> test) ->
			test.setAndContinue(test.isMoving() ? walkLoop : idleLoop));
	}

	/** Each named animation plays once when triggered; between triggers the controller is idle. */
	public static <T extends GeoAnimatable> AnimationController<T> actions(T animatable, String... names) {
		AnimationController<T> controller = new AnimationController<>(ACTIONS, 2, test -> PlayState.STOP);
		for (String name : names) {
			controller.triggerableAnim(name, RawAnimation.begin().thenPlay(name));
		}
		return controller;
	}

	private Animations() {}
}
