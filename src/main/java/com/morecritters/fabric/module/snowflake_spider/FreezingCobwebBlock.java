package com.morecritters.fabric.module.snowflake_spider;

import com.morecritters.fabric.core.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A frozen cobweb the snowflake spider's web sacks leave behind. Anything caught in it is slowed
 * like in a cobweb (spiders slip through) and frosts over a little each tick, up to the point
 * where powder snow starts to hurt.
 */
public class FreezingCobwebBlock extends Block {
	/** Kept from the original for its block-state file; only state 0 is ever used and the others are invisible. */
	public static final IntegerProperty BLOCKSTATE = IntegerProperty.create("blockstate", 0, 2);

	private static final Vec3 STUCK_SPEED = new Vec3(0.25, 0.05, 0.25);
	private static final int FREEZE_CAP = 200;
	private static final int FREEZE_STEP_MIN = 2, FREEZE_STEP_MAX = 5;

	public FreezingCobwebBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(BLOCKSTATE, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BLOCKSTATE);
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return true;
	}

	@Override
	protected int getLightDampening(BlockState state) {
		return 0;
	}

	@Override
	protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(BLOCKSTATE) == 0 ? Shapes.block() : Shapes.empty();
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
		if (entity instanceof SnowflakeSpiderEntity) {
			return;
		}
		// Spiders (and cave spiders, which are spiders) are not slowed; the original passed air as the trapping block.
		if (!(entity instanceof Spider) && Config.flag("freezing_cobweb_freeze", true)) {
			entity.makeStuckInBlock(Blocks.AIR.defaultBlockState(), STUCK_SPEED);
		}
		if (entity.getTicksFrozen() <= FREEZE_CAP) {
			entity.setTicksFrozen(entity.getTicksFrozen() + Mth.nextInt(level.getRandom(), FREEZE_STEP_MIN, FREEZE_STEP_MAX));
		}
	}
}
