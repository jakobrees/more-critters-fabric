package com.morecritters.fabric.module.fossils;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.ids.FossilsIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A mounted skeleton for show. It stands still, cannot be pushed through (solid like a boat),
 * shrugs off players, projectiles, potions, falls, fire and most environmental damage, and
 * vanishes without a death animation when killed. Right-clicking cycles it through ten poses;
 * sneaking with an empty hand packs it back into its item.
 */
public class AncientSkeletonExhibitEntity extends PathfinderMob implements GeoEntity {
	private static final int POSES = 10;
	private static final EntityDataAccessor<Integer> POSE = SynchedEntityData.defineId(AncientSkeletonExhibitEntity.class, EntityDataSerializers.INT);
	private static final RawAnimation[] POSE_ANIMATIONS = new RawAnimation[POSES];

	static {
		for (int i = 0; i < POSES; i++) POSE_ANIMATIONS[i] = RawAnimation.begin().thenLoop("pose" + (i + 1));
	}

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);

	public AncientSkeletonExhibitEntity(EntityType<? extends AncientSkeletonExhibitEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 50.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(POSE, 0);
	}

	/** The pose it shows, 0 to 9 ({@code pose1} to {@code pose10}). */
	public int pose() {
		return this.entityData.get(POSE);
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
		SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
		Sounds.playAt(this, FossilsModule.SKELETON_PLACE_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
		float facing = this.getRandom().nextFloat() * 360.0F;
		this.snapTo(this.getX(), this.getY(), this.getZ(), facing, 0.0F);
		this.setYBodyRot(facing);
		this.setYHeadRot(facing);
		this.yBodyRotO = facing;
		this.yHeadRotO = facing;
		return result;
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
		if (player.isShiftKeyDown()) {
			if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
			player.swing(hand, SwingAnimation.DEFAULT, true);
			if (!this.level().isClientSide()) {
				this.discard();
				player.setItemInHand(hand, new ItemStack(BuiltInRegistries.ITEM.getValue(FossilsIds.Items.ANCIENT_SKELETON_EXHIBIT_ITEM)));
			}
			return InteractionResult.SUCCESS;
		}
		player.swing(hand, SwingAnimation.DEFAULT, true);
		if (!this.level().isClientSide()) {
			this.entityData.set(POSE, (pose() + 1) % POSES);
			Sounds.playAt(this, FossilsModule.SKELETON_HURT_SOUND, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (isShruggedOff(source)) return false;
		return super.hurtServer(level, source, damage);
	}

	private static boolean isShruggedOff(DamageSource source) {
		Entity direct = source.getDirectEntity();
		if (direct instanceof AbstractArrow || direct instanceof Player
			|| direct instanceof AbstractThrownPotion || direct instanceof AreaEffectCloud) {
			return true;
		}
		return source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.FALL) || source.is(DamageTypes.CACTUS)
			|| source.is(DamageTypes.DROWN) || source.is(DamageTypes.LIGHTNING_BOLT) || source.is(DamageTypes.EXPLOSION)
			|| source.is(DamageTypes.TRIDENT) || source.is(DamageTypes.FALLING_ANVIL) || source.is(DamageTypes.DRAGON_BREATH)
			|| source.is(DamageTypes.WITHER) || source.is(DamageTypes.WITHER_SKULL) || source.is(DamageTypes.IN_WALL);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!this.level().isClientSide()) {
			Sounds.playAt(this, FossilsModule.SKELETON_HURT_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
			this.discard();
		}
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return FossilsModule.SKELETON_HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return FossilsModule.SKELETON_HURT_SOUND;
	}

	@Override
	public boolean canBeCollidedWith(@Nullable Entity other) {
		return true;
	}

	@Override
	public boolean canCollideWith(Entity entity) {
		return true;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Datapose", pose());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.entityData.set(POSE, input.getIntOr("Datapose", 0));
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<AncientSkeletonExhibitEntity>("pose", 2,
			test -> test.setAndContinue(POSE_ANIMATIONS[Math.floorMod(pose(), POSES)])));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
