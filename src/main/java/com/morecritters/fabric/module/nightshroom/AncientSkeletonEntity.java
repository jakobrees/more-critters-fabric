package com.morecritters.fabric.module.nightshroom;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.TextureVariants;
import com.morecritters.fabric.ids.FossilsIds;
import com.morecritters.fabric.ids.MiscIds;
import com.morecritters.fabric.module.mightshroom.MightshroomModule;
import com.morecritters.fabric.module.mightshroom.ShroomRaisable;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The bones of a great shroom-bird, found in the ground or placed from its item. Nothing can
 * hurt it; blows only rattle it. It is the body the mightshroom module's raising ritual works
 * on ({@link ShroomRaisable}): a stew of death or of life poured over it takes root (mori or vita
 * mushrooms grow on the bones), and a purgatorial mixture then makes it shake amid sparks and
 * rise as a frightshroom, a nightshroom or a mightshroom. One placed by a player can be picked up
 * again by sneaking with an empty hand. Solid like a boat.
 */
public class AncientSkeletonEntity extends PathfinderMob implements GeoEntity, TextureVariants, ShroomRaisable {
	/** The stew poured over it, as the original's {@code shroomed}: 0 none, 1 death, 2 life. */
	private static final EntityDataAccessor<Integer> SHROOMED = SynchedEntityData.defineId(AncientSkeletonEntity.class, EntityDataSerializers.INT);
	private static final Soup[] SOUPS = {Soup.NONE, Soup.DEATH, Soup.LIFE};
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("fossil_idle");

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	/** Placed from the item by a player, and so may be picked up again. */
	private boolean placed;

	public AncientSkeletonEntity(EntityType<? extends AncientSkeletonEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 50.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SHROOMED, Soup.NONE.ordinal());
	}

	@Override
	public Soup soup() {
		return SOUPS[Math.floorMod(this.entityData.get(SHROOMED), SOUPS.length)];
	}

	@Override
	public void pourSoup(Soup soup) {
		this.entityData.set(SHROOMED, soup.ordinal());
	}

	@Override
	public void startRising() {
		this.triggerAnim(Animations.ACTIONS, "fossil_shake");
	}

	@Override
	public void comeAlive() {
		this.triggerAnim(Animations.ACTIONS, "fossil_alive");
	}

	/** Under the raising ritual (the mightshroom module's Spawn Mightshroom effect). */
	private boolean isRising() {
		return MightshroomModule.SPAWN_MIGHTSHROOM != null && this.hasEffect(MightshroomModule.SPAWN_MIGHTSHROOM);
	}

	void markPlaced() {
		this.placed = true;
	}

	/** Settles into the ground facing a random way, with the sound of bones being laid down. */
	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, groupData);
		ServerScheduler.runLater(1, () -> this.triggerAnim(Animations.ACTIONS, "fossil_spawn"));
		OtherModules.sound(FossilsIds.Sounds.ENTITY_ANCIENT_SKELETON_PLACE)
			.ifPresent(sound -> Sounds.playAt(this, sound, SoundSource.NEUTRAL, 1.0F, 1.0F));
		float facing = this.random.nextFloat() * 360.0F;
		this.snapTo(this.getX(), this.getY(), this.getZ(), facing, 0.0F);
		this.setYBodyRot(facing);
		this.setYHeadRot(facing);
		this.yBodyRotO = facing;
		this.yHeadRotO = facing;
		return result;
	}

	/** Sneaking with an empty hand packs a player-placed skeleton back into its item. */
	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
		// The client does not know whether it was placed; it only claims the click.
		if (this.level().isClientSide()) return InteractionResult.SUCCESS;
		if (!this.placed || isRising()) return InteractionResult.PASS;
		player.swing(hand, SwingAnimation.DEFAULT, true);
		this.discard();
		player.setItemInHand(hand, new ItemStack(NightshroomItems.ancientSkeleton));
		return InteractionResult.SUCCESS;
	}

	/** Black and yellow sparks around the bones while the Spawn Nightshroom effect works on them. */
	void sparkStripes(ServerLevel level) {
		Vec3 at = this.position().add(0.0, 1.0, 0.0);
		OtherModules.particle(MiscIds.Particles.BLACK_STRIPE).ifPresent(stripe -> Bursts.forced(level, stripe, at, 6, 1.0, 1.0, 1.0, 0.0));
		OtherModules.particle(MiscIds.Particles.YELLOW_STRIPE).ifPresent(stripe -> Bursts.forced(level, stripe, at, 6, 1.0, 1.0, 1.0, 0.0));
	}

	/** Blows only rattle it (not while it is rising); only the void and /kill get rid of it. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurtServer(level, source, damage);
		if (!isRising()) {
			OtherModules.sound(FossilsIds.Sounds.ENTITY_ANCIENT_SKELETON_HURT)
				.ifPresent(sound -> Sounds.playAt(this, sound, SoundSource.NEUTRAL, 1.0F, 1.0F));
			this.triggerAnim(Animations.ACTIONS, "fossil_hurt");
		}
		return false;
	}

	/** Falls apart at once, with no death animation. */
	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!this.level().isClientSide()) this.discard();
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return OtherModules.sound(FossilsIds.Sounds.ENTITY_ANCIENT_SKELETON_HURT).orElse(null);
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return OtherModules.sound(FossilsIds.Sounds.ENTITY_ANCIENT_SKELETON_HURT).orElse(null);
	}

	@Override
	public String textureName() {
		return switch (soup()) {
			case DEATH -> "ancient_skeleton_mori";
			case LIFE -> "ancient_skeleton_vita";
			case NONE -> "ancient_skeleton";
		};
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
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Datashroomed", soup().ordinal());
		output.putBoolean("Placed", this.placed);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.entityData.set(SHROOMED, input.getIntOr("Datashroomed", Soup.NONE.ordinal()));
		this.placed = input.getBooleanOr("Placed", false);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(Animations.MOVEMENT, 0, (AnimationTest<AncientSkeletonEntity> test) -> test.setAndContinue(IDLE)));
		controllers.add(Animations.actions(this, "fossil_spawn", "fossil_hurt", "fossil_shake", "fossil_alive"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
