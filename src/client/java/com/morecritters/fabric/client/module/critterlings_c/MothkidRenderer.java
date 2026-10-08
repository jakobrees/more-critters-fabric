package com.morecritters.fabric.client.module.critterlings_c;

import com.geckolib.renderer.layer.builtin.TextureLayerGeoLayer;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.client.core.CritterRenderer;
import com.morecritters.fabric.module.critterlings_c.MothkidEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** The mothkid at 1.1x, with its glowing markings drawn full-bright over every rarity. */
public class MothkidRenderer extends CritterRenderer<MothkidEntity> {
	private static final Identifier GLOW = MoreCritters.id("textures/entities/mothkid_glow.png");

	public MothkidRenderer(EntityRendererProvider.Context context) {
		super(context, "mothkid", 0.3F, 1.1F);
		withRenderLayer(new TextureLayerGeoLayer<>(this, GLOW, RenderTypes::eyes));
	}
}
