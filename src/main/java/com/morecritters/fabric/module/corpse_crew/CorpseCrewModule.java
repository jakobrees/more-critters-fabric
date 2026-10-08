package com.morecritters.fabric.module.corpse_crew;

import com.morecritters.fabric.Module;
import com.morecritters.fabric.core.Registration;
import com.morecritters.fabric.core.Tooltips;
import com.morecritters.fabric.ids.CorpseCrewIds;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The corpse crew: the undead pirates that man the ghost ships (mate, quartermaster, tank,
 * captain, lookout and parrot), the spawner that musters a crew, the tamed parrot, the pebbles and
 * spit they fling, their spawn dolls, the captain's heart, tattered cloth and the corpse barnacle.
 * The crew comes aboard with the ship structures, which are data; there are no natural spawns.
 */
public final class CorpseCrewModule implements Module {
	public static EntityType<CorpseMateEntity> CORPSE_MATE;
	public static EntityType<CorpseQuartermasterEntity> CORPSE_QUARTERMASTER;
	public static EntityType<CorpseTankEntity> CORPSE_TANK;
	public static EntityType<CorpseCaptainEntity> CORPSE_CAPTAIN;
	public static EntityType<CorpseLookoutEntity> CORPSE_LOOKOUT;
	public static EntityType<CorpseParrotEntity> CORPSE_PARROT;
	public static EntityType<TamedCorpseParrotEntity> TAMED_CORPSE_PARROT;
	public static EntityType<CorpseCrewEntity> CORPSE_CREW;
	public static EntityType<PebbleEntity> PEBBLE;
	public static EntityType<LookoutSpitEntity> LOOKOUT_SPIT;
	public static CorpseBarnacleBlock CORPSE_BARNACLE;

	static SoundEvent CAPTAIN_ALERT, CAPTAIN_ATTACK, CAPTAIN_DEATH, CAPTAIN_HEAL, CAPTAIN_HURT, CAPTAIN_IDLE, CAPTAIN_SING, CAPTAIN_SPEECH;
	static SoundEvent LOOKOUT_DEATH, LOOKOUT_HURT, LOOKOUT_IDLE, LOOKOUT_SPIT_SOUND, LOOKOUT_SPIT_HITS;
	static SoundEvent MATE_ATTACK, MATE_DEATH, MATE_HURT, MATE_IDLE, MATE_MISS, MATE_READY, MATE_SING, MATE_SPEECH;
	static SoundEvent PARROT_ATTACK, PARROT_DEATH, PARROT_HURT, PARROT_IDLE, PARROT_SING, PARROT_SPEECH, PARROT_THROW;
	static SoundEvent QUARTERMASTER_BITE, QUARTERMASTER_DEATH, QUARTERMASTER_HURT, QUARTERMASTER_IDLE, QUARTERMASTER_SING,
		QUARTERMASTER_SPEECH, QUARTERMASTER_THROW;
	static SoundEvent TANK_ATTACK, TANK_DEATH, TANK_HURT, TANK_IDLE, TANK_READY, TANK_SHOOT, TANK_SING, TANK_SPEECH;
	static SoundEvent TAMED_PARROT_DEATH, TAMED_PARROT_HURT, TAMED_PARROT_IDLE;

	@Override
	public void register() {
		registerSounds();
		registerEntities();
		registerItems();

		CORPSE_BARNACLE = Registration.blockWithItem(CorpseCrewIds.Blocks.CORPSE_BARNACLE, CorpseBarnacleBlock::new,
			BlockBehaviour.Properties.of().sound(SoundType.STEM).strength(1.0F).requiresCorrectToolForDrops().noOcclusion()
				.isRedstoneConductor((state, level, pos) -> false));

		ServerLivingEntityEvents.ALLOW_DAMAGE.register(CorpseCrewModule::playAttackSound);
	}

	private static void registerEntities() {
		CORPSE_MATE = Registration.livingEntity(CorpseCrewIds.Entities.CORPSE_MATE,
			EntityType.Builder.of(CorpseMateEntity::new, MobCategory.MONSTER).sized(0.6F, 2.5F).clientTrackingRange(4).updateInterval(3),
			CorpseMateEntity.createAttributes());
		CORPSE_QUARTERMASTER = Registration.livingEntity(CorpseCrewIds.Entities.CORPSE_QUARTERMASTER,
			EntityType.Builder.of(CorpseQuartermasterEntity::new, MobCategory.MONSTER).sized(0.9F, 2.0F).clientTrackingRange(4).updateInterval(3),
			CorpseQuartermasterEntity.createAttributes());
		CORPSE_TANK = Registration.livingEntity(CorpseCrewIds.Entities.CORPSE_TANK,
			EntityType.Builder.of(CorpseTankEntity::new, MobCategory.MONSTER).sized(1.2F, 2.0F).clientTrackingRange(4).updateInterval(3),
			CorpseTankEntity.createAttributes());
		CORPSE_CAPTAIN = Registration.livingEntity(CorpseCrewIds.Entities.CORPSE_CAPTAIN,
			EntityType.Builder.of(CorpseCaptainEntity::new, MobCategory.MONSTER).sized(1.0F, 1.8F).clientTrackingRange(4).updateInterval(3),
			CorpseCaptainEntity.createAttributes());
		CORPSE_LOOKOUT = Registration.livingEntity(CorpseCrewIds.Entities.CORPSE_LOOKOUT,
			EntityType.Builder.of(CorpseLookoutEntity::new, MobCategory.MONSTER).sized(0.6F, 3.0F).clientTrackingRange(15).updateInterval(3),
			CorpseLookoutEntity.createAttributes());
		CORPSE_PARROT = Registration.livingEntity(CorpseCrewIds.Entities.CORPSE_PARROT,
			EntityType.Builder.of(CorpseParrotEntity::new, MobCategory.MONSTER).sized(0.6F, 0.7F).clientTrackingRange(8).updateInterval(3),
			CorpseParrotEntity.createAttributes());
		TAMED_CORPSE_PARROT = Registration.livingEntity(CorpseCrewIds.Entities.TAMED_CORPSE_PARROT,
			EntityType.Builder.of(TamedCorpseParrotEntity::new, MobCategory.MONSTER).sized(0.6F, 0.7F).clientTrackingRange(8).updateInterval(3),
			TamedCorpseParrotEntity.createAttributes());
		CORPSE_CREW = Registration.livingEntity(CorpseCrewIds.Entities.CORPSE_CREW,
			EntityType.Builder.of(CorpseCrewEntity::new, MobCategory.MONSTER).sized(1.0F, 1.0F).clientTrackingRange(8).updateInterval(3),
			CorpseCrewEntity.createAttributes());

		PEBBLE = Registration.entity(CorpseCrewIds.Entities.PEBBLE,
			EntityType.Builder.<PebbleEntity>of(PebbleEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(1));
		LOOKOUT_SPIT = Registration.entity(CorpseCrewIds.Entities.LOOKOUT_SPIT,
			EntityType.Builder.<LookoutSpitEntity>of(LookoutSpitEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(1));
	}

	private static void registerItems() {
		Registration.item(CorpseCrewIds.Items.CAPTAINS_HEART, new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
		Registration.item(CorpseCrewIds.Items.TATTERED_CLOTH);
		Registration.item(CorpseCrewIds.Items.CORPSE_PARROT_ITEM, CorpseParrotItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
		Registration.spawnEgg(CorpseCrewIds.Items.CORPSE_CREW_SPAWN_EGG, CORPSE_CREW);

		spawnDoll(CorpseCrewIds.Items.CORPSE_MATE_SPAWN_DOLL, "corpse_mate_spawn_doll", () -> CORPSE_MATE);
		spawnDoll(CorpseCrewIds.Items.CORPSE_QUARTERMASTER_SPAWN_DOLL, "corpse_quartermaster_spawn_doll", () -> CORPSE_QUARTERMASTER);
		spawnDoll(CorpseCrewIds.Items.CORPSE_TANK_SPAWN_DOLL, "corpse_tank_spawn_doll", () -> CORPSE_TANK);
		spawnDoll(CorpseCrewIds.Items.CORPSE_CAPTAIN_SPAWN_DOLL, "corpse_captain_spawn_doll", () -> CORPSE_CAPTAIN);
		spawnDoll(CorpseCrewIds.Items.CORPSE_PARROT_SPAWN_DOLL, "corpse_parrot_spawn_doll", () -> CORPSE_PARROT);
		spawnDoll(CorpseCrewIds.Items.CORPSE_LOOKOUT_SPAWN_DOLL, "corpse_lookout_spawn_doll", () -> CORPSE_LOOKOUT);
	}

	private static void spawnDoll(Identifier id, String name, Supplier<EntityType<?>> type) {
		Registration.item(id, properties -> new CrewSpawnDollItem(type, properties), Tooltips.describe(new Item.Properties(), name, 3));
	}

	/**
	 * Whenever a crew member (or the tamed parrot) hurts something, by hand or by projectile, its
	 * attack sound plays where the victim stands.
	 */
	private static boolean playAttackSound(LivingEntity victim, DamageSource source, float amount) {
		SoundEvent sound = null;
		SoundSource category = SoundSource.HOSTILE;
		if (source.getEntity() instanceof CorpseCrewMember member) {
			sound = member.attackSound();
		} else if (source.getEntity() instanceof TamedCorpseParrotEntity) {
			sound = PARROT_ATTACK;
			category = SoundSource.NEUTRAL;
		}
		if (sound != null) {
			victim.level().playSound(null, BlockPos.containing(victim.position()), sound, category, 1.0F, 1.0F);
		}
		return true;
	}

	private static void registerSounds() {
		CAPTAIN_ALERT = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_CAPTAIN_ALERT);
		CAPTAIN_ATTACK = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_CAPTAIN_ATTACK);
		CAPTAIN_DEATH = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_CAPTAIN_DEATH);
		CAPTAIN_HEAL = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_CAPTAIN_HEAL);
		CAPTAIN_HURT = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_CAPTAIN_HURT);
		CAPTAIN_IDLE = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_CAPTAIN_IDLE);
		CAPTAIN_SING = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_CAPTAIN_SING);
		CAPTAIN_SPEECH = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_CAPTAIN_SPEECH);
		LOOKOUT_DEATH = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_LOOKOUT_DEATH);
		LOOKOUT_HURT = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_LOOKOUT_HURT);
		LOOKOUT_IDLE = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_LOOKOUT_IDLE);
		LOOKOUT_SPIT_SOUND = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_LOOKOUT_SPIT);
		LOOKOUT_SPIT_HITS = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_LOOKOUT_SPIT_HITS);
		MATE_ATTACK = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_MATE_ATTACK);
		MATE_DEATH = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_MATE_DEATH);
		MATE_HURT = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_MATE_HURT);
		MATE_IDLE = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_MATE_IDLE);
		MATE_MISS = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_MATE_MISS);
		MATE_READY = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_MATE_READY);
		MATE_SING = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_MATE_SING);
		MATE_SPEECH = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_MATE_SPEECH);
		PARROT_ATTACK = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_PARROT_ATTACK);
		PARROT_DEATH = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_PARROT_DEATH);
		PARROT_HURT = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_PARROT_HURT);
		PARROT_IDLE = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_PARROT_IDLE);
		PARROT_SING = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_PARROT_SING);
		PARROT_SPEECH = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_PARROT_SPEECH);
		PARROT_THROW = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_PARROT_THROW);
		QUARTERMASTER_BITE = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_QUARTERMASTER_BITE);
		QUARTERMASTER_DEATH = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_QUARTERMASTER_DEATH);
		QUARTERMASTER_HURT = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_QUARTERMASTER_HURT);
		QUARTERMASTER_IDLE = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_QUARTERMASTER_IDLE);
		QUARTERMASTER_SING = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_QUARTERMASTER_SING);
		QUARTERMASTER_SPEECH = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_QUARTERMASTER_SPEECH);
		QUARTERMASTER_THROW = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_QUARTERMASTER_THROW);
		TANK_ATTACK = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_TANK_ATTACK);
		TANK_DEATH = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_TANK_DEATH);
		TANK_HURT = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_TANK_HURT);
		TANK_IDLE = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_TANK_IDLE);
		TANK_READY = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_TANK_READY);
		TANK_SHOOT = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_TANK_SHOOT);
		TANK_SING = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_TANK_SING);
		TANK_SPEECH = Registration.sound(CorpseCrewIds.Sounds.ENTITY_CORPSE_TANK_SPEECH);
		TAMED_PARROT_DEATH = Registration.sound(CorpseCrewIds.Sounds.ENTITY_TAMED_CORPSE_PARROT_DEATH);
		TAMED_PARROT_HURT = Registration.sound(CorpseCrewIds.Sounds.ENTITY_TAMED_CORPSE_PARROT_HURT);
		TAMED_PARROT_IDLE = Registration.sound(CorpseCrewIds.Sounds.ENTITY_TAMED_CORPSE_PARROT_IDLE);
	}
}
