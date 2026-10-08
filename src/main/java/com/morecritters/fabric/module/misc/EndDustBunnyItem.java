package com.morecritters.fabric.module.misc;

import com.morecritters.fabric.ids.ShimmerwingIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A ball of End dust, edible at any time; eating it makes you sneeze and kicks you backwards. */
public class EndDustBunnyItem extends Item {
	private static final double SNEEZE_RECOIL = -0.3;

	public EndDustBunnyItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		ItemStack result = super.finishUsingItem(stack, level, entity);
		if (!level.isClientSide()) {
			BuiltInRegistries.SOUND_EVENT.get(ShimmerwingIds.Sounds.ENTITY_SNEEZE).ifPresent(sneeze ->
				level.playSound((Player) null, entity.blockPosition(), sneeze.value(), SoundSource.PLAYERS, 1.0F, 1.0F));
		}
		entity.setDeltaMovement(entity.getLookAngle().scale(SNEEZE_RECOIL));
		return result;
	}
}
