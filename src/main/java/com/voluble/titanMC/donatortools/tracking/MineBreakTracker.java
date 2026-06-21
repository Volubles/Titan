package com.voluble.titanMC.donatortools.tracking;

import com.voluble.titanMC.mines.Mine;
import com.voluble.titanMC.mines.MineManager;
import com.voluble.titanMC.mines.reset.MineScheduler;
import org.bukkit.Location;

import java.util.Objects;

public final class MineBreakTracker implements BlockBreakTracker {

	private final MineManager mines;
	private final MineScheduler scheduler;

	public MineBreakTracker(MineManager mines, MineScheduler scheduler) {
		this.mines = Objects.requireNonNull(mines, "mines");
		this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
	}

	@Override
	public void record(Location location) {
		Mine mine = mines.getFirstAt(Objects.requireNonNull(location, "location"));
		if (mine == null) return;
		mine.incrementBroken(1);
		if (mine.isEnabled() && mine.shouldAutoResetByDepletion()) {
			scheduler.scheduleDepletionReset(mine.getName());
		}
	}
}
