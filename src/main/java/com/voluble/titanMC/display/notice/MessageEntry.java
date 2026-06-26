package com.voluble.titanMC.display.notice;

import java.util.List;
import java.util.Objects;

public record MessageEntry(MessageType type, MessageKey key, List<String> lines) {
	public MessageEntry {
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(lines, "lines");
		lines = List.copyOf(lines);
		if (lines.stream().anyMatch(Objects::isNull)) {
			throw new IllegalArgumentException("lines must not contain null");
		}
	}

	public static MessageEntry single(MessageType type, MessageKey key, String text) {
		return new MessageEntry(type, key, List.of(Objects.requireNonNull(text, "text")));
	}
}
