package com.morecritters.fabric.module.shriekbat;

import java.util.Comparator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A shriekbat's test shriek: an echo that sinks to the floor and fades over two seconds. If any player
 * within ten blocks who is not in creative or spectator and not shriek resistant moves while it lasts,
 * the nearest shriekbat is alarmed.
 */
public class TesterShriekEntity extends EchoEntity {
	private static final double LISTEN_REACH = 10.0, BAT_SEARCH = 100.0, ALARM_REACH = 50.0;

	private boolean heardSomething;

	public TesterShriekEntity(EntityType<? extends TesterShriekEntity> type, Level level) {
		super(type, level);
	}

	@Override
	protected void move(ServerLevel level) {
		this.setDeltaMovement(0.0, airBelow() ? -0.4 : 0.0, 0.0);
	}

	@Override
	protected void age(ServerLevel level) {
		if (this.lifeLeft == 30) setStage(1);
		if (this.lifeLeft == 20) setStage(2);
		if (this.lifeLeft == 10) setStage(3);
		Vec3 centre = this.position();
		ShriekbatEntity nearestBat = level.getEntitiesOfClass(ShriekbatEntity.class, AABB.ofSize(centre, BAT_SEARCH, BAT_SEARCH, BAT_SEARCH)).stream()
			.min(Comparator.comparingDouble(bat -> bat.distanceToSqr(centre))).orElse(null);
		if (nearestBat == null) return;
		if (!this.heardSomething) {
			this.heardSomething = !level.getEntitiesOfClass(Player.class, new AABB(centre, centre).inflate(LISTEN_REACH), this::canHear).isEmpty();
		}
		if (this.heardSomething && nearestBat.distanceToSqr(centre) <= ALARM_REACH * ALARM_REACH) nearestBat.alarm();
	}

	private boolean canHear(Player player) {
		return !player.isCreative() && !player.isSpectator()
			&& !player.hasEffect(ShriekbatModule.SHRIEK_RESISTANCE)
			&& player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
	}
}
