package com.morecritters.fabric.module.wandering_collector;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.morecritters.fabric.core.Animations;
import com.morecritters.fabric.core.TextureVariants;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The wandering collector: a merchant found in his camps who strolls about, never despawns, and
 * buys critter drops. Right-clicking him opens his trade screen ({@link WanderingCollectorMenu}).
 * He dresses for the biome he first appears in, and huffs angrily when hit.
 */
public class WanderingCollectorEntity extends PathfinderMob implements GeoEntity, TextureVariants {
	private static final EntityDataAccessor<String> OUTFIT = SynchedEntityData.defineId(WanderingCollectorEntity.class, EntityDataSerializers.STRING);
	private static final String DEFAULT_OUTFIT = "plains";
	/** Biome → outfit; any biome not listed gets the plains outfit. */
	private static final Map<ResourceKey<Biome>, String> OUTFITS = new HashMap<>();

	static {
		outfit("savanna", Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA);
		outfit("desert", Biomes.DESERT);
		outfit("badlands", Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS);
		outfit("tundra", Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA);
		outfit("swamp", Biomes.SWAMP, Biomes.MANGROVE_SWAMP);
		outfit("jungle", Biomes.JUNGLE, Biomes.BAMBOO_JUNGLE, Biomes.SPARSE_JUNGLE);
		outfit("ocean", Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.FROZEN_OCEAN,
			Biomes.DEEP_FROZEN_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.WARM_OCEAN);
		outfit("taiga", Biomes.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA);
		outfit("cave", Biomes.DRIPSTONE_CAVES, Biomes.LUSH_CAVES);
	}

	@SafeVarargs
	private static void outfit(String name, ResourceKey<Biome>... biomes) {
		for (ResourceKey<Biome> biome : Set.of(biomes)) {
			OUTFITS.put(biome, name);
		}
	}

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);

	public WanderingCollectorEntity(EntityType<? extends WanderingCollectorEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			.add(Attributes.MAX_HEALTH, 20.0)
			.add(Attributes.ARMOR, 0.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.STEP_HEIGHT, 0.6)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(OUTFIT, DEFAULT_OUTFIT);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new RandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(3, new PanicGoal(this, 1.0));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(5, new FloatGoal(this));
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	// --- outfit -----------------------------------------------------------------------

	/** Textures are named {@code wandering_collector_<biome outfit>}. */
	@Override
	public String textureName() {
		return "wandering_collector_" + this.entityData.get(OUTFIT);
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		String outfit = level.getBiome(this.blockPosition()).unwrapKey().map(OUTFITS::get).orElse(null);
		this.entityData.set(OUTFIT, outfit == null ? DEFAULT_OUTFIT : outfit);
		return data;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("Outfit", this.entityData.get(OUTFIT));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.entityData.set(OUTFIT, input.getStringOr("Outfit", DEFAULT_OUTFIT));
	}

	// --- trading ----------------------------------------------------------------------

	/** Opens the trade screen and turns to look the customer in the eye. */
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		super.mobInteract(player, hand);
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.openMenu(new SimpleMenuProvider(
				(id, inventory, opener) -> new WanderingCollectorMenu(id, inventory, this), this.getDisplayName()));
		}
		this.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(player.getX(), player.getY() + player.getBbHeight(), player.getZ()));
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		level.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getY() + 1.0, this.getZ(), 5, 0.2, 0.2, 0.2, 1.0);
		return super.hurtServer(level, source, damage);
	}

	// --- sounds -----------------------------------------------------------------------

	@Override
	protected SoundEvent getAmbientSound() {
		return WanderingCollectorModule.COLLECTOR_IDLE_SOUND;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return WanderingCollectorModule.COLLECTOR_HURT_SOUND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return WanderingCollectorModule.COLLECTOR_DEATH_SOUND;
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
