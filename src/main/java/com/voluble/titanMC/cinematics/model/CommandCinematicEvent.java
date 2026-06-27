package com.voluble.titanMC.cinematics.model;

import java.util.Objects;

public record CommandCinematicEvent(
	int tick,
	int timelineSlot,
	int row,
	String command,
	boolean console
) implements CinematicEvent {
	public CommandCinematicEvent {
		validatePlacement(tick, timelineSlot, row);
		command = Objects.requireNonNull(command, "command").trim();
		if (command.isBlank()) throw new IllegalArgumentException("cinematic command must not be blank");
	}

	@Override
	public CinematicEventType type() {
		return CinematicEventType.COMMAND;
	}

	static void validatePlacement(int tick, int timelineSlot, int row) {
		if (tick < 0) throw new IllegalArgumentException("cinematic event tick must not be negative");
		if (timelineSlot < 0) throw new IllegalArgumentException("cinematic event timeline slot must not be negative");
		if (row < 1) throw new IllegalArgumentException("cinematic event row must be at least 1");
	}
}
