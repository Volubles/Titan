package com.voluble.titanMC.display.notice;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public record MessageDefinition(MessageKey key, MessageType type, List<String> defaultLines) {
	public MessageDefinition {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(defaultLines, "defaultLines");
		defaultLines = List.copyOf(defaultLines);
		if (defaultLines.stream().anyMatch(Objects::isNull)) {
			throw new IllegalArgumentException("defaultLines must not contain null");
		}
	}

	public static MessageDefinition of(String key, MessageType type, String defaultText) {
		return new MessageDefinition(MessageKey.of(key), type, List.of(Objects.requireNonNull(defaultText, "defaultText")));
	}

	public static MessageDefinition ofLines(String key, MessageType type, String... defaultLines) {
		Objects.requireNonNull(defaultLines, "defaultLines");
		return new MessageDefinition(MessageKey.of(key), type, Arrays.asList(defaultLines));
	}
}
