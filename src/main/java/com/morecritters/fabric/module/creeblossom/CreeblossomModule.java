package com.morecritters.fabric.module.creeblossom;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.MoreCritters;
import com.morecritters.fabric.core.Advancements;
import com.morecritters.fabric.core.Config;
import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.ids.CreeblossomIds;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The creeblossom: a flower-headed creeper that grows from blossombushes, the blossoming
 * infection that turns badlands and savanna monsters into creeblossom seed pods, and the potted bush.
 */
public final class CreeblossomModule implements Module {
	public static EntityType<CreeblossomEntity> CREEBLOSSOM;
	public static Holder<MobEffect> BLOSSOMING;
	public static SimpleParticleType BLOSSOM_PARTICLE, BLOSSOM_EXPLOSION, RECRUITED_LEAF;

	static SoundEvent HURT_SOUND, DEATH_SOUND, ATTACK_SOUND, PRIMED_SOUND, SPAWN_SOUND, OPEN_CLOSE_SOUND, BLOSSOMING_EXPLOSION_SOUND;
	static SoundEvent ELECTRIC_HURT_SOUND, ELECTRIC_DEATH_SOUND, ELECTRIC_SPAWN_SOUND, ELECTRIC_PRIMED_SOUND, ELECTRIC_ATTACK_SOUND, ELECTRIC_EXPLODE_SOUND;

	static Item blossombushSeed;
	static Block blossombush, closedBlossombush, electricBlossombush, closedElectricBlossombush, potBlossombush, potElectricBlossombush;

	/** Biomes where monsters may arrive already blossoming. */
	private static final List<ResourceKey<Biome>> INFECTION_BIOMES = List.of(
		Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS,
		Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA, Biomes.DESERT);
	private static final int BURST_PARTICLES = 7;
	private static final double BURST_SPEED = 0.1, BURST_LIFT = 0.2;

	@Override
	public void register() {
		HURT_SOUND = Registration.sound(CreeblossomIds.Sounds.ENTITY_CREEBLOSSOM_HURT);
		DEATH_SOUND = Registration.sound(CreeblossomIds.Sounds.ENTITY_CREEBLOSSOM_DEATH);
		ATTACK_SOUND = Registration.sound(CreeblossomIds.Sounds.ENTITY_CREEBLOSSOM_ATTACK);
		PRIMED_SOUND = Registration.sound(CreeblossomIds.Sounds.ENTITY_CREEBLOSSOM_PRIMED);
		SPAWN_SOUND = Registration.sound(CreeblossomIds.Sounds.ENTITY_CREEBLOSSOM_SPAWN);
		OPEN_CLOSE_SOUND = Registration.sound(CreeblossomIds.Sounds.BLOCK_BLOSSOMBUSH_OPENCLOSE);
		BLOSSOMING_EXPLOSION_SOUND = Registration.sound(CreeblossomIds.Sounds.ENTITY_BLOSSOMING_EXPLOSION);
		ELECTRIC_HURT_SOUND = Registration.sound(CreeblossomIds.Sounds.ENTITY_ELECTRIC_CREEBLOSSOM_HURT);
		ELECTRIC_DEATH_SOUND = Registration.sound(CreeblossomIds.Sounds.ENTITY_ELECTRIC_CREEBLOSSOM_DEATH);
		ELECTRIC_SPAWN_SOUND = Registration.sound(CreeblossomIds.Sounds.ENTITY_ELECTRIC_CREEBLOSSOM_SPAWN);
		ELECTRIC_PRIMED_SOUND = Registration.sound(CreeblossomIds.Sounds.ENTITY_ELECTRIC_CREEBLOSSOM_PRIMED);
		ELECTRIC_ATTACK_SOUND = Registration.sound(CreeblossomIds.Sounds.ENTITY_ELECTRIC_CREEBLOSSOM_ATTACK);
		ELECTRIC_EXPLODE_SOUND = Registration.sound(CreeblossomIds.Sounds.ENTITY_ELECTRIC_CREEBLOSSOM_EXPLODE);

		BLOSSOM_PARTICLE = Particles.simple(CreeblossomIds.Particles.BLOSSOM_PARTICLE, true);
		BLOSSOM_EXPLOSION = Particles.simple(CreeblossomIds.Particles.BLOSSOM_EXPLOSION, false);
		RECRUITED_LEAF = Particles.simple(CreeblossomIds.Particles.RECRUITED_LEAF, false);

		BLOSSOMING = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, CreeblossomIds.Effects.BLOSSOMING, new BlossomingEffect());

		// A monster in the original; it has no natural spawns and only grows from bushes and infected mobs.
		CREEBLOSSOM = Registration.livingEntity(CreeblossomIds.Entities.CREEBLOSSOM,
			EntityType.Builder.of(CreeblossomEntity::new, MobCategory.MONSTER).sized(0.6F, 1.0F).clientTrackingRange(8).updateInterval(3),
			CreeblossomEntity.createAttributes());
		Registration.spawnEgg(CreeblossomIds.Items.CREEBLOSSOM_SPAWN_EGG, CREEBLOSSOM);
		blossombushSeed = Registration.item(CreeblossomIds.Items.BLOSSOMBUSH_SEED, new Item.Properties());

		registerBlocks();

		ServerEntityEvents.ENTITY_LOAD.register(CreeblossomModule::maybeInfect);
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> !burstIfBlossoming(entity, source.getEntity()));
		UseBlockCallback.EVENT.register(CreeblossomModule::potBush);
	}

	private static void registerBlocks() {
		blossombush = Registration.blockWithItem(CreeblossomIds.Blocks.BLOSSOMBUSH, props -> new BlossombushBlock(props, false, true), bushProperties(true));
		closedBlossombush = Registration.blockWithItem(CreeblossomIds.Blocks.CLOSED_BLOSSOMBUSH, props -> new BlossombushBlock(props, false, false), bushProperties(false));
		electricBlossombush = Registration.blockWithItem(CreeblossomIds.Blocks.ELECTRIC_BLOSSOMBUSH, props -> new BlossombushBlock(props, true, true), bushProperties(true));
		closedElectricBlossombush = Registration.blockWithItem(CreeblossomIds.Blocks.CLOSED_ELECTRIC_BLOSSOMBUSH, props -> new BlossombushBlock(props, true, false), bushProperties(false));
		potBlossombush = Registration.blockWithItem(CreeblossomIds.Blocks.POT_BLOSSOMBUSH, props -> new PottedBlossombushBlock(props, () -> blossombush), potProperties());
		potElectricBlossombush = Registration.blockWithItem(CreeblossomIds.Blocks.POT_ELECTRIC_BLOSSOMBUSH, props -> new PottedBlossombushBlock(props, () -> electricBlossombush), potProperties());
	}

	/** Open bushes glow; closed ones do not. */
	private static BlockBehaviour.Properties bushProperties(boolean open) {
		BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
			.sound(SoundType.GRASS)
			.instabreak()
			.noCollision()
			.noOcclusion()
			.isRedstoneConductor((state, level, pos) -> false);
		return open ? properties.emissiveRendering(state -> true) : properties;
	}

	private static BlockBehaviour.Properties potProperties() {
		return BlockBehaviour.Properties.of()
			.sound(SoundType.STONE)
			.instabreak()
			.noOcclusion()
			.emissiveRendering(state -> true)
			.isRedstoneConductor((state, level, pos) -> false);
	}

	/** Undead and arthropod monsters (not creepers) in dry biomes sometimes arrive blossoming, for good. */
	private static void maybeInfect(Entity entity, ServerLevel level) {
		if (!(entity instanceof Monster monster) || entity instanceof Creeper) {
			return;
		}
		if (!monster.is(EntityTypeTags.UNDEAD) && !monster.is(EntityTypeTags.ARTHROPOD)) {
			return;
		}
		if (monster.hasEffect(BLOSSOMING) || INFECTION_BIOMES.stream().noneMatch(level.getBiome(monster.blockPosition())::is)) {
			return;
		}
		if (Mth.nextInt(monster.getRandom(), 1, Config.integer("creeblossom_infection_rate", 30)) == 1) {
			monster.addEffect(new MobEffectInstance(BLOSSOMING, MobEffectInstance.INFINITE_DURATION, 0, false, false));
		}
	}

	/**
	 * A blossoming mob killed by something bursts in a puff of petals into four creeblossoms
	 * instead of dying normally. Returns whether it burst (and so must not die).
	 */
	private static boolean burstIfBlossoming(LivingEntity entity, Entity killer) {
		if (killer == null || !entity.hasEffect(BLOSSOMING) || !(entity.level() instanceof ServerLevel level)) {
			return false;
		}
		Advancements.award(killer, MoreCritters.id("kill_infected_mob"));
		Vec3 at = entity.position();
		level.sendParticles(BLOSSOM_EXPLOSION, at.x, at.y, at.z, BURST_PARTICLES, 1.0, 1.0, 1.0, 0.0);
		level.playSound(null, entity.blockPosition(), BLOSSOMING_EXPLOSION_SOUND, SoundSource.BLOCKS, 1.0F, 1.0F);
		entity.discard();

		BlockPos above = BlockPos.containing(at.x, at.y + 1.0, at.z);
		for (Vec3 velocity : List.of(
				new Vec3(0.0, BURST_LIFT, BURST_SPEED), new Vec3(0.0, BURST_LIFT, -BURST_SPEED),
				new Vec3(BURST_SPEED, BURST_LIFT, 0.0), new Vec3(-BURST_SPEED, BURST_LIFT, 0.0))) {
			CreeblossomEntity sprout = CREEBLOSSOM.spawn(level, above, EntitySpawnReason.MOB_SUMMONED);
			if (sprout != null) {
				sprout.setDeltaMovement(velocity);
			}
		}
		return true;
	}

	/** A blossombush used on an empty flower pot pots it; closed bushes pot as open ones. */
	private static InteractionResult potBush(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		BlockPos pos = hit.getBlockPos();
		if (hand != InteractionHand.MAIN_HAND || !level.getBlockState(pos).is(Blocks.FLOWER_POT)) {
			return InteractionResult.PASS;
		}
		ItemStack held = player.getMainHandItem();
		Block potted;
		if (held.is(blossombush.asItem()) || held.is(closedBlossombush.asItem())) {
			potted = potBlossombush;
		} else if (held.is(electricBlossombush.asItem()) || held.is(closedElectricBlossombush.asItem())) {
			potted = potElectricBlossombush;
		} else {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			player.swing(hand, SwingAnimation.DEFAULT, true);
			level.setBlock(pos, potted.defaultBlockState(), Block.UPDATE_ALL);
			if (!player.hasInfiniteMaterials()) {
				held.shrink(1);
			}
		}
		return InteractionResult.SUCCESS;
	}
}
