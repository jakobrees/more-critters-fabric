package com.morecritters.fabric.module.shriekbat;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import java.util.List;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.morecritters.fabric.core.TextureVariants;

/**
 * A ring of sound left in the air for two seconds; it stands still and then vanishes. The large echo
 * (the shriek bomb's burst) is the same thing drawn twice the size. Both use the tester shriek's model.
 */
public class EchoEntity extends PathfinderMob implements GeoEntity, TextureVariants {
	static final int LIFETIME = 40;
	private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(EchoEntity.class, EntityDataSerializers.INT);
	/** Damage that never touches it, as in the original. */
	private static final List<ResourceKey<DamageType>> IMMUNE_TO = List.of(
		DamageTypes.IN_FIRE, DamageTypes.FALL, DamageTypes.CACTUS, DamageTypes.DROWN, DamageTypes.LIGHTNING_BOLT, DamageTypes.EXPLOSION,
		DamageTypes.TRIDENT, DamageTypes.FALLING_ANVIL, DamageTypes.DRAGON_BREATH, DamageTypes.WITHER, DamageTypes.WITHER_SKULL, DamageTypes.IN_WALL);

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	protected int lifeLeft = LIFETIME;

	public EchoEntity(EntityType<? extends EchoEntity> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 10.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(STAGE, 0);
	}

	/** Stage 0 is the plain texture; the subclass fades through 1..3. */
	protected void setStage(int stage) {
		this.entityData.set(STAGE, stage);
	}

	@Override
	public String textureName() {
		int stage = this.entityData.get(STAGE);
		return stage == 0 ? "tester_shriek" : "tester_shriek" + stage;
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!(this.level() instanceof ServerLevel level)) return;
		move(level);
		this.lookAt(EntityAnchorArgument.Anchor.EYES, this.position());
		if (--this.lifeLeft <= 0) {
			this.discard();
			return;
		}
		age(level);
	}

	/** An echo hangs where it is. */
	protected void move(ServerLevel level) {
		this.setDeltaMovement(0.0, 0.0, 0.0);
	}

	protected void age(ServerLevel level) {
	}

	protected boolean airBelow() {
		return this.level().getBlockState(BlockPos.containing(this.getX(), this.getY() - 1.0, this.getZ())).isAir();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		for (ResourceKey<DamageType> immune : IMMUNE_TO) {
			if (source.is(immune)) return false;
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("LifeLeft", this.lifeLeft);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.lifeLeft = input.getIntOr("LifeLeft", LIFETIME);
	}

	private static final RawAnimation LOOP = RawAnimation.begin().thenLoop("0");

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>("loop", 0, (AnimationTest<EchoEntity> test) -> test.setAndContinue(LOOP)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animations;
	}
}
