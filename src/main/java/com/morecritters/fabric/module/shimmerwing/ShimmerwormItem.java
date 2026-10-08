package com.morecritters.fabric.module.shimmerwing;

import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** A picked-up shimmerworm: set it down against any block face, or (you monster) eat it. */
public class ShimmerwormItem extends Item {
	public ShimmerwormItem(Properties properties) {
		super(properties);
	}

	/** Lets the worm go in the block next to the clicked face. */
	@Override
	public InteractionResult useOn(UseOnContext context) {
		context.getItemInHand().shrink(1);
		if (context.getLevel() instanceof ServerLevel level) {
			ShimmerwingModule.SHIMMERWORM.spawn(level, context.getClickedPos().relative(context.getClickedFace()), EntitySpawnReason.MOB_SUMMONED);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
		ItemStack result = super.finishUsingItem(stack, level, eater);
		Advancements.award(eater, MoreCritters.id("eat_shimmerworm"));
		return result;
	}
}
