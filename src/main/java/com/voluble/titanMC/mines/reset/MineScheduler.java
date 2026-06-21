package com.voluble.titanMC.mines.reset;

import com.voluble.titanMC.mines.Mine;
import com.voluble.titanMC.mines.MineManager;
import com.voluble.titanMC.util.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class MineScheduler {

	private static final long RESET_BUDGET_NANOS = 4_000_000L;
	private final Plugin plugin;
	private final MineManager manager;
	private final MineResetQueue resetQueue = new MineResetQueue();
	private final Map<String, Long> resetTimes = new HashMap<>(); // Track when depletion resets are triggered
	private BukkitTask task;
	private static final int COUNTDOWN_WARNING_RADIUS = 20; // blocks

	public MineScheduler(Plugin plugin, MineManager manager) {
		this.plugin = plugin;
		this.manager = manager;
	}

	public void start() {
		if (task != null) return;
		task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
			long now = System.currentTimeMillis();
			// Check for time-based resets and show countdowns
			for (Mine mine : manager.getAll()) {
				if (!mine.isEnabled()) continue;
				if (resetQueue.contains(mine.getName())) continue;
				
				long remainingMs = mine.getNextResetEpochMs() - now;
				long remainingSeconds = remainingMs / 1000L;
				
				// Show countdown for time-based resets
				if (remainingSeconds >= 0 && remainingSeconds <= 10) {
					broadcastCountdown(mine, remainingSeconds, "Time");
				}
				
				if (now >= mine.getNextResetEpochMs()) {
					resetQueue.replace(new MineResetRunner(plugin, mine));
				}
			}
			resetQueue.processTick(RESET_BUDGET_NANOS, manager::completeReset);
			
			// Also tick depletion countdowns
			tickDepletionCountdown();
		}, 1L, 1L);
	}

	public void stop() {
		if (task != null) {
			task.cancel();
			task = null;
		}
		resetQueue.clear();
		resetTimes.clear();
	}

	public boolean forceReset(String name) {
		Mine mine = manager.get(name);
		if (mine == null) return false;
		cancelReset(name);
		resetQueue.replace(new MineResetRunner(plugin, mine));
		return true;
	}

	public void scheduleDepletionReset(String name) {
		// Don't schedule if already resetting or already scheduled
		if (resetQueue.contains(name) || resetTimes.containsKey(name)) return;
		// Store when this depletion reset was triggered for countdown
		resetTimes.put(name, System.currentTimeMillis() + 10000); // 10 seconds from now
	}

	public void cancelReset(String name) {
		resetQueue.cancel(name);
		resetTimes.remove(name);
	}

	private void broadcastCountdown(Mine mine, long seconds, String reason) {
		World world = Bukkit.getWorld(mine.getCuboid().worldId);
		if (world == null) return;

		String color = seconds <= 3 ? "<red><bold>" : seconds <= 6 ? "<yellow><bold>" : "<gold>";
		String message = color + "Resetting mine " + mine.getName() + " in " + seconds + " seconds";

		for (Player player : world.getPlayers()) {
			double distance = mine.getCuboid().distanceTo(player.getLocation());
			if (distance <= COUNTDOWN_WARNING_RADIUS) {
				ChatUtils.sendActionBar(player, message);
			}
		}
	}

	public void tickDepletionCountdown() {
		// Check for depletion countdowns
		Iterator<Map.Entry<String, Long>> it = resetTimes.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Long> entry = it.next();
			String mineName = entry.getKey();
			long resetTime = entry.getValue();

			// Only show if not already resetting
			if (resetQueue.contains(mineName)) {
				it.remove();
				continue;
			}

			long now = System.currentTimeMillis();
			long remainingMs = resetTime - now;
			long remainingSeconds = remainingMs / 1000L;

			if (remainingSeconds <= 0) {
				// Time's up! Trigger the actual reset
				it.remove();
				Mine mine = manager.get(mineName);
				if (mine != null) {
					resetQueue.replace(new MineResetRunner(plugin, mine));
				}
				continue;
			}

			if (remainingSeconds <= 10) {
				Mine mine = manager.get(mineName);
				if (mine != null) {
					broadcastCountdown(mine, remainingSeconds, "Depleted");
				}
			}
		}
	}
}


