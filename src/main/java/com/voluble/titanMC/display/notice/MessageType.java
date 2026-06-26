package com.voluble.titanMC.display.notice;

import java.util.Locale;
import java.util.Objects;

public enum MessageType {
	INFO("info", "<glyph:prefix_defaultchat> <color:#30bbf1>{{message}}</color>"),
	SUCCESS("success", "<glyph:prefix_successchat> <color:#42d829>{{message}}</color>"),
	ERROR("error", "<glyph:prefix_errorchat> <color:#d43030>{{message}}</color>");

	private final String configKey;
	private final String defaultTemplate;

	MessageType(String configKey, String defaultTemplate) {
		this.configKey = configKey;
		this.defaultTemplate = defaultTemplate;
	}

	public String configKey() {
		return configKey;
	}

	public String defaultTemplate() {
		return defaultTemplate;
	}

	public static MessageType parse(String value) {
		String normalized = Objects.requireNonNull(value, "value").trim().toLowerCase(Locale.ROOT);
		for (MessageType type : values()) {
			if (type.configKey.equals(normalized)) return type;
		}
		throw new IllegalArgumentException("Unknown message type: " + value);
	}
}
