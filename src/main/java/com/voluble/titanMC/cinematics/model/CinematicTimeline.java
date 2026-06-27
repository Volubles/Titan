package com.voluble.titanMC.cinematics.model;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record CinematicTimeline(List<CinematicEvent> events) {
	public static final CinematicTimeline EMPTY = new CinematicTimeline(List.of());

	public CinematicTimeline {
		events = Objects.requireNonNull(events, "events").stream()
			.sorted(Comparator.comparingInt(CinematicEvent::tick).thenComparingInt(CinematicEvent::row))
			.toList();
	}

	public List<CinematicEvent> atTick(int tick) {
		return events.stream()
			.filter(event -> event.tick() == tick)
			.toList();
	}

	public CinematicTimeline withEvent(CinematicEvent event) {
		Objects.requireNonNull(event, "event");
		java.util.ArrayList<CinematicEvent> updated = new java.util.ArrayList<>(events);
		updated.add(event);
		return new CinematicTimeline(updated);
	}

	public CinematicTimeline without(CinematicEvent event) {
		Objects.requireNonNull(event, "event");
		java.util.ArrayList<CinematicEvent> updated = new java.util.ArrayList<>(events);
		updated.remove(event);
		return new CinematicTimeline(updated);
	}
}
