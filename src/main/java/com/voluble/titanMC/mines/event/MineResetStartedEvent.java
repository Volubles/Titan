package com.voluble.titanMC.mines.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class MineResetStartedEvent extends Event {
	private static final HandlerList HANDLERS = new HandlerList();

	private final String mineName;
	private final long startedAtEpochMillis;

	public MineResetStartedEvent(String mineName, long startedAtEpochMillis) {
		this.mineName = requireMineName(mineName);
		if (startedAtEpochMillis < 0L) throw new IllegalArgumentException("startedAtEpochMillis must not be negative");
		this.startedAtEpochMillis = startedAtEpochMillis;
	}

	public String mineName() {
		return mineName;
	}

	public long startedAtEpochMillis() {
		return startedAtEpochMillis;
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
