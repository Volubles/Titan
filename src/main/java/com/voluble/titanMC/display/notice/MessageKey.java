package com.voluble.titanMC.display.notice;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record MessageKey(String value) {
	private static final Pattern VALID = Pattern.compile("[a-z0-9][a-z0-9_.-]*");

	public MessageKey {
		value = Objects.requireNonNull(value, "value").trim().toLowerCase(Locale.ROOT);
		if (!VALID.matcher(value).matches()) {
			throw new IllegalArgumentException("Invalid message key: " + value);
		}
	}

	public static MessageKey of(String value) {
		return new MessageKey(value);
	}
}
