package com.morecritters.fabric.module.shock_cube;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.TextureVariants;
import com.morecritters.fabric.ids.CreeblossomIds;
import com.morecritters.fabric.ids.ShockCubeIds;
import com.morecritters.fabric.ids.ShriekbatIds;
import com.morecritters.fabric.ids.StincarpIds;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A crackling cube of electricity left behind where something electric bursts. It hangs in
 * place for two seconds, fading through four textures, and while it lasts it zaps whatever
 * stands within four blocks with lightning. The big one can be scooped into a glass bottle.
 */
public class ShockCubeEntity extends PathfinderMob implements GeoEntity, TextureVariants {
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	/** Ticks the cube lives. */
	private static final int LIFETIME = 40;
	private static final double ZAP_RANGE = 4.0;
	/** One tick in this many it zaps its surroundings. */
	private static final int ZAP_CHANCE = 10;

	/** Damage the cube ignores, as in the original. */
	private static final List<ResourceKey<DamageType>> IMMUNE_TO = List.of(
		DamageTypes.IN_FIRE, DamageTypes.FALL, DamageTypes.CACTUS, DamageTypes.DROWN, DamageTypes.LIGHTNING_BOLT,
		DamageTypes.EXPLOSION, DamageTypes.TRIDENT, DamageTypes.FALLING_ANVIL, DamageTypes.DRAGON_BREATH,
		DamageTypes.WITHER, DamageTypes.WITHER_SKULL, DamageTypes.IN_WALL
	);

	/** Electric creatures (and the cubes themselves) that a cube never zaps. */
	private static final Set<Identifier> SPARED = Set.of(
		ShockCubeIds.Entities.SHOCK_CUBE,
		ShockCubeIds.Entities.SHOCK_CUBE_SMALL,
		ShriekbatIds.Entities.ECHO, ShriekbatIds.Entities.LARGE_ECHO,
		StincarpIds.Entities.STINCARP, CreeblossomIds.Entities.CREEBLOSSOM
	);

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private int timeLeft = LIFETIME;

	public ShockCubeEntity(EntityType<? extends ShockCubeEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setNoAi(true);
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.MAX_HEALTH, 10.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		this.setDeltaMovement(Vec3.ZERO);
		timeLeft--;
		if (this.level() instanceof ServerLevel level) {
			if (timeLeft < 0) {
				this.discard();
				return;
			}
			if (this.random.nextInt(ZAP_CHANCE) == 0) {
				zapSurroundings(level);
			}
		}
	}

	/** Strikes everything nearby that is not electric itself or a dropped item. */
	private void zapSurroundings(ServerLevel level) {
		Vec3 centre = this.position();
		for (Entity target : level.getEntities(this, new AABB(centre, centre).inflate(ZAP_RANGE))) {
			if (target instanceof ItemEntity || SPARED.contains(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()))) continue;
			target.hurtServer(level, level.damageSources().lightningBolt(), (float) Mth.nextDouble(this.random, 2.0, 6.0));
		}
	}

	/** Fades from bright to dim over its life. */
	@Override
	public String textureName() {
		if (timeLeft > 30) return "shock_cube1";
		if (timeLeft > 20) return "shock_cube2";
		if (timeLeft > 10) return "shock_cube3";
		return "shock_cube4";
	}

	/** The big cube can be bottled: a glass bottle in the main hand becomes a bottle o' electricity. */
	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getMainHandItem();
		if (this.getType() != ShockCubeModule.SHOCK_CUBE || !held.is(Items.GLASS_BOTTLE)) {
			return super.mobInteract(player, hand);
		}
		if (this.level() instanceof ServerLevel) {
			held.shrink(1);
			this.level().playSound(null, this.blockPosition(), SoundEvents.BOTTLE_FILL_DRAGONBREATH, SoundSource.PLAYERS, 1.0F, 1.0F);
			ItemStack bottle = new ItemStack(ShockCubeModule.BOTTLE_OF_ELECTRICITY);
			if (!player.getInventory().add(bottle)) player.spawnAtLocation((net.minecraft.server.level.ServerLevel) player.level(), bottle);
			this.discard();
		}
		return InteractionResult.SUCCESS;
	}

	/** Shrugs off most environmental damage, arrows, potions and players' hits. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		Entity direct = source.getDirectEntity();
		if (direct instanceof AbstractArrow || direct instanceof Player || direct instanceof AbstractThrownPotion || direct instanceof AreaEffectCloud) return false;
		for (ResourceKey<DamageType> immune : IMMUNE_TO) {
			if (source.is(immune)) return false;
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.GENERIC_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.GENERIC_DEATH;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	protected void pushEntities() {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("TimeLeft", timeLeft);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		timeLeft = input.getIntOr("TimeLeft", LIFETIME);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>("idle", 0, test -> test.setAndContinue(IDLE)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animations;
	}
}
