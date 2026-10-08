package com.morecritters.fabric.module.wandering_collector;

import com.morecritters.fabric.core.Config;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The wandering collector's pack beast: a saddled carrybug with 36 slots of chest space
 * (sneak-use to open) that a trusted player can ride and steer. Its colour follows the biome
 * it spawned in. With {@code can_remove_carrybug_saddle} on, shears take the saddle and chests
 * off, spilling the cargo and leaving a {@link CarrybugNoSaddleEntity}.
 */
public class CarrybugEntity extends CarrybugBaseEntity {
	public static final int INVENTORY_SIZE = 36;

	/** Biome to colour; anything not listed is plains. */
	private static final Map<ResourceKey<Biome>, String> BIOME_VARIANTS = Map.ofEntries(
		Map.entry(Biomes.SAVANNA, "savanna"), Map.entry(Biomes.SAVANNA_PLATEAU, "savanna"), Map.entry(Biomes.WINDSWEPT_SAVANNA, "savanna"),
		Map.entry(Biomes.DESERT, "desert"),
		Map.entry(Biomes.BADLANDS, "badlands"), Map.entry(Biomes.ERODED_BADLANDS, "badlands"), Map.entry(Biomes.WOODED_BADLANDS, "badlands"),
		Map.entry(Biomes.SNOWY_PLAINS, "tundra"), Map.entry(Biomes.SNOWY_TAIGA, "tundra"),
		Map.entry(Biomes.SWAMP, "swamp"), Map.entry(Biomes.MANGROVE_SWAMP, "swamp"),
		Map.entry(Biomes.JUNGLE, "jungle"), Map.entry(Biomes.BAMBOO_JUNGLE, "jungle"), Map.entry(Biomes.SPARSE_JUNGLE, "jungle"),
		Map.entry(Biomes.OCEAN, "ocean"), Map.entry(Biomes.DEEP_OCEAN, "ocean"), Map.entry(Biomes.COLD_OCEAN, "ocean"),
		Map.entry(Biomes.DEEP_COLD_OCEAN, "ocean"), Map.entry(Biomes.FROZEN_OCEAN, "ocean"), Map.entry(Biomes.DEEP_FROZEN_OCEAN, "ocean"),
		Map.entry(Biomes.LUKEWARM_OCEAN, "ocean"), Map.entry(Biomes.DEEP_LUKEWARM_OCEAN, "ocean"), Map.entry(Biomes.WARM_OCEAN, "ocean"),
		Map.entry(Biomes.TAIGA, "taiga"), Map.entry(Biomes.OLD_GROWTH_PINE_TAIGA, "taiga"), Map.entry(Biomes.OLD_GROWTH_SPRUCE_TAIGA, "taiga"),
		Map.entry(Biomes.DRIPSTONE_CAVES, "cave"), Map.entry(Biomes.LUSH_CAVES, "cave"));

	private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE);

	public CarrybugEntity(EntityType<? extends CarrybugEntity> type, Level level) {
		super(type, level);
	}

	@Override
	public String textureName() {
		return "carrybug_" + this.variant();
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		Holder<Biome> biome = level.getBiome(this.blockPosition());
		this.setVariant(biome.unwrapKey().map(key -> BIOME_VARIANTS.getOrDefault(key, "plains")).orElse("plains"));
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player.isSecondaryUseActive()) {
			if (!this.level().isClientSide()) {
				player.openMenu(new SimpleMenuProvider((id, playerInventory, opener) -> new CarrybugMenu(id, playerInventory, this.inventory, this),
					Component.literal("Carrybug")));
			}
			return InteractionResult.SUCCESS;
		}
		if (player.getMainHandItem().is(Items.SHEARS) && Config.flag("can_remove_carrybug_saddle", true)) {
			if (this.level() instanceof ServerLevel level) {
				shear(level, player);
			}
			return InteractionResult.SUCCESS;
		}
		if (!this.level().isClientSide()) {
			player.startRiding(this);
		}
		return InteractionResult.SUCCESS;
	}

	/** Drops the saddle, three chests and the cargo, and swaps this carrybug for a bare one of the same colour. */
	private void shear(ServerLevel level, Player player) {
		if (!player.hasInfiniteMaterials()) {
			player.getMainHandItem().hurtAndBreak(1, player, InteractionHand.MAIN_HAND);
		}
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		level.playSound(null, this.blockPosition(), SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 1.0F, 1.0F);

		drop(level, new ItemStack(Items.SADDLE));
		for (int i = 0; i < 3; i++) {
			drop(level, new ItemStack(Blocks.CHEST));
		}
		for (ItemStack stack : this.inventory.removeAllItems()) {
			drop(level, stack);
		}

		CarrybugNoSaddleEntity bare = CarrybugRegistry.CARRYBUG_NO_SADDLE.spawn(level, this.blockPosition(), EntitySpawnReason.MOB_SUMMONED);
		if (bare != null) {
			bare.setDeltaMovement(Vec3.ZERO);
			bare.setVariant(this.variant());
		}
		this.discard();
	}

	private void drop(ServerLevel level, ItemStack stack) {
		ItemEntity item = new ItemEntity(level, this.getX(), this.getY(), this.getZ(), stack);
		item.setPickUpDelay(10);
		level.addFreshEntity(item);
	}

	@Override
	protected void dropEquipment(ServerLevel level) {
		super.dropEquipment(level);
		for (ItemStack stack : this.inventory.removeAllItems()) {
			if (!EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
				this.spawnAtLocation(level, stack);
			}
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		ContainerHelper.saveAllItems(output, this.inventory.getItems());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		ContainerHelper.loadAllItems(input, this.inventory.getItems());
	}

	// Riding: the rider steers with forward/back only, at the carrybug's own walking speed.

	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		return this.getFirstPassenger() instanceof Player player ? player : super.getControllingPassenger();
	}

	@Override
	protected void tickRidden(Player controller, Vec3 riddenInput) {
		super.tickRidden(controller, riddenInput);
		this.setRot(controller.getYRot(), controller.getXRot() * 0.5F);
		this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
	}

	@Override
	protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
		return new Vec3(0.0, 0.0, controller.zza);
	}

	@Override
	protected float getRiddenSpeed(Player controller) {
		return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
	}
}
