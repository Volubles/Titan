package com.voluble.titanMC.cinematics.model;

import com.voluble.titanMC.display.screen.ScreenEffectId;
import com.voluble.titanMC.display.screen.ScreenEffectTiming;
import net.kyori.adventure.text.Component;

import java.util.Objects;
import java.util.Optional;

public record ScreenCinematicEvent(
	int tick,
	int timelineSlot,
	int row,
	ScreenEffectId screenId,
	Optional<Component> title,
	Optional<ScreenEffectTiming> timing
) implements CinematicEvent {
	public ScreenCinematicEvent {
		CommandCinematicEvent.validatePlacement(tick, timelineSlot, row);
		Objects.requireNonNull(screenId, "screenId");
		title = Objects.requireNonNull(title, "title");
		timing = Objects.requireNonNull(timing, "timing");
	}

	@Override
	public CinematicEventType type() {
		return CinematicEventType.SCREEN;
	}
}
