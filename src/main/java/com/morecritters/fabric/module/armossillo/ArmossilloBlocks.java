package com.morecritters.fabric.module.armossillo;

import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.ids.ArmossilloIds;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The armossillo's blocks: the sturdy shell block, the moss clump a baby hatches from, the glowing
 * ooze blocks, the glow the ooze leaves behind, and the hanging goobulb. Every one has a block item.
 */
public final class ArmossilloBlocks {
	public static Block STURDY_SHELL_BLOCK, CUT_GLOWING_OOZE_BLOCK;
	public static ColoredFallingBlock GLOWING_OOZE_BLOCK;
	public static MossClumpBlock MOSS_CLUMP;
	public static GlowBlock GLOW;
	public static GoobulbBlock GOOBULB;

	/** Dust under a glowing ooze block about to fall: 1.21.1's default falling-block colour (black). */
	private static final ColorRGBA OOZE_DUST = new ColorRGBA(0xFF000000);
	private static final int GLOWING_OOZE_LIGHT = 5;
	private static final int CUT_GLOWING_OOZE_LIGHT = 3;
	private static final float SHELL_HARDNESS = 10.0F, SHELL_RESISTANCE = 1200.0F;
	private static final float OOZE_HARDNESS = 0.5F;
	private static final float UNBREAKABLE = -1.0F, GLOW_RESISTANCE = 3_600_000.0F;

	static void register() {
		STURDY_SHELL_BLOCK = Registration.blockWithItem(ArmossilloIds.Blocks.STURDY_SHELL_BLOCK, Block::new,
			BlockBehaviour.Properties.of().sound(SoundType.STONE).strength(SHELL_HARDNESS, SHELL_RESISTANCE).requiresCorrectToolForDrops());
		MOSS_CLUMP = Registration.blockWithItem(ArmossilloIds.Blocks.MOSS_CLUMP, MossClumpBlock::new,
			BlockBehaviour.Properties.of().sound(SoundType.MOSS).instabreak().noOcclusion().isRedstoneConductor((state, level, pos) -> false));
		GLOWING_OOZE_BLOCK = Registration.blockWithItem(ArmossilloIds.Blocks.GLOWING_OOZE_BLOCK,
			properties -> new ColoredFallingBlock(OOZE_DUST, properties),
			BlockBehaviour.Properties.of().sound(SoundType.SLIME_BLOCK).strength(OOZE_HARDNESS)
				.lightLevel(state -> GLOWING_OOZE_LIGHT).emissiveRendering(state -> true));
		CUT_GLOWING_OOZE_BLOCK = Registration.blockWithItem(ArmossilloIds.Blocks.CUT_GLOWING_OOZE_BLOCK, Block::new,
			BlockBehaviour.Properties.of().sound(SoundType.STONE).strength(OOZE_HARDNESS)
				.lightLevel(state -> CUT_GLOWING_OOZE_LIGHT).emissiveRendering(state -> true));
		GLOW = Registration.blockWithItem(ArmossilloIds.Blocks.GLOW, GlowBlock::new,
			BlockBehaviour.Properties.of().sound(SoundType.EMPTY).strength(UNBREAKABLE, GLOW_RESISTANCE).noCollision().noOcclusion()
				.emissiveRendering(state -> true).isRedstoneConductor((state, level, pos) -> false));
		GOOBULB = Registration.blockWithItem(ArmossilloIds.Blocks.GOOBULB, GoobulbBlock::new,
			BlockBehaviour.Properties.of().sound(SoundType.SLIME_BLOCK).instabreak().lightLevel(state -> GoobulbBlock.LIGHT)
				.noCollision().noOcclusion().emissiveRendering(state -> true).isRedstoneConductor((state, level, pos) -> false));
	}

	private ArmossilloBlocks() {}
}
