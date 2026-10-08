package com.morecritters.fabric.module.bunbug;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Advancements;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Config;
import com.morecritters.fabric.core.Drops;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.core.TextureVariants;
import com.morecritters.fabric.MoreCritters;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A desert bug that can be decorated like a pastry: iced with sugar or cocoa, then
 * sprinkled, then topped with a berry. Every so often it hops and sheds a few crusts
 * in its current flavour. Breeding two produces a litter of {@link BabyBunbugEntity}.
 */
public class BunbugEntity extends Animal implements GeoEntity, TextureVariants {
	/** 0 = plain, 1 = sugar, 2 = chocolate. */
	private static final EntityDataAccessor<Integer> ICING = SynchedEntityData.defineId(BunbugEntity.class, EntityDataSerializers.INT);
	/** 0 = none, 1 = sprinkled. */
	private static final EntityDataAccessor<Integer> SPRINKLES = SynchedEntityData.defineId(BunbugEntity.class, EntityDataSerializers.INT);
	/** 0 = none, 1 = sweet berries, 2 = glow berries, 3 = bounceberries. */
	private static final EntityDataAccessor<Integer> BERRIES = SynchedEntityData.defineId(BunbugEntity.class, EntityDataSerializers.INT);

	private static final int FIRST_SHED_MIN = 10_000, FIRST_SHED_MAX = 15_000;
	private static final int SHED_MIN = 45_000, SHED_MAX = 64_000;
	private static final int SHED_DROP_DELAY = 5;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	/** Ticks until the next shed. */
	private int shedTimer;

	public BunbugEntity(EntityType<? extends BunbugEntity> type, Level level) {
		super(type, level);
		this.xpReward = 2;
	}

	public static AttributeSupplier.Builder createAttributes() {
		// Animals, not Mob: TemptGoal needs the tempt_range attribute.
		return Animal.createAnimalAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 7.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ICING, 0);
		builder.define(SPRINKLES, 0);
		builder.define(BERRIES, 0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new BreedGoal(this, 1.0));
		this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(3, new PanicGoal(this, 1.2));
		this.goalSelector.addGoal(4, new TemptGoal(this, 1.0, this::isFood, false));
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(6, new FloatGoal(this));
	}

	// --- decoration state -------------------------------------------------------------

	public int icing() { return this.entityData.get(ICING); }
	public int sprinkles() { return this.entityData.get(SPRINKLES); }
	public int berries() { return this.entityData.get(BERRIES); }

	/** Textures are named {@code bunbug_<icing>_<sprinkles>_<berries>}. */
	@Override
	public String textureName() {
		return "bunbug_" + icing() + "_" + sprinkles() + "_" + berries();
	}

	/** The crust this bunbug sheds in its current decoration. */
	private Item crustItem() {
		return BunbugItems.crustFor(icing(), sprinkles(), berries());
	}

	// --- lifecycle --------------------------------------------------------------------

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		this.shedTimer = Mth.nextInt(this.random, FIRST_SHED_MIN, FIRST_SHED_MAX);
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!(this.level() instanceof ServerLevel level)) return;

		// A bred bunbug is born as a litter of babies rather than growing up itself.
		if (this.isBaby()) {
			this.discard();
			int litter = Config.integer("bunbug_birth_number", 4);
			for (int i = 0; i < litter; i++) {
				BabyBunbugEntity baby = BunbugModule.BABY_BUNBUG.spawn(level, this.blockPosition(), EntitySpawnReason.MOB_SUMMONED);
				if (baby != null) {
					baby.setYRot(this.getYRot());
					baby.setYBodyRot(this.getYRot());
					baby.setYHeadRot(this.getYRot());
					baby.setXRot(this.getXRot());
					baby.setDeltaMovement(Vec3.ZERO);
				}
			}
			return;
		}

		if (--this.shedTimer <= 0) {
			this.shedTimer = Mth.nextInt(this.random, SHED_MIN, SHED_MAX);
			shed();
		}
	}

	/** Hops, then a moment later leaves one to two crusts behind. */
	private void shed() {
		if (this.onGround()) this.setDeltaMovement(0.0, 0.2, 0.0);
		Item crust = crustItem();
		ServerScheduler.runLater(SHED_DROP_DELAY, () -> Drops.dropSingles(this, crust, Drops.randomCount(this, 1, 3)));
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		InteractionResult result = super.mobInteract(player, hand);
		ItemStack held = player.getMainHandItem();
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);

		if (icing() == 0) {
			if (held.is(Items.SUGAR)) return decorate(player, held, ICING, 1, BunbugModule.CREAM_SOUND);
			if (held.is(Items.COCOA_BEANS)) return decorate(player, held, ICING, 2, BunbugModule.CREAM_SOUND);
		} else if (sprinkles() == 0) {
			if (held.is(BunbugModule.sprinklesItem())) return decorate(player, held, SPRINKLES, 1, BunbugModule.SPRINKLE_SOUND);
		} else if (berries() == 0) {
			int berry = held.is(Items.SWEET_BERRIES) ? 1 : held.is(Items.GLOW_BERRIES) ? 2 : held.is(BunbugModule.bounceberryItem()) ? 3 : 0;
			if (berry != 0) {
				Advancements.award(player, MoreCritters.id("fully_decorate_bunbug"));
				return decorate(player, held, BERRIES, berry, BunbugModule.BERRY_SOUND);
			}
		}
		return result;
	}

	private InteractionResult decorate(Player player, ItemStack ingredient, EntityDataAccessor<Integer> stage, int value, SoundEvent sound) {
		this.entityData.set(stage, value);
		if (!player.hasInfiniteMaterials()) ingredient.shrink(1);
		Sounds.playAt(this, sound);
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		// In addition to the loot table, as in the original.
		Drops.dropSingles(this, BunbugModule.rawMeatItem(), Drops.randomCount(this, 1, 3));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Icing", icing());
		output.putInt("Sprinkles", sprinkles());
		output.putInt("Berries", berries());
		output.putInt("ShedTimer", this.shedTimer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.entityData.set(ICING, input.getIntOr("Icing", 0));
		this.entityData.set(SPRINKLES, input.getIntOr("Sprinkles", 0));
		this.entityData.set(BERRIES, input.getIntOr("Berries", 0));
		this.shedTimer = input.getIntOr("ShedTimer", Mth.nextInt(this.random, FIRST_SHED_MIN, FIRST_SHED_MAX));
	}

	// --- vanilla hooks ----------------------------------------------------------------

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Blocks.DEAD_BUSH.asItem());
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return BunbugModule.BUNBUG.create(level, EntitySpawnReason.BREEDING);
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).scale(1.2F);
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(BunbugModule.STEP_SOUND, 0.15F, 1.0F);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return BunbugModule.HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return BunbugModule.DEATH_SOUND;
	}

	// --- GeckoLib ---------------------------------------------------------------------

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(Animations.idleWalk(this, "idle", "walk"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
