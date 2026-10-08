package com.morecritters.fabric.module.ship_fittings;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The cannon's one-slot magazine, and the firing: on a rising redstone edge it fires the loaded ball and remembers
 * that it has fired until the signal goes off again. Hoppers may load and unload it from any side.
 */
public class CannonBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
	static final int MAGAZINE_SIZE = 16;
	private static final int[] SLOTS = {0};
	private static final int SMOKE_PUFFS = 15;
	/** Side of the box around the cannon in which the nearest player earns "BOOM!". */
	private static final double WITNESS_RANGE = 10.0;

	private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
	/** Set when the cannon fires, cleared when its redstone signal goes off. */
	private boolean fired;

	public CannonBlockEntity(BlockPos pos, BlockState state) {
		super(ShipFittingsModule.CANNON_BLOCK_ENTITY, pos, state);
	}

	void onRedstone(ServerLevel level, boolean powered) {
		if (!powered) {
			this.fired = false;
			this.setChanged();
			return;
		}
		if (this.fired) return;

		Infusion ammo = Infusion.ofAmmo(this.getItem(0).getItem());
		if (ammo != null) {
			this.fire(level, ammo);
		}
		// Anyone standing by when the cannon is triggered counts as a witness, loaded or not.
		Vec3 centre = Vec3.atLowerCornerOf(this.worldPosition);
		level.getEntitiesOfClass(Player.class, AABB.ofSize(centre, WITNESS_RANGE, WITNESS_RANGE, WITNESS_RANGE)).stream()
			.min(Comparator.comparingDouble(player -> player.distanceToSqr(centre)))
			.ifPresent(player -> Advancements.award(player, MoreCritters.id("shoot_cannon")));
	}

	private void fire(ServerLevel level, Infusion ammo) {
		BlockPos pos = this.worldPosition;
		level.playSound(null, pos, ShipFittingsModule.CANNON_FIRE_SOUND, SoundSource.BLOCKS, 1.0F, 1.0F);
		this.removeItem(0, 1);
		this.fired = true;

		Direction facing = this.getBlockState().getValue(CannonBlock.FACING);
		CannonBallProjectile.fireFrom(level, pos, facing, ammo);
		Vec3 muzzle = Vec3.atCenterOf(pos.relative(facing));
		level.sendParticles(ParticleTypes.LARGE_SMOKE, true, false, muzzle.x, muzzle.y, muzzle.z, SMOKE_PUFFS, 0.2, 0.2, 0.2, 0.01);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		if (!this.tryLoadLootTable(input)) {
			ContainerHelper.loadAllItems(input, this.items);
		}
		this.fired = input.getBooleanOr("fired", false);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!this.trySaveLootTable(output)) {
			ContainerHelper.saveAllItems(output, this.items);
		}
		output.putBoolean("fired", this.fired);
	}

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	public int getMaxStackSize() {
		return MAGAZINE_SIZE;
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("block.more_critters.cannon");
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
		return new CannonMenu(containerId, inventory, this);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return true;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return true;
	}
}
