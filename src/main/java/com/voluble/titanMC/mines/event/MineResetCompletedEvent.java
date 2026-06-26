package com.voluble.titanMC.mines.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class MineResetCompletedEvent extends Event {
	private static final HandlerList HANDLERS = new HandlerList();

	private final String mineName;
	private final long completedAtEpochMillis;

	public MineResetCompletedEvent(String mineName, long completedAtEpochMillis) {
		this.mineName = requireMineName(mineName);
		if (completedAtEpochMillis < 0L) throw new IllegalArgumentException("completedAtEpochMillis must not be negative");
		this.completedAtEpochMillis = completedAtEpochMillis;
	}

	public String mineName() {
		return mineName;
	}

	public long completedAtEpochMillis() {
		return completedAtEpochMillis;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	private static String requireMineName(String value) {
		String normalized = Objects.requireNonNull(value, "mineName").trim();
		if (normalized.isBlank()) throw new IllegalArgumentException("mineName must not be blank");
		return normalized;
	}
}
