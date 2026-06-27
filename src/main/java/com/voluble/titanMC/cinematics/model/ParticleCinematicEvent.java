package com.voluble.titanMC.cinematics.model;

import java.util.Objects;

public record ParticleCinematicEvent(
	int tick,
	int timelineSlot,
	int row,
	CinematicEventPosition position,
	String particle,
	int count,
	double offsetX,
	double offsetY,
	double offsetZ,
	double speed
) implements CinematicEvent {
	public ParticleCinematicEvent {
		CommandCinematicEvent.validatePlacement(tick, timelineSlot, row);
		position = Objects.requireNonNull(position, "position");
		particle = Objects.requireNonNull(particle, "particle").trim();
		if (particle.isBlank()) throw new IllegalArgumentException("cinematic particle must not be blank");
		if (count <= 0) throw new IllegalArgumentException("cinematic particle count must be positive");
	}

	@Override
	public CinematicEventType type() {
		return CinematicEventType.PARTICLE;
	}
}
