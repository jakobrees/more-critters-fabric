package com.morecritters.fabric.module.ghostly_wood;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * A small fitting stuck to the floor, a wall or the ceiling of whatever was clicked, like a button.
 * On a wall it faces away from the wall; on a floor or ceiling it faces the way the player looks.
 * The ectometal nail is one as it is; the barnacle cluster builds on it.
 */
public class FaceMountedBlock extends Block {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<AttachFace> FACE = FaceAttachedHorizontalDirectionalBlock.FACE;

	private final Map<AttachFace, Map<Direction, VoxelShape>> shapes;

	/** @param northWallShape the shape on a wall, facing north (so touching the block's south side) */
	public FaceMountedBlock(VoxelShape northWallShape, Properties properties) {
		super(properties);
		this.shapes = Shapes.rotateAttachFace(northWallShape);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FACE, AttachFace.WALL));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, FACE);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction clicked = context.getClickedFace();
		BlockState state = defaultBlockState();
		if (clicked.getAxis() == Direction.Axis.Y) {
			return state.setValue(FACE, clicked == Direction.DOWN ? AttachFace.CEILING : AttachFace.FLOOR)
				.setValue(FACING, context.getHorizontalDirection());
		}
		return state.setValue(FACE, AttachFace.WALL).setValue(FACING, clicked);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapes.get(state.getValue(FACE)).get(state.getValue(FACING));
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
}
