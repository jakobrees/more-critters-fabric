package com.morecritters.fabric.client.core;

import com.morecritters.fabric.MoreCritters;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.ItemLike;

/**
 * Worn armour drawn with its own model and texture instead of a flat equipment layer, as the original's armour model
 * classes (with {@code getHumanoidArmorModel} and {@code getArmorTexture}) did.
 *
 * <p>The layer's top-level parts must be named like the humanoid model's ({@code head}, {@code body},
 * {@code right_arm}, {@code left_arm}, {@code right_leg}, {@code left_leg}): each one takes the wearer's pose for the
 * part of that name, so the part's own pivot is replaced by the wearer's, exactly as the original copied the
 * humanoid model's poses onto its armour model.
 */
public final class ModelArmor {
	/**
	 * Draws {@code item}, worn in its equipment slot, with the model {@code layer} (registered as
	 * {@code more_critters:<layerName>}) and the full texture path {@code texture}.
	 */
	public static void register(ItemLike item, String layerName, ModelLayerRegistry.TexturedLayerDefinitionProvider layer, Identifier texture) {
		ModelLayerLocation location = new ModelLayerLocation(MoreCritters.id(layerName), "main");
		ModelLayerRegistry.registerModelLayer(location, layer);
		ArmorRenderer.register(context -> renderer(new Model.Simple(context.bakeLayer(location), RenderTypes::armorCutoutNoCull), texture), item);
	}

	/** {@code textures/models/armor/<name>.png}, where the original kept the textures of its reshaped helmets and chestplate. */
	public static Identifier armorTexture(String name) {
		return MoreCritters.id("textures/models/armor/" + name + ".png");
	}

	private static ArmorRenderer renderer(Model<Unit> model, Identifier texture) {
		return (poseStack, collector, stack, state, slot, lightCoords, wearerModel) -> {
			// Only where the piece is meant to be worn (a command can put a coat on someone's head).
			Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
			if (equippable == null || equippable.slot() != slot) return;
			RenderType renderType = stack.hasFoil() ? RenderTypes.armorCutoutNoCullGlint(texture) : RenderTypes.armorCutoutNoCull(texture);
			ArmorRenderer.submitTransformCopyingModel(wearerModel, state, model, Unit.INSTANCE, false, collector, poseStack, renderType,
				lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		};
	}

	private ModelArmor() {}
}
