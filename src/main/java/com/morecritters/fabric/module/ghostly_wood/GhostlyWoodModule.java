package com.morecritters.fabric.module.ghostly_wood;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.ids.GhostlyWoodIds;
import com.morecritters.fabric.ids.GhostlyWoodIds.Blocks;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Map;

/**
 * The ghost ship's building set: ghostly wood (logs, planks and everything made of them) with its
 * wet, kelpy and petrified variants, ectometal, fish bones, dried kelp carpet, barnacle clusters,
 * the giant chain, the tattered flag and rum spilled on deck.
 */
public final class GhostlyWoodModule implements Module {
	public static Item ECTOMETAL, FISH_BONE;

	/** Ghostly logs and wood turn into their stripped forms under an axe. */
	private static Map<Block, Block> strippedForms = Map.of();

	private static final float SLIPPERY_FRICTION = 0.9F;
	private static final int BUTTON_PRESS_TICKS = 30;
	private static final double GIANT_CHAIN_THICKNESS = 12.0;
	private static final double FISH_BONE_POLE_THICKNESS = 4.0;
	/** A nail head on a short shank driven into the wall behind it. */
	private static final VoxelShape NAIL_ON_NORTH_WALL =
		Shapes.or(Block.box(6.0, 6.0, 9.0, 10.0, 10.0, 16.0), Block.box(2.0, 2.0, 5.0, 14.0, 14.0, 9.0));

	@Override
	public void register() {
		ECTOMETAL = Registration.item(GhostlyWoodIds.Items.ECTOMETAL);
		FISH_BONE = Registration.item(GhostlyWoodIds.Items.FISH_BONE);

		registerGhostlyWood();
		registerShipParts();
		UseBlockCallback.EVENT.register(GhostlyWoodModule::stripLog);
	}

	private static void registerGhostlyWood() {
		Block log = Registration.blockWithItem(Blocks.GHOSTLY_LOG, RotatedPillarBlock::new, ghostlyWood());
		Block wood = Registration.blockWithItem(Blocks.GHOSTLY_WOOD, RotatedPillarBlock::new, ghostlyWood());
		Block strippedLog = Registration.blockWithItem(Blocks.STRIPPED_GHOSTLY_LOG, RotatedPillarBlock::new, ghostlyWood());
		Block strippedWood = Registration.blockWithItem(Blocks.STRIPPED_GHOSTLY_WOOD, RotatedPillarBlock::new, ghostlyWood());
		strippedForms = Map.of(log, strippedLog, wood, strippedWood);

		Block planks = Registration.blockWithItem(Blocks.GHOSTLY_PLANKS, GhostlyPlanksBlock::new, ghostlyWood());
		Registration.blockWithItem(Blocks.GHOSTLY_MOSAIC_PLANKS, GhostlyPlanksBlock::new, ghostlyWood());
		Registration.blockWithItem(Blocks.KELPY_GHOSTLY_PLANKS, GhostlyPlanksBlock::new, ghostlyWood());
		Registration.blockWithItem(Blocks.WET_GHOSTLY_PLANKS, WetGhostlyPlanksBlock::new, ghostlyWood().friction(SLIPPERY_FRICTION));
		Registration.blockWithItem(Blocks.WET_GHOSTLY_MOSAIC_PLANKS, WetGhostlyPlanksBlock::new, ghostlyWood().friction(SLIPPERY_FRICTION));
		// The kelp gives grip: wet kelpy planks are not slippery.
		Registration.blockWithItem(Blocks.WET_KELPY_GHOSTLY_PLANKS, WetGhostlyPlanksBlock::new, ghostlyWood());

		Registration.blockWithItem(Blocks.GHOSTLY_STAIRS, properties -> new StairBlock(planks.defaultBlockState(), properties), ghostlyWood());
		Registration.blockWithItem(Blocks.GHOSTLY_SLAB, SlabBlock::new, ghostlyWood());
		Registration.blockWithItem(Blocks.GHOSTLY_FENCE, FenceBlock::new, ghostlyWood().forceSolidOn());
		Registration.blockWithItem(Blocks.GHOSTLY_FENCE_GATE, properties -> new FenceGateBlock(WoodType.OAK, properties), ghostlyWood().forceSolidOn());
		Registration.blockWithItem(Blocks.GHOSTLY_DOOR, properties -> new DoorBlock(BlockSetType.OAK, properties), ghostlyWood().noOcclusion());
		Registration.blockWithItem(Blocks.GHOSTLY_TRAPDOOR, properties -> new TrapDoorBlock(BlockSetType.OAK, properties), ghostlyWood().noOcclusion());
		Registration.blockWithItem(Blocks.GHOSTLY_PRESSURE_PLATE,
			properties -> new PressurePlateBlock(BlockSetType.OAK, properties), ghostlyWood().forceSolidOn());
		Registration.blockWithItem(Blocks.GHOSTLY_BUTTON,
			properties -> new ButtonBlock(BlockSetType.OAK, BUTTON_PRESS_TICKS, properties), ghostlyWood());

		Block petrifiedPlanks = Registration.blockWithItem(Blocks.PETRIFIED_GHOSTLY_PLANKS, GhostlyPlanksBlock::new, petrifiedWood());
		Registration.blockWithItem(Blocks.PETRIFIED_GHOSTLY_MOSAIC_PLANKS, GhostlyPlanksBlock::new, petrifiedWood());
		// Kelpy petrified planks sound like wood, as in the original.
		Registration.blockWithItem(Blocks.KELPY_PETRIFIED_GHOSTLY_PLANKS, GhostlyPlanksBlock::new, ghostlyWood());
		Registration.blockWithItem(Blocks.PETRIFIED_GHOSTLY_STAIRS,
			properties -> new GhostlyPlanksBlock.Stairs(petrifiedPlanks.defaultBlockState(), properties), petrifiedWood());
		Registration.blockWithItem(Blocks.PETRIFIED_GHOSTLY_SLAB, GhostlyPlanksBlock.Slab::new, petrifiedWood());
	}

	private static void registerShipParts() {
		Registration.blockWithItem(Blocks.ECTOMETAL_BLOCK, Block::new, ectometal());
		Registration.blockWithItem(Blocks.ECTOMETAL_RAILING, IronBarsBlock::new, ectometal().noOcclusion());
		Registration.blockWithItem(Blocks.ECTOMETAL_NAIL, properties -> new FaceMountedBlock(NAIL_ON_NORTH_WALL, properties),
			ectometal().noOcclusion());

		Registration.blockWithItem(Blocks.FISH_BONE_BLOCK, RotatedPillarBlock::new, fishBone());
		Registration.blockWithItem(Blocks.FISH_BONE_POLE, properties -> new PoleBlock(FISH_BONE_POLE_THICKNESS, properties),
			fishBone().noOcclusion());
		Registration.blockWithItem(Blocks.GIANT_CHAIN, properties -> new PoleBlock(GIANT_CHAIN_THICKNESS, properties),
			BlockBehaviour.Properties.of().sound(SoundType.CHAIN).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noCollision().noOcclusion());

		Registration.blockWithItem(Blocks.DRIED_KELP_CARPET, DriedKelpCarpetBlock::new,
			BlockBehaviour.Properties.of().sound(SoundType.MOSS_CARPET).strength(0.1F).noOcclusion());
		Registration.blockWithItem(Blocks.BARNACLE_CLUSTER, BarnacleClusterBlock::new,
			BlockBehaviour.Properties.of().sound(SoundType.STEM).strength(0.2F).noOcclusion());
		Registration.blockWithItem(Blocks.TATTERED_FLAG, TatteredFlagBlock::new,
			BlockBehaviour.Properties.of().sound(SoundType.WOOL).strength(0.2F).noOcclusion());

		Block rum = Registration.block(Blocks.RUM_BOTTLE, RumBottleBlock::new,
			BlockBehaviour.Properties.of().sound(SoundType.SLIME_BLOCK).instabreak().noCollision().noOcclusion());
		Registration.item(Blocks.RUM_BOTTLE, properties -> new BlockItem(rum, properties),
			blockDescription(new Item.Properties().useBlockDescriptionPrefix(), Blocks.RUM_BOTTLE));
	}

	/** Every piece of ghostly wood: strength 2, wooden sounds, catches fire from lava. */
	private static BlockBehaviour.Properties ghostlyWood() {
		return BlockBehaviour.Properties.of().ignitedByLava().instrument(NoteBlockInstrument.BASS).sound(SoundType.WOOD).strength(2.0F, 3.0F);
	}

	/** Petrified ghostly wood is as tough as the plain kind but sounds like stone. */
	private static BlockBehaviour.Properties petrifiedWood() {
		return ghostlyWood().sound(SoundType.STONE);
	}

	/** Ectometal: as hard and blast-proof as netherite, mined with a pickaxe. */
	private static BlockBehaviour.Properties ectometal() {
		return BlockBehaviour.Properties.of().sound(SoundType.NETHERITE_BLOCK).strength(10.0F, 1200.0F).requiresCorrectToolForDrops();
	}

	private static BlockBehaviour.Properties fishBone() {
		return BlockBehaviour.Properties.of().sound(SoundType.BONE_BLOCK).strength(2.0F);
	}

	/** The block's one description line ({@code block.more_critters.<name>.description_0}) as the item's lore. */
	private static Item.Properties blockDescription(Item.Properties properties, Identifier id) {
		Component line = Component.translatable("block." + id.getNamespace() + "." + id.getPath() + ".description_0")
			.withStyle(style -> style.withItalic(false).withColor(ChatFormatting.WHITE));
		return properties.component(DataComponents.LORE, new ItemLore(List.of(line)));
	}

	/** Right-clicking a ghostly log or wood with an axe in the main hand strips it, wearing the axe. */
	private static InteractionResult stripLog(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		ItemStack held = player.getMainHandItem();
		if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || !held.is(ItemTags.AXES)) {
			return InteractionResult.PASS;
		}
		BlockPos pos = hit.getBlockPos();
		BlockState state = level.getBlockState(pos);
		Block stripped = strippedForms.get(state.getBlock());
		if (stripped == null) {
			return InteractionResult.PASS;
		}
		level.playSound(player, pos, SoundEvents.AXE_STRIP.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
		if (!level.isClientSide()) {
			level.setBlock(pos, stripped.withPropertiesOf(state), Block.UPDATE_ALL);
			held.hurtAndBreak(1, player, hand);
		}
		return InteractionResult.SUCCESS;
	}
}
