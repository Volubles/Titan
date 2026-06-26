package com.voluble.titanMC.ranks.config;

import com.voluble.titanMC.display.message.DisplayLine;
import com.voluble.titanMC.display.message.DisplayMessage;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public record RankNotificationMessage(boolean enabled, boolean centered, List<String> lines) {
	public RankNotificationMessage {
		lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
		if (lines.stream().anyMatch(Objects::isNull)) {
			throw new IllegalArgumentException("lines must not contain null");
		}
	}

	public boolean hasLines() {
		return !lines.isEmpty();
	}

	public DisplayMessage render(Function<String, Component> renderer) {
		Objects.requireNonNull(renderer, "renderer");
		return new DisplayMessage(lines.stream()
			.map(renderer)
			.map(component -> centered ? DisplayLine.centered(component) : DisplayLine.left(component))
			.toList());
	}
}
