package com.voluble.titanMC.display.screen;

import net.kyori.adventure.text.Component;

import java.util.Objects;
import java.util.Optional;

public record ScreenEffectRequest(
	ScreenEffectId screenId,
	Optional<Component> title,
	Optional<ScreenEffectTiming> timing
) {
	public ScreenEffectRequest {
		Objects.requireNonNull(screenId, "screenId");
		title = Objects.requireNonNull(title, "title");
		timing = Objects.requireNonNull(timing, "timing");
	}

	public static ScreenEffectRequest of(ScreenEffectId screenId) {
		return new ScreenEffectRequest(screenId, Optional.empty(), Optional.empty());
	}

	public ScreenEffectRequest withTiming(ScreenEffectTiming timing) {
		return new ScreenEffectRequest(screenId, title, Optional.of(timing));
	}
}
