package com.morecritters.fabric.client.module.critter_atlas;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.client.module.critter_atlas.AtlasPage.ButtonAction;
import com.morecritters.fabric.client.module.critter_atlas.AtlasPage.Model;
import com.morecritters.fabric.client.module.critter_atlas.AtlasPage.Picture;
import com.morecritters.fabric.client.module.critter_atlas.AtlasPage.Tooltip;
import com.morecritters.fabric.module.critterling_system.CritterlingCollection;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws any page of the critter atlas from {@link AtlasPages}: the open book and its text panels,
 * the critters standing on the page (turning to follow the mouse), hover tooltips and the buttons
 * that turn pages. Turning a page reopens this screen on the new page, on the client only.
 */
public class CritterAtlasScreen extends Screen {
	/**
	 * The corpse crew member picked with the number buttons. The original kept it on the player,
	 * so it outlasts closing the book; here it lasts for the game session.
	 */
	private static int shownTab = 0;

	private final AtlasPage page;
	/** The page's models, created once per page opening; null where the entity type is not registered. */
	private final List<LivingEntity> modelEntities = new ArrayList<>();
	private int leftPos, topPos;

	public CritterAtlasScreen(String pageName) {
		super(Component.translatable("item.more_critters.critter_atlas"));
		this.page = AtlasPages.get(pageName);
	}

	@Override
	protected void init() {
		this.leftPos = (this.width - this.page.width()) / 2;
		this.topPos = (this.height - this.page.height()) / 2;
		if (this.modelEntities.isEmpty()) {
			for (Model model : this.page.models()) this.modelEntities.add(createModel(model.entityType()));
		}
		for (AtlasPage.Button button : this.page.buttons()) {
			addRenderableWidget(new PageButton(button, this.leftPos, this.topPos, () -> press(button.action())));
		}
	}

	/** Ids for the screen's own entities: negative, so they never collide with the server's. */
	private static int nextModelId = -1;

	/** An entity of the given type that only this screen knows of, or null if no module registered it. */
	private LivingEntity createModel(Identifier entityType) {
		LivingEntity entity = BuiltInRegistries.ENTITY_TYPE.getOptional(entityType)
			.map(type -> type.create(this.minecraft.level, new EntitySpawnRequest(EntitySpawnReason.LOAD, true)))
			.filter(LivingEntity.class::isInstance)
			.map(LivingEntity.class::cast)
			.orElse(null);
		// Rendering a living entity asks for its id (held items); an entity never added to a level has none.
		if (entity != null) entity.setId(nextModelId--);
		return entity;
	}

	private void press(ButtonAction action) {
		switch (action) {
			case ButtonAction.TurnTo turn -> {
				playSound(SoundEvents.BOOK_PAGE_TURN);
				this.minecraft.gui.setScreen(new CritterAtlasScreen(turn.page()));
			}
			case ButtonAction.ShowTab tab -> shownTab = tab.tab();
			case ButtonAction.PlaySound sound -> BuiltInRegistries.SOUND_EVENT.getOptional(sound.sound()).ifPresent(this::playSound);
		}
	}

	private void playSound(SoundEvent sound) {
		this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F));
	}

	@Override
	public void tick() {
		// Lets the models' idle animations run, as they would for a living mob.
		for (LivingEntity entity : this.modelEntities) {
			if (entity != null) entity.tickCount++;
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		for (Picture picture : this.page.pictures()) {
			if (picture.collectedCritterling() != null
				&& !CritterlingCollection.hasCollected(this.minecraft.player, picture.collectedCritterling())) continue;
			graphics.blit(RenderPipelines.GUI_TEXTURED, MoreCritters.id(picture.texture()),
				this.leftPos + picture.x(), this.topPos + picture.y(), 0.0F, 0.0F,
				picture.width(), picture.height(), picture.width(), picture.height());
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		// Screens are handed the time since the last frame; the models' animations need the partial tick.
		float partialTick = this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		for (int i = 0; i < this.page.models().size(); i++) {
			Model model = this.page.models().get(i);
			LivingEntity entity = this.modelEntities.get(i);
			if (entity != null && (model.tab() == Model.ALWAYS || model.tab() == shownTab)) {
				drawModel(graphics, model, entity, mouseX, mouseY, partialTick);
			}
		}
		for (Tooltip tooltip : this.page.tooltips()) {
			if (mouseX > this.leftPos + tooltip.x0() && mouseX < this.leftPos + tooltip.x1()
				&& mouseY > this.topPos + tooltip.y0() && mouseY < this.topPos + tooltip.y1()) {
				graphics.setTooltipForNextFrame(this.font, Component.translatable(tooltip.key()), mouseX, mouseY);
			}
		}
	}

	/**
	 * The entity with its feet at the model's spot, body and head turned towards the mouse the way
	 * the inventory's player follows it. Drawn into a box covering the whole screen, so tall critters
	 * (the mightshroom, the custodian) are not cut off; the original did not clip them either.
	 */
	private void drawModel(GuiGraphicsExtractor graphics, Model model, LivingEntity entity, int mouseX, int mouseY, float partialTick) {
		int feetX = this.leftPos + model.x();
		int feetY = this.topPos + model.y();
		float turn = (float) Math.atan((feetX - mouseX) / 40.0);
		float tilt = (float) Math.atan((this.topPos + model.lookY() - mouseY) / 40.0);
		Quaternionf cameraTilt = new Quaternionf().rotateX(tilt * 20.0F * Mth.DEG_TO_RAD);
		Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).mul(cameraTilt);

		EntityRenderState state = this.minecraft.getEntityRenderDispatcher().extractEntity(entity, partialTick);
		state.shadowPieces.clear();
		state.outlineColor = 0;
		if (state instanceof LivingEntityRenderState living) {
			living.bodyRot = 180.0F + turn * 20.0F;
			living.yRot = turn * 20.0F;
			living.xRot = -tilt * 20.0F;
		}

		int scale = model.scale();
		// The box's centre is the drawing origin; move the feet from there to (feetX, feetY).
		Vector3f feetOffset = new Vector3f((feetX - this.width / 2.0F) / scale, (feetY - this.height / 2.0F) / scale, 0.0F);
		graphics.entity(state, scale, feetOffset, rotation, cameraTilt, 0, 0, this.width, this.height);
	}

	/** The inventory key closes the book too, as it closed the original's container screens. */
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.minecraft.options.keyInventory.matches(event)) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean isInGameUi() {
		return true;
	}

	/** An image button whose texture holds the normal look above the hovered look. */
	private static final class PageButton extends AbstractButton {
		private final Identifier texture;
		private final Runnable onPress;

		PageButton(AtlasPage.Button button, int leftPos, int topPos, Runnable onPress) {
			super(leftPos + button.x(), topPos + button.y(), button.width(), button.height(), Component.empty());
			this.texture = MoreCritters.id(button.texture());
			this.onPress = onPress;
		}

		@Override
		public void onPress(InputWithModifiers input) {
			this.onPress.run();
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
			float v = isHoveredOrFocused() ? this.height : 0.0F;
			graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, getX(), getY(), 0.0F, v,
				this.width, this.height, this.width, this.height * 2);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			defaultButtonNarrationText(output);
		}
	}
}
