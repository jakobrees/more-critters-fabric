package com.morecritters.fabric.module.creeblossom;

import com.morecritters.fabric.ids.ShockCubeIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A blossombush, plain or electric, open or closed. It opens at night and closes by day; while open, with a
 * monster within five blocks, one tick in fifty it grows a recruited creeblossom that hunts the monster.
 * Grows only on dirt, and never beside a potted bush of its own kind.
 */
public class BlossombushBlock extends Block {
	private static final VoxelShape SHAPE = box(3.0, 0.0, 3.0, 13.0, 7.0, 13.0);
	private static final int GROW_CHANCE = 50;
	private static final double MONSTER_SEARCH_SIZE = 10.0;

	private final boolean electric;
	private final boolean open;

	public BlossombushBlock(Properties properties, boolean electric, boolean open) {
		super(properties);
		this.electric = electric;
		this.open = open;
	}

	public boolean isElectric() {
		return electric;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return true;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		if (!level.getBlockState(pos.below()).is(BlockTags.DIRT)) {
			return false;
		}
		Block pot = electric ? CreeblossomModule.potElectricBlossombush : CreeblossomModule.potBlossombush;
		for (Direction side : Direction.Plane.HORIZONTAL) {
			if (level.getBlockState(pos.relative(side)).is(pot)) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
	                                 BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
		return state.canSurvive(level, pos) ? state : Blocks.AIR.defaultBlockState();
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
		super.onPlace(state, level, pos, oldState, moving);
		level.scheduleTick(pos, this, 1);
	}

	/** Checked every tick, like the original. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		boolean day = level.isBrightOutside();
		if (open && day) {
			toggle(level, pos, electric ? CreeblossomModule.closedElectricBlossombush : CreeblossomModule.closedBlossombush);
		} else if (!open && !day) {
			toggle(level, pos, electric ? CreeblossomModule.electricBlossombush : CreeblossomModule.blossombush);
		}
		if (open) {
			growCreeblossomNearMonsters(level, pos, random);
		}
		level.scheduleTick(pos, level.getBlockState(pos).getBlock(), 1);
	}

	private void toggle(ServerLevel level, BlockPos pos, Block other) {
		puff(level, pos);
		level.setBlock(pos, other.defaultBlockState(), Block.UPDATE_ALL);
		level.playSound(null, pos, CreeblossomModule.OPEN_CLOSE_SOUND, SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	private void growCreeblossomNearMonsters(ServerLevel level, BlockPos pos, RandomSource random) {
		AABB area = AABB.ofSize(Vec3.atLowerCornerOf(pos), MONSTER_SEARCH_SIZE, MONSTER_SEARCH_SIZE, MONSTER_SEARCH_SIZE);
		if (random.nextInt(GROW_CHANCE) != 0 || level.getEntitiesOfClass(Monster.class, area).isEmpty()) {
			return;
		}
		SoundEvent sound = electric ? CreeblossomModule.ELECTRIC_SPAWN_SOUND : CreeblossomModule.SPAWN_SOUND;
		level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
		puff(level, pos);
		CreeblossomModule.CREEBLOSSOM.spawn(level, pos, EntitySpawnReason.MOB_SUMMONED);
	}

	/** Leaves for a plain bush, sparks for an electric one. */
	private void puff(ServerLevel level, BlockPos pos) {
		int count = 5 + level.getRandom().nextInt(3);
		Vec3 centre = Vec3.atCenterOf(pos);
		if (!electric) {
			level.sendParticles(CreeblossomModule.RECRUITED_LEAF, centre.x, centre.y, centre.z, count, 0.3, 0.3, 0.3, 0.02);
		} else if (BuiltInRegistries.PARTICLE_TYPE.getValue(ShockCubeIds.Particles.ZAP) instanceof ParticleOptions zap) {
			level.sendParticles(zap, centre.x, centre.y, centre.z, count, 0.3, 0.3, 0.3, 0.0);
		}
	}
}
