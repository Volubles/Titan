package com.voluble.titanMC.display.notice;

import java.util.Locale;
import java.util.Objects;

public enum MessageType {
	INFO("info"),
	SUCCESS("success"),
	ERROR("error");

	private final String configKey;

	MessageType(String configKey) {
		this.configKey = configKey;
	}

	public String configKey() {
		return configKey;
	}

	public static MessageType parse(String value) {
		String normalized = Objects.requireNonNull(value, "value").trim().toLowerCase(Locale.ROOT);
		for (MessageType type : values()) {
			if (type.configKey.equals(normalized)) return type;
		}
		throw new IllegalArgumentException("Unknown message type: " + value);
	}
}
