package com.morecritters.fabric.module.nervoid;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.ids.NervoidIds;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A nervoid brain: a falling block that lures the undead (from 30, 15 or 5 blocks as it decays);
 * an undead mob that reaches it eats it. Left alone it decomposes and then rots, and a rotten brain
 * stinks. Ice in hand freezes it (stopping the decay), a torch thaws it.
 */
public final class NervoidBrainBlock extends FallingBlock {
	public enum Stage {
		FRESH(30.0, NervoidIds.Blocks.NERVOID_BRAIN, NervoidIds.Blocks.ICED_NERVOID_BRAIN),
		DECOMPOSING(15.0, NervoidIds.Blocks.DECOMPOSING_NERVOID_BRAIN, NervoidIds.Blocks.ICED_DECOMPOSING_NERVOID_BRAIN),
		ROTTEN(5.0, NervoidIds.Blocks.ROTTEN_NERVOID_BRAIN, NervoidIds.Blocks.ICED_ROTTEN_NERVOID_BRAIN);

		final double lureRange;
		final Identifier id, icedId;

		Stage(double lureRange, Identifier id, Identifier icedId) {
			this.lureRange = lureRange;
			this.id = id;
			this.icedId = icedId;
		}
	}

	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	private static final Map<Stage, NervoidBrainBlock> PLAIN = new EnumMap<>(Stage.class);
	private static final Map<Stage, NervoidBrainBlock> ICED = new EnumMap<>(Stage.class);
	private static final int CHECK_INTERVAL = 5;
	private static final int DECAY_CHANCE = 200;
	private static final int STINK_CHANCE = 5;
	private static final VoxelShape NORTH_SOUTH = Block.box(2.5, 0.0, 2.0, 13.5, 7.0, 14.0);
	private static final VoxelShape EAST_WEST = Block.box(2.0, 0.0, 2.5, 14.0, 7.0, 13.5);

	private final Stage stage;
	private final boolean iced;

	private NervoidBrainBlock(Stage stage, boolean iced, Properties properties) {
		super(properties);
		this.stage = stage;
		this.iced = iced;
		registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	static void registerAll(SoundType sound) {
		for (Stage stage : Stage.values()) {
			PLAIN.put(stage, Registration.blockWithItem(stage.id, p -> new NervoidBrainBlock(stage, false, p), properties(sound)));
			ICED.put(stage, Registration.blockWithItem(stage.icedId, p -> new NervoidBrainBlock(stage, true, p), properties(sound)));
		}
	}

	private static BlockBehaviour.Properties properties(SoundType sound) {
		return BlockBehaviour.Properties.of().sound(sound).strength(0.2F).friction(0.8F).noOcclusion()
			.isRedstoneConductor((state, level, pos) -> false);
	}

	@Override
	public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
		return -16777216;
	}

	// --- shape and facing ---------------------------------------------------------------

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Direction facing = state.getValue(FACING);
		return facing == Direction.EAST || facing == Direction.WEST ? EAST_WEST : NORTH_SOUTH;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	// --- luring, decay and stink --------------------------------------------------------

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		level.scheduleTick(pos, this, CHECK_INTERVAL);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		super.tick(state, level, pos, random);
		if (!level.getBlockState(pos).is(this)) {
			return; // it fell
		}
		lureUndead(level, pos);
		if (!level.getBlockState(pos).is(this)) {
			return; // eaten
		}
		if (!this.iced) {
			decay(level, pos, state);
		}
		level.scheduleTick(pos, this, CHECK_INTERVAL);
	}

	/** Undead mobs in range walk to the brain; one standing on it eats it, dropping its loot. */
	private void lureUndead(ServerLevel level, BlockPos pos) {
		double x = pos.getX(), y = pos.getY(), z = pos.getZ();
		AABB lure = new AABB(x, y, z, x, y, z).inflate(this.stage.lureRange);
		for (Mob undead : level.getEntitiesOfClass(Mob.class, lure, mob -> mob.is(EntityTypeTags.UNDEAD))) {
			undead.getNavigation().moveTo(x, y, z, 1.0);
		}
		AABB reach = new AABB(x, y, z, x, y, z).inflate(0.5);
		if (!level.getEntitiesOfClass(LivingEntity.class, reach, entity -> entity.is(EntityTypeTags.UNDEAD)).isEmpty()) {
			level.destroyBlock(pos, true);
		}
	}

	/** One time in 200 a fresh brain decomposes and a decomposing one rots; a rotten brain gives off stink. */
	private void decay(ServerLevel level, BlockPos pos, BlockState state) {
		RandomSource random = level.getRandom();
		boolean decays = Mth.nextInt(random, 1, DECAY_CHANCE) == 1;
		if (decays && this.stage != Stage.ROTTEN) {
			Stage next = Stage.values()[this.stage.ordinal() + 1];
			level.setBlock(pos, PLAIN.get(next).withFacingOf(state), Block.UPDATE_ALL);
		} else if (this.stage == Stage.ROTTEN && Mth.nextInt(random, 1, STINK_CHANCE) == 1) {
			level.sendParticles(NervoidModule.STINK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.0);
		}
	}

	private BlockState withFacingOf(BlockState other) {
		return defaultBlockState().setValue(FACING, other.getValue(FACING));
	}

	// --- icing and thawing --------------------------------------------------------------

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!this.iced && stack.is(Items.ICE)) {
			swap(level, pos, state, player, stack, ICED.get(this.stage), NervoidModule.ICE_ON, NervoidModule.BRAIN_BREAKING, "ice_brain");
			return InteractionResult.SUCCESS;
		}
		if (this.iced && stack.is(Items.TORCH)) {
			swap(level, pos, state, player, stack, PLAIN.get(this.stage), NervoidModule.ICE_OFF, SoundEvents.CANDLE_EXTINGUISH, "ice_off_brain");
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	/** Uses up the held ice or torch and turns this brain into its iced or thawed twin. */
	private static void swap(Level level, BlockPos pos, BlockState state, Player player, ItemStack held, NervoidBrainBlock into,
			SimpleParticleType particle, SoundEvent sound, String advancement) {
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		Advancements.award(player, MoreCritters.id(advancement));
		if (!player.hasInfiniteMaterials()) {
			held.shrink(1);
		}
		server.sendParticles(particle, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.01);
		level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
		level.setBlock(pos, into.withFacingOf(state), Block.UPDATE_ALL);
	}
}
