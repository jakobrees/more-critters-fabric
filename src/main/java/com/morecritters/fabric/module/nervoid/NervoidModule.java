package com.morecritters.fabric.module.nervoid;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Config;
import com.morecritters.fabric.core.Particles;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Sounds;
import com.morecritters.fabric.ids.CustodianIds;
import com.morecritters.fabric.ids.NervoidIds;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;

/**
 * The nervoid: a flying End brain that hatches from Endfected endermen, rushes players and
 * possesses what it bites. Also its brains (which lure the undead and rot unless iced), the
 * nerve foods, spinal fluid, Endfected and Brain Scented.
 */
public final class NervoidModule implements Module {
	/** Endermen are infected with a one in this many chance (the original's {@code nervoid_infection_rate}). */
	private static final double DEFAULT_INFECTION_RATE = 50.0;
	private static final int BRAIN_SCENT_DURATION = 3600;

	public static EntityType<NervoidEntity> NERVOID;
	public static Holder<MobEffect> BRAIN_SCENTED, ENDFECTED;
	public static SimpleParticleType END_EXPLOSION, SPINAL_FLUID, STINK, ICE_ON, ICE_OFF;

	static SoundEvent BRAIN_BREAKING, IDLE_SOUND, HURT_SOUND, DEATH_SOUND, ATTACK_SOUND,
		RUSH_READY_SOUND, RUSH_START_SOUND, POSSESS_SOUND, UNPOSSESS_SOUND;
	static Item spinalFluidBottle;

	@Override
	public void register() {
		registerSounds();

		END_EXPLOSION = Particles.simple(NervoidIds.Particles.END_EXPLOSION, true);
		SPINAL_FLUID = Particles.simple(NervoidIds.Particles.SPINAL_FLUID, true);
		STINK = Particles.simple(NervoidIds.Particles.STINK, true);
		ICE_ON = Particles.simple(NervoidIds.Particles.ICE_ON, true);
		ICE_OFF = Particles.simple(NervoidIds.Particles.ICE_OFF, true);

		BRAIN_SCENTED = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, NervoidIds.Effects.BRAIN_SCENTED, new BrainScentedEffect());
		ENDFECTED = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, NervoidIds.Effects.ENDFECTED, new EndfectedEffect());
		Registry.register(BuiltInRegistries.POTION, NervoidIds.Potions.BRAIN_SCENT,
			new Potion(NervoidIds.Potions.BRAIN_SCENT.getPath(), new MobEffectInstance(BRAIN_SCENTED, BRAIN_SCENT_DURATION, 0, false, true)));

		NERVOID = Registration.livingEntity(NervoidIds.Entities.NERVOID,
			EntityType.Builder.of(NervoidEntity::new, MobCategory.MONSTER).sized(0.6F, 0.6F).clientTrackingRange(8).updateInterval(3),
			NervoidEntity.createAttributes());
		Registration.spawnEgg(NervoidIds.Items.NERVOID_SPAWN_EGG, NERVOID);

		registerItems();
		NervoidBrainBlock.registerAll(brainSoundType());

		ServerEntityEvents.ENTITY_LOAD.register(NervoidModule::maybeInfectEnderman);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			burstEndfected(entity);
			releaseNervoid(entity);
		});
	}

	private static void registerSounds() {
		BRAIN_BREAKING = Registration.sound(NervoidIds.Sounds.BLOCK_BRAIN_BREAKING);
		IDLE_SOUND = Registration.sound(NervoidIds.Sounds.ENTITY_NERVOID_IDLE);
		HURT_SOUND = Registration.sound(NervoidIds.Sounds.ENTITY_NERVOID_HURT);
		DEATH_SOUND = Registration.sound(NervoidIds.Sounds.ENTITY_NERVOID_DEATH);
		ATTACK_SOUND = Registration.sound(NervoidIds.Sounds.ENTITY_NERVOID_ATTACK);
		RUSH_READY_SOUND = Registration.sound(NervoidIds.Sounds.ENTITY_NERVOID_RUSH_READY);
		RUSH_START_SOUND = Registration.sound(NervoidIds.Sounds.ENTITY_NERVOID_RUSH_START);
		POSSESS_SOUND = Registration.sound(NervoidIds.Sounds.ENTITY_NERVOID_POSSESS);
		UNPOSSESS_SOUND = Registration.sound(NervoidIds.Sounds.ENTITY_NERVOID_UNPOSSESS);
	}

	/** Brains squelch: their own break, step, place, hit and fall sounds. */
	private static SoundType brainSoundType() {
		return new SoundType(1.0F, 1.0F,
			Registration.sound(NervoidIds.Sounds.BLOCK_BRAIN_BREAK),
			Registration.sound(NervoidIds.Sounds.BLOCK_BRAIN_FOOTSTEPS),
			Registration.sound(NervoidIds.Sounds.BLOCK_BRAIN_PLACE),
			BRAIN_BREAKING,
			Registration.sound(NervoidIds.Sounds.BLOCK_BRAIN_FALL));
	}

	private static void registerItems() {
		spinalFluidBottle = Registration.item(NervoidIds.Items.SPINAL_FLUID_BOTTLE, new Item.Properties().stacksTo(16));
		Registration.item(NervoidIds.Items.LOST_NERVE, new Item.Properties().food(food(4, 0.2F)));
		Registration.item(NervoidIds.Items.COOKED_NERVE, new Item.Properties().food(food(6, 0.5F)));
		Registration.item(NervoidIds.Items.NERVAL_SALAD, new Item.Properties().stacksTo(1).food(food(8, 0.8F)).usingConvertsTo(Items.BOWL));
		Registration.item(NervoidIds.Items.POPPED_NERVAL_MIXTURE, new Item.Properties().stacksTo(1).food(food(10, 0.8F)).usingConvertsTo(Items.BOWL));
	}

	private static FoodProperties food(int nutrition, float saturation) {
		return new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build();
	}

	/** An enderman appearing in the End carries a nervoid inside it, one time in {@code nervoid_infection_rate}. */
	private static void maybeInfectEnderman(Entity entity, ServerLevel level) {
		if (!(entity instanceof LivingEntity enderman) || entity.getType() != EntityTypes.ENDERMAN || level.dimension() != Level.END || enderman.hasEffect(ENDFECTED)) {
			return;
		}
		int rate = Math.max(1, (int) Config.number("nervoid_infection_rate", DEFAULT_INFECTION_RATE));
		if (Mth.nextInt(level.getRandom(), 1, rate) == 1) {
			enderman.addEffect(new MobEffectInstance(ENDFECTED, Integer.MAX_VALUE, 0, false, true));
		}
	}

	/**
	 * An Endfected creature bursts on death and a nervoid flies out of it. It comes out dry, so it is
	 * spawned as an event rather than MOB_SUMMONED (which makes a nervoid wet).
	 */
	private static void burstEndfected(LivingEntity entity) {
		if (!entity.hasEffect(ENDFECTED) || !(entity.level() instanceof ServerLevel level)) {
			return;
		}
		Particles.spawnAt(entity, END_EXPLOSION, 7, 1.0, 1.0, 1.0, 0.0);
		Sounds.playAt(entity, UNPOSSESS_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
		NervoidEntity nervoid = NERVOID.spawn(level, BlockPos.containing(entity.getX(), entity.getY() + 1.0, entity.getZ()), EntitySpawnReason.EVENT);
		if (nervoid != null) {
			nervoid.setDeltaMovement(0.0, 0.5, 0.0);
		}
	}

	/**
	 * A creature a nervoid possessed (Under Control) lets it out again, dripping wet (MOB_SUMMONED),
	 * when it dies. Effects are cleared silently on death, so the custodian module's release on the
	 * effect ending does not fire as well.
	 */
	private static void releaseNervoid(LivingEntity entity) {
		Holder<MobEffect> underControl = underControl();
		if (underControl == null || !entity.hasEffect(underControl) || !(entity.level() instanceof ServerLevel level)) {
			return;
		}
		NervoidEntity nervoid = NERVOID.spawn(level, BlockPos.containing(entity.getX() + 0.5, entity.getY() + 1.0, entity.getZ() + 0.5), EntitySpawnReason.MOB_SUMMONED);
		if (nervoid != null) {
			nervoid.setDeltaMovement(0.2, 0.5, 0.0);
		}
		Sounds.playAt(entity, UNPOSSESS_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
	}

	/** The custodian module's Under Control effect, or null while that module is not loaded. */
	static Holder<MobEffect> underControl() {
		return BuiltInRegistries.MOB_EFFECT.get(CustodianIds.Effects.UNDER_CONTROL).<Holder<MobEffect>>map(holder -> holder).orElse(null);
	}
}
