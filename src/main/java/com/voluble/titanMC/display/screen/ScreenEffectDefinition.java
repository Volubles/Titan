package com.voluble.titanMC.display.screen;

import net.kyori.adventure.text.format.TextColor;

import java.util.Objects;
import java.util.Optional;

public record ScreenEffectDefinition(
	ScreenEffectId id,
	Optional<String> nexoGlyph,
	Optional<String> overlay,
	TextColor color,
	String title,
	ScreenEffectTiming timing,
	boolean hideHud
) {
	public ScreenEffectDefinition {
		Objects.requireNonNull(id, "id");
		nexoGlyph = Objects.requireNonNull(nexoGlyph, "nexoGlyph").map(String::trim).filter(value -> !value.isBlank());
		overlay = Objects.requireNonNull(overlay, "overlay").map(String::trim).filter(value -> !value.isBlank());
		Objects.requireNonNull(color, "color");
		title = Objects.requireNonNull(title, "title");
		Objects.requireNonNull(timing, "timing");
		if (nexoGlyph.isEmpty() && overlay.isEmpty()) {
			throw new IllegalArgumentException("screen " + id.value() + " must define nexo-glyph or overlay");
		}
	}
}
