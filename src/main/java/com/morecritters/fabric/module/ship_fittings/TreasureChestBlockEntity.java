package com.morecritters.fabric.module.ship_fittings;

import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The treasure chest's 27-slot store, shared by its locked, opening and open stages. The chest fills from its loot
 * table; only the first nine slots are ever shown or spilled. Hoppers can put items in but never take them out, and
 * breaking the chest does not spill it. The opening and open stages count down to their next step.
 */
public class TreasureChestBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
	private static final int SIZE = 27;
	/** The slots shown in the chest's screen and spilled when it opens. */
	static final int TREASURE_SLOTS = 9;
	private static final int[] ALL_SLOTS = IntStream.range(0, SIZE).toArray();
	/** Ticks the open chest lasts before it crumbles. */
	private static final int CRUMBLE_TICKS = 40;

	private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
	/** Ticks until the next stage: the lid coming off, or the open chest crumbling. */
	private int countdown;

	public TreasureChestBlockEntity(BlockPos pos, BlockState state) {
		super(typeFor(state), pos, state);
	}

	private static BlockEntityType<TreasureChestBlockEntity> typeFor(BlockState state) {
		if (state.is(ShipFittingsModule.TREASURE_CHEST_OPENING)) return ShipFittingsModule.TREASURE_CHEST_OPENING_BLOCK_ENTITY;
		if (state.is(ShipFittingsModule.TREASURE_CHEST_OPEN)) return ShipFittingsModule.TREASURE_CHEST_OPEN_BLOCK_ENTITY;
		return ShipFittingsModule.TREASURE_CHEST_BLOCK_ENTITY;
	}

	void startCountdown(int ticks) {
		this.countdown = ticks;
		this.setChanged();
	}

	/** Moves the locked chest's contents (or its not yet rolled loot table) into this one. */
	void takeContentsOf(TreasureChestBlockEntity other) {
		this.items = other.items;
		this.lootTable = other.lootTable;
		this.lootTableSeed = other.lootTableSeed;
		other.items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		other.lootTable = null;
		this.setChanged();
	}

	/** The lid comes off: the treasure spills out and the chest becomes the open chest. */
	void tickOpening(ServerLevel level) {
		if (--this.countdown != 0) {
			this.setChanged();
			return;
		}
		BlockPos pos = this.worldPosition;
		for (int slot = 0; slot < TREASURE_SLOTS; slot++) {
			ItemStack treasure = this.getItem(slot);
			if (treasure.isEmpty()) continue;
			ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, treasure.copy());
			drop.setPickUpDelay(10);
			level.addFreshEntity(drop);
		}
		Direction facing = this.getBlockState().getValue(TreasureChestBlock.FACING);
		// The rest of the store is not carried over, as in the original.
		level.setBlock(pos, ShipFittingsModule.TREASURE_CHEST_OPEN.defaultBlockState().setValue(TreasureChestOpenBlock.FACING, facing), Block.UPDATE_ALL);
		level.playSound(null, pos, ShipFittingsModule.CHEST_UNLID_SOUND, SoundSource.BLOCKS, 1.0F, 1.0F);
		Vec3 centre = Vec3.atCenterOf(pos);
		level.sendParticles(ParticleTypes.CLOUD, true, false, centre.x, centre.y, centre.z, 10, 0.5, 0.0, 0.5, 0.02);
		level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, pos, Block.getId(ShipFittingsModule.TREASURE_CHEST.defaultBlockState()));
		if (level.getBlockEntity(pos) instanceof TreasureChestBlockEntity open) {
			open.startCountdown(CRUMBLE_TICKS);
		}
	}

	/** The open chest crumbles away. */
	void tickOpen(ServerLevel level) {
		if (--this.countdown != 0) {
			this.setChanged();
			return;
		}
		BlockPos pos = this.worldPosition;
		level.playSound(null, pos, ShipFittingsModule.CHEST_DESTROY_SOUND, SoundSource.BLOCKS, 1.0F, 1.0F);
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, pos, Block.getId(ShipFittingsModule.TREASURE_CHEST.defaultBlockState()));
	}

	/** Breaking or swapping the chest never spills its store. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		if (!this.tryLoadLootTable(input)) {
			ContainerHelper.loadAllItems(input, this.items);
		}
		this.countdown = input.getIntOr("countdown", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!this.trySaveLootTable(output)) {
			ContainerHelper.saveAllItems(output, this.items);
		}
		output.putInt("countdown", this.countdown);
	}

	@Override
	public int getContainerSize() {
		return SIZE;
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("block.more_critters.treasure_chest");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new TreasureChestMenu(containerId, inventory, this);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return ALL_SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return true;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return false;
	}
}
