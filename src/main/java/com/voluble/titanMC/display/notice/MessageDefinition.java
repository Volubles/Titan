package com.voluble.titanMC.display.notice;

import java.util.Objects;

public record MessageDefinition(MessageKey key, MessageType type, String defaultText) {
	public MessageDefinition {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(type, "type");
		defaultText = Objects.requireNonNull(defaultText, "defaultText").trim();
		if (defaultText.isEmpty()) throw new IllegalArgumentException("defaultText must not be blank");
	}

	public static MessageDefinition of(String key, MessageType type, String defaultText) {
		return new MessageDefinition(MessageKey.of(key), type, defaultText);
	}
}
