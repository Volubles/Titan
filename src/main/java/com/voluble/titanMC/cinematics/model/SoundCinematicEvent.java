package com.voluble.titanMC.cinematics.model;

import java.util.Objects;

public record SoundCinematicEvent(
	int tick,
	int timelineSlot,
	int row,
	CinematicEventPosition position,
	String key,
	float volume,
	float pitch,
	String category
) implements CinematicEvent {
	public SoundCinematicEvent {
		CommandCinematicEvent.validatePlacement(tick, timelineSlot, row);
		position = Objects.requireNonNull(position, "position");
		key = Objects.requireNonNull(key, "key").trim();
		if (key.isBlank()) throw new IllegalArgumentException("cinematic sound key must not be blank");
		if (volume < 0.0f) throw new IllegalArgumentException("cinematic sound volume must not be negative");
		if (pitch < 0.0f) throw new IllegalArgumentException("cinematic sound pitch must not be negative");
		category = Objects.requireNonNull(category, "category").trim();
		if (category.isBlank()) throw new IllegalArgumentException("cinematic sound category must not be blank");
	}

	@Override
	public CinematicEventType type() {
		return CinematicEventType.SOUND;
	}
}
