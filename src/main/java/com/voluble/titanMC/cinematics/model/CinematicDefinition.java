package com.voluble.titanMC.cinematics.model;

import java.util.Objects;

public record CinematicDefinition(
	CinematicId id,
	int durationTicks,
	CameraPathDefinition camera,
	CinematicTimeline timeline
) {
	public CinematicDefinition {
		Objects.requireNonNull(id, "id");
		if (durationTicks <= 0) throw new IllegalArgumentException("cinematic duration must be positive");
		Objects.requireNonNull(camera, "camera");
		Objects.requireNonNull(timeline, "timeline");
	}
}
