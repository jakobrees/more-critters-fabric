package com.morecritters.fabric.core;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Runs work on the server thread a number of ticks from now; the original mod's {@code queueServerWork}. */
public final class ServerScheduler {
	private static final List<Pending> PENDING = new ArrayList<>();

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(ServerScheduler::tick);
	}

	public static void runLater(int ticks, Runnable work) {
		PENDING.add(new Pending(ticks, work));
	}

	private static void tick(MinecraftServer server) {
		// Work may schedule more work, so iterate over a snapshot.
		for (Iterator<Pending> it = new ArrayList<>(PENDING).iterator(); it.hasNext(); ) {
			Pending pending = it.next();
			if (--pending.ticksLeft <= 0) {
				PENDING.remove(pending);
				pending.work.run();
			}
		}
	}

	private static final class Pending {
		int ticksLeft;
		final Runnable work;

		Pending(int ticksLeft, Runnable work) {
			this.ticksLeft = ticksLeft;
			this.work = work;
		}
	}

	private ServerScheduler() {}
}
