package com.morecritters.fabric.client.module.snowflake_spider;

import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.snowflake_spider.WebEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Draws the freezing web at model size, translucent, with the original's half-block shadow. */
public class WebEntityRenderer extends CritterRenderer<WebEntity> {
	public WebEntityRenderer(EntityRendererProvider.Context context) {
		super(context, "web_entity", 0.5F, 1.0F);
	}
}
