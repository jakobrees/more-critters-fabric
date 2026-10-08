package com.morecritters.fabric.module.snowflake_spider;

import com.morecritters.fabric.ids.NightshroomIds;
import com.morecritters.fabric.core.Config;
import com.morecritters.fabric.core.ServerScheduler;
import com.morecritters.fabric.ids.MightshroomIds;
import com.morecritters.fabric.ids.ShockCubeIds;
import com.morecritters.fabric.ids.ShriekbatIds;
import com.morecritters.fabric.ids.SnowflakeSpiderIds;
import com.morecritters.fabric.ids.WanderingCollectorIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * A thrown sack of freezing web. Hitting a block, it spins a small random tuft of freezing
 * cobwebs above the spot; hitting a small enough mob, it wraps it in webs for good (the webbed
 * effect, forever); a mob too big to wrap gets cobwebs spun around it instead. It never sticks
 * in the ground, and does nothing special in the Nether.
 */
public class WebSackProjectile extends AbstractArrow implements ItemSupplier {
	private static final double BASE_DAMAGE = 2.0;
	/** Mobs with at most this much maximum health get webbed; config {@code items.webbed_requirement}. */
	private static final double WEBBED_MAX_HEALTH_DEFAULT = 50.0;
	private static final double SHOOT_POWER = 0.7;

	/** Critters the sack splats on without webbing them (and players). */
	private static final Set<Identifier> UNWEBBABLE = Set.of(
		NightshroomIds.Entities.ANCIENT_SKELETON,
		MightshroomIds.Entities.HEAL_ECHO,
		NightshroomIds.Entities.MORI_ROOTS,
		ShriekbatIds.Entities.ECHO,
		ShriekbatIds.Entities.LARGE_ECHO,
		ShriekbatIds.Entities.SHRIEKBOMB_PROJECTILE,
		ShockCubeIds.Entities.SHOCK_CUBE,
		ShockCubeIds.Entities.SHOCK_CUBE_SMALL,
		SnowflakeSpiderIds.Entities.WEB_ENTITY,
		WanderingCollectorIds.Entities.CARRYBUG
	);

	/**
	 * Where cobwebs go after a block hit, relative to the block hit, in order: one right above
	 * it, one above that, then one of each pair picked at random.
	 */
	private static final BlockPos FIRST_WEB = new BlockPos(0, 1, 0);
	private static final BlockPos SECOND_WEB = new BlockPos(0, 2, 0);
	private static final BlockPos[][] RANDOM_WEBS = {
		{new BlockPos(0, 1, 1), new BlockPos(0, 1, -1)},
		{new BlockPos(1, 2, -1), new BlockPos(-1, 2, -1)},
	};

	public WebSackProjectile(EntityType<? extends WebSackProjectile> type, Level level) {
		super(type, level);
	}

	private WebSackProjectile(LivingEntity shooter, Level level) {
		super(SnowflakeSpiderModule.WEB_SACK_PROJECTILE, shooter, level, new ItemStack(SnowflakeSpiderModule.WEB_SACK), null);
		this.setSilent(true);
		this.setCritArrow(false);
		this.setBaseDamage(BASE_DAMAGE);
	}

	/** Throws a web sack the way the shooter is looking, as the web sack item does. */
	public static WebSackProjectile shoot(LivingEntity shooter) {
		Level level = shooter.level();
		WebSackProjectile sack = new WebSackProjectile(shooter, level);
		Vec3 look = shooter.getViewVector(1.0F);
		sack.shoot(look.x, look.y, look.z, (float) (SHOOT_POWER * 2.0), 0.0F);
		level.addFreshEntity(sack);
		float pitch = 1.0F / (shooter.getRandom().nextFloat() * 0.5F + 1.0F) + (float) (SHOOT_POWER / 2.0);
		level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, pitch);
		return sack;
	}

	/** Lobs a web sack from a mob at its target, a little inaccurately, like a skeleton's arrow. */
	public static WebSackProjectile shoot(LivingEntity shooter, LivingEntity target) {
		Level level = shooter.level();
		WebSackProjectile sack = new WebSackProjectile(shooter, level);
		double dx = target.getX() - shooter.getX();
		double dy = target.getY() + target.getEyeHeight() - 1.1;
		double dz = target.getZ() - shooter.getZ();
		sack.shoot(dx, dy - sack.getY() + Math.hypot(dx, dz) * 0.2, dz, 1.4F, 12.0F);
		level.addFreshEntity(sack);
		float pitch = 1.0F / (shooter.getRandom().nextFloat() * 0.5F + 1.0F);
		level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, pitch);
		return sack;
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		return new ItemStack(SnowflakeSpiderModule.WEB_SACK);
	}

	@Override
	public ItemStack getItem() {
		return this.getPickupItemStackOrigin();
	}

	@Override
	public void tick() {
		super.tick();
		if (this.isInGround()) {
			this.discard();
		}
	}

	/** The sack does not stay stuck in the mob like an arrow. */
	@Override
	protected void doPostHurtEffects(LivingEntity mob) {
		super.doPostHurtEffects(mob);
		mob.setArrowCount(mob.getArrowCount() - 1);
	}

	@Override
	protected void onHitBlock(BlockHitResult hitResult) {
		super.onHitBlock(hitResult);
		if (!(this.level() instanceof ServerLevel level)) return;
		BlockPos hit = hitResult.getBlockPos();
		playHitSound(level, hit);
		spinWeb(level, hit.offset(FIRST_WEB));
		// The rest of the tuft grows over the next tick.
		ServerScheduler.runLater(0, () -> spinWeb(level, hit.offset(SECOND_WEB)));
		for (BlockPos[] pair : RANDOM_WEBS) {
			BlockPos offset = pair[this.random.nextInt(pair.length)];
			ServerScheduler.runLater(1, () -> spinWeb(level, hit.offset(offset)));
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		if (!(this.level() instanceof ServerLevel level) || level.dimension() == Level.NETHER) return;
		Entity target = hitResult.getEntity();
		if (target instanceof Player || UNWEBBABLE.contains(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()))) {
			discardNearbySacks(level);
		} else if (maxHealth(target) <= Config.number("webbed_requirement", WEBBED_MAX_HEALTH_DEFAULT)) {
			wrap(level, target);
		} else {
			webAround(level);
		}
	}

	/** Webs a small mob for good, in a burst of snowflakes. */
	private void wrap(ServerLevel level, Entity target) {
		playHitSound(level, this.blockPosition());
		if (target instanceof LivingEntity living) {
			living.addEffect(new MobEffectInstance(SnowflakeSpiderModule.WEBBED, Integer.MAX_VALUE, 0, false, false));
		}
		int bursts = Mth.nextInt(this.random, 2, 5) * 2;
		for (int i = 0; i < bursts; i++) {
			snowflakes(level, this.getX(), this.getY(), this.getZ());
		}
	}

	/** Too big to wrap: cobwebs are spun where the sack hit and just above. */
	private void webAround(ServerLevel level) {
		playHitSound(level, this.blockPosition());
		snowflakes(level, this.getX(), this.getY() + 1.0, this.getZ());
		cobwebBits(level, this.getX(), this.getY() + 1.0, this.getZ());
		BlockPos here = BlockPos.containing(this.getX(), this.getY(), this.getZ());
		placeWeb(level, here.above());
		placeWeb(level, here);
	}

	/** Against players and the listed critters the sack just splats, taking any sack beside it along. */
	private void discardNearbySacks(ServerLevel level) {
		AABB around = new AABB(this.position(), this.position()).inflate(1.0);
		for (WebSackProjectile sack : level.getEntitiesOfClass(WebSackProjectile.class, around)) {
			sack.discard();
		}
	}

	/** Places a freezing cobweb in open air, with snowflakes and bits of web at the block's corner. */
	private static void spinWeb(ServerLevel level, BlockPos pos) {
		if (placeWeb(level, pos)) {
			snowflakes(level, pos.getX(), pos.getY(), pos.getZ());
			cobwebBits(level, pos.getX(), pos.getY(), pos.getZ());
		}
	}

	private static boolean placeWeb(ServerLevel level, BlockPos pos) {
		if (!level.getBlockState(pos).is(Blocks.AIR)) return false;
		level.setBlock(pos, SnowflakeSpiderModule.FREEZING_COBWEB.defaultBlockState(), 3);
		return true;
	}

	private static void snowflakes(ServerLevel level, double x, double y, double z) {
		level.sendParticles(ParticleTypes.SNOWFLAKE, true, false, x, y, z, 10, 0.5, 0.8, 0.5, 0.03);
	}

	private static void cobwebBits(ServerLevel level, double x, double y, double z) {
		BlockParticleOption bits = new BlockParticleOption(ParticleTypes.BLOCK, SnowflakeSpiderModule.FREEZING_COBWEB.defaultBlockState());
		level.sendParticles(bits, true, false, x, y, z, 10, 0.2, 0.2, 0.2, 0.03);
	}

	private static void playHitSound(ServerLevel level, BlockPos pos) {
		level.playSound(null, pos, SnowflakeSpiderModule.WEB_SACK_HIT_SOUND, SoundSource.AMBIENT, 1.0F, 1.0F);
	}

	/** Anything that is not a living mob counts as tiny, as in the original. */
	private static float maxHealth(Entity entity) {
		return entity instanceof LivingEntity living ? living.getMaxHealth() : -1.0F;
	}
}
