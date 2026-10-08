package com.morecritters.fabric.module.shadelet;

import com.morecritters.fabric.ids.NightshroomIds;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.Drops;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.ids.CustodianIds;
import com.morecritters.fabric.ids.FossilsIds;
import com.morecritters.fabric.ids.IropodIds;
import com.morecritters.fabric.ids.MightshroomIds;
import com.morecritters.fabric.ids.ShockCubeIds;
import com.morecritters.fabric.ids.ShriekbatIds;
import com.morecritters.fabric.ids.SnowflakeSpiderIds;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Chattering teeth: a wind-up toy set down from the item. It hops forward, chomping (5 damage) whatever it
 * runs into, and winds down back into an item when it hits a wall, touches water, or is hit by a creature.
 */
public class ChatteringTeethEntity extends PathfinderMob implements GeoEntity {
	private static final ResourceKey<DamageType> CHATTER = ResourceKey.create(Registries.DAMAGE_TYPE, MoreCritters.id("chatter"));
	private static final float BITE_DAMAGE = 5.0F;
	private static final double BITE_RANGE = 1.0;
	private static final double HOP_WHILE_BITING = 0.05, HOP = 0.1, HOP_DOWN = -0.5;
	/** Other modules' sturdy or effect-like entities the teeth do not bite (by id, so their modules may be stubs). */
	private static final Set<Identifier> UNBITEABLE = Set.of(
		NightshroomIds.Entities.ANCIENT_SKELETON, ShriekbatIds.Entities.ECHO, MightshroomIds.Entities.HEAL_ECHO,
		ShriekbatIds.Entities.LARGE_ECHO, NightshroomIds.Entities.MORI_ROOTS, ShockCubeIds.Entities.SHOCK_CUBE,
		ShockCubeIds.Entities.SHOCK_CUBE_SMALL, SnowflakeSpiderIds.Entities.WEB_ENTITY, ShriekbatIds.Entities.SHRIEKBOMB_PROJECTILE,
		FossilsIds.Entities.ANCIENT_SKELETON_EXHIBIT, CustodianIds.Entities.ANCIENT_CUSTODIAN, CustodianIds.Entities.CUSTODIAN,
		IropodIds.Entities.IROBALL, NightshroomIds.Entities.ROT_SPLASH);

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);

	public ChatteringTeethEntity(EntityType<? extends ChatteringTeethEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
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
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}
		chompAndHop(level);
		this.setSprinting(true);
		if (this.isInWater() || bumpedIntoWall()) {
			windDown(level);
		}
	}

	/** Every entity it touches (itself included) gives it a forward hop; biteable ones are chomped and slow the hop. */
	private void chompAndHop(ServerLevel level) {
		Vec3 centre = this.position();
		Vec3 look = this.getLookAngle();
		for (Entity other : level.getEntities(this, new AABB(centre, centre).inflate(BITE_RANGE), e -> true)) {
			boolean bite = !(other instanceof ItemEntity || other instanceof ChatteringTeethEntity) && !UNBITEABLE.contains(BuiltInRegistries.ENTITY_TYPE.getKey(other.getType()));
			if (bite) {
				other.hurtServer(level, new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(CHATTER)), BITE_DAMAGE);
			}
			hop(look, bite ? HOP_WHILE_BITING : HOP);
		}
		hop(look, HOP);
	}

	private void hop(Vec3 look, double strength) {
		if (this.onGround()) {
			this.push(strength * look.x, HOP_DOWN, strength * look.z);
		}
	}

	private boolean bumpedIntoWall() {
		double x = getX(), y = getY(), z = getZ();
		return solidAt(x + 0.5, y, z) || solidAt(x - 0.5, y, z) || solidAt(x, y, z + 0.5) || solidAt(x, y, z - 0.5);
	}

	private boolean solidAt(double x, double y, double z) {
		return this.level().getBlockState(BlockPos.containing(x, y, z)).canOcclude();
	}

	/** Turns back into the item with a winding-down click. */
	private void windDown(ServerLevel level) {
		this.discard();
		Drops.dropSingles(level, this.position(), ShadeletModule.CHATTERING_TEETH_ITEM, 1);
		Sounds.playAt(this, ShadeletModule.TEETH_END_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
	}

	/** A creature's hit (or being stuck in a wall) packs it up; it cannot otherwise be hurt by players, arrows or most hazards. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() != null && (this.isInWall() || source.getEntity() instanceof LivingEntity)) {
			windDown(level);
		}
		if (source.getDirectEntity() instanceof AbstractThrownPotion || source.getDirectEntity() instanceof AreaEffectCloud
			|| ShadeletEntity.isImmuneTo(source)) {
			return false;
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(ShadeletModule.TEETH_STEP_SOUND, 0.15F, 1.0F);
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
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(Animations.idleWalk(this, "idle", "walk"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animations;
	}
}
