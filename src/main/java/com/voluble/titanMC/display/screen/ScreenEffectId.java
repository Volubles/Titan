package com.voluble.titanMC.display.screen;

import java.util.Locale;
import java.util.Objects;

public record ScreenEffectId(String value) {
	public ScreenEffectId {
		value = Objects.requireNonNull(value, "value").trim().toLowerCase(Locale.ROOT);
		if (value.isBlank()) throw new IllegalArgumentException("screen effect id must not be blank");
		if (!value.matches("[a-z0-9_\\-]+")) throw new IllegalArgumentException("invalid screen effect id: " + value);
	}

	public static ScreenEffectId of(String value) {
		return new ScreenEffectId(value);
	}
}
