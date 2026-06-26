package com.voluble.titanMC.milestones.tracking;

import com.voluble.titanMC.mines.event.MineResetCompletedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class MineResetWindowTracker implements Listener {
	private final Duration window;
	private final Map<String, Long> completedResets = new HashMap<>();

	public MineResetWindowTracker(Duration window) {
		this.window = Objects.requireNonNull(window, "window");
		if (window.isNegative() || window.isZero()) throw new IllegalArgumentException("window must be positive");
	}

	@EventHandler
	public void onMineResetCompleted(MineResetCompletedEvent event) {
		completedResets.put(normalize(event.mineName()), event.completedAtEpochMillis());
	}

	public boolean active(String mineName, long nowEpochMillis) {
		Long completedAt = completedResets.get(normalize(mineName));
		if (completedAt == null) return false;
		long age = nowEpochMillis - completedAt;
		return age >= 0L && age <= window.toMillis();
	}

	private static String normalize(String mineName) {
		return Objects.requireNonNull(mineName, "mineName").trim().toLowerCase(Locale.ROOT);
	}
}
