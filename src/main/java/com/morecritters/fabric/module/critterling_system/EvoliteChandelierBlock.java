package com.morecritters.fabric.module.critterling_system;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A hanging evolite chandelier. Every tick it makes the critterlings within 12.5 blocks
 * evolightened (invulnerable, {@link EvolightenedEffect}) for ten seconds and lets a little
 * evolite light drift down from it.
 */
public class EvoliteChandelierBlock extends WaterloggableBlock {
	private static final VoxelShape SHAPE = box(0, 9, 0, 16, 12, 16);
	private static final TagKey<EntityType<?>> CRITTERLINGS = TagKey.create(Registries.ENTITY_TYPE, Identifier.withDefaultNamespace("critterling"));
	private static final double REACH = 12.5;
	private static final int EFFECT_TICKS = 200;

	public EvoliteChandelierBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false));
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		level.scheduleTick(pos, this, 1);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		List<LivingEntity> critterlings = level.getEntitiesOfClass(LivingEntity.class, new AABB(Vec3.atLowerCornerOf(pos), Vec3.atLowerCornerOf(pos)).inflate(REACH),
			entity -> entity.is(CRITTERLINGS) && !entity.hasEffect(CritterlingSystemModule.EVOLIGHTENED));
		for (LivingEntity critterling : critterlings) {
			critterling.addEffect(new MobEffectInstance(CritterlingSystemModule.EVOLIGHTENED, EFFECT_TICKS, 0, false, false));
		}
		level.sendParticles(CritterlingSystemModule.EVOLIGHTENED_PARTICLE, true, false,
			pos.getX() + 0.5, pos.getY() - 1.0, pos.getZ() + 0.5, 3, 0.5, -0.6, 0.5, 0.01);
		level.scheduleTick(pos, this, 1);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}
}
