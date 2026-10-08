package com.morecritters.fabric.module.custodian;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A dormant custodian found in Ancient Cities. It does nothing and nothing living can hurt it.
 * Fed three echo shards it spins up, opens, and becomes a working custodian.
 */
public class AncientCustodianEntity extends PathfinderMob implements GeoEntity {
	private static final int SHARDS_TO_AWAKEN = 3;
	private static final int AWAKEN_TICKS = 35;

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private int shardsFed;
	private boolean awakening;

	public AncientCustodianEntity(EntityType<? extends AncientCustodianEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		setPersistenceRequired();
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getMainHandItem();
		if (!held.is(Items.ECHO_SHARD)) {
			return super.mobInteract(player, hand);
		}
		if (level() instanceof ServerLevel level) {
			feedShard(level, player, held);
		}
		return InteractionResult.SUCCESS;
	}

	/** Each shard spins it a little further; the third wakes it. Extra shards are still eaten. */
	private void feedShard(ServerLevel level, Player player, ItemStack shard) {
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		Sounds.playAt(this, CustodianModule.SPIN_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
		if (!player.hasInfiniteMaterials()) {
			shard.shrink(1);
		}
		if (this.shardsFed < SHARDS_TO_AWAKEN) {
			this.shardsFed++;
			switch (this.shardsFed) {
				case 1 -> triggerAnim(Animations.ACTIONS, "feed1");
				case 2 -> triggerAnim(Animations.ACTIONS, "feed2");
				default -> {
					Advancements.award(player, MoreCritters.id("awaken_custodian"));
					awaken(level);
				}
			}
		}
	}

	/** Opens up, then is replaced by a working custodian that keeps the ancient look. */
	private void awaken(ServerLevel level) {
		if (this.awakening) {
			return;
		}
		this.awakening = true;
		Sounds.playAt(this, CustodianModule.OPEN_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
		triggerAnim(Animations.ACTIONS, "openup");
		ServerScheduler.runLater(AWAKEN_TICKS, () -> {
			discard();
			CustodianEntity custodian = CustodianModule.CUSTODIAN.spawn(level, blockPosition(), EntitySpawnReason.MOB_SUMMONED);
			if (custodian != null) {
				custodian.setYRot(getYRot());
				custodian.setYBodyRot(getYRot());
				custodian.setYHeadRot(getYRot());
				custodian.setXRot(getXRot());
				custodian.setAncientLook();
			}
		});
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() != null || CustodianEntity.isImmuneTo(source)) {
			return false;
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	public boolean canBeCollidedWith(@Nullable Entity other) {
		return isAlive();
	}

	@Override
	public boolean canCollideWith(Entity entity) {
		return true;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).scale(1.2F);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return CustodianModule.HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return CustodianModule.DEATH_SOUND;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("ShardsFed", this.shardsFed);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.shardsFed = input.getIntOr("ShardsFed", 0);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		RawAnimation idle = RawAnimation.begin().thenLoop("idle_close");
		controllers.add(new AnimationController<AncientCustodianEntity>(Animations.MOVEMENT, 2, test -> test.setAndContinue(idle)));
		controllers.add(Animations.actions(this, "feed1", "feed2", "openup"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}
}
