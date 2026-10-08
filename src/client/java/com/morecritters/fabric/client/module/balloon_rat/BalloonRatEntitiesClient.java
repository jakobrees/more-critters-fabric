package com.morecritters.fabric.client.module.balloon_rat;

import com.geckolib.renderer.layer.builtin.TextureLayerGeoLayer;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.morecritters.fabric.module.balloon_rat.BalloonRatEntities;
import com.morecritters.fabric.module.balloon_rat.BalloonRatEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.morecritters.fabric.module.balloon_rat.PinkMonsterEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** Renderers for the balloon rat (texture follows variant and inflation) and the glowing pink monster. */
final class BalloonRatEntitiesClient {
	static void register() {
		EntityRendererRegistry.register(BalloonRatEntities.BALLOON_RAT, BalloonRatRenderer::new);
		EntityRendererRegistry.register(BalloonRatEntities.PINK_MONSTER, context -> {
			CritterRenderer<PinkMonsterEntity> renderer = new CritterRenderer<>(context, "pink_monster", 0.0F, 2.0F);
			renderer.withRenderLayer(new TextureLayerGeoLayer<>(renderer, MoreCritters.id("textures/entities/pink_monster_glow.png"), RenderTypes::eyes));
			return renderer;
		});
	}

	/** Drawn at 1.2 when grown and 0.7 as a baby, as the original's size procedure did. */
	private static final class BalloonRatRenderer extends CritterRenderer<BalloonRatEntity> {
		private static final float ADULT_SCALE = 1.2F, BABY_SCALE = 0.7F;

		BalloonRatRenderer(EntityRendererProvider.Context context) {
			super(context, "balloon_rat", 0.4F, ADULT_SCALE);
		}

		@Override
		public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> pass, float widthScale, float heightScale) {
			float baby = pass.renderState().isBaby ? BABY_SCALE / ADULT_SCALE : 1.0F;
			super.scaleModelForRender(pass, widthScale * baby, heightScale * baby);
		}
	}

	private BalloonRatEntitiesClient() {}
}
