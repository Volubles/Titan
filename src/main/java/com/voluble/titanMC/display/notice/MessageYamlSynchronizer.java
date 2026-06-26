package com.voluble.titanMC.display.notice;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Objects;

final class MessageYamlSynchronizer {
	private static final Map<MessageType, String> DEFAULT_TEMPLATES = Map.of(
		MessageType.INFO, "<glyph:prefix_defaultchat><color:#30bbf1>{{message}}</color>",
		MessageType.SUCCESS, "<glyph:prefix_successchat><color:#42d829>{{message}}</color>",
		MessageType.ERROR, "<glyph:prefix_errorchat><color:#d43030>{{message}}</color>"
	);
	private static final Map<String, String> DEFAULT_GLYPHS = Map.of(
		"prefix_defaultchat", "",
		"prefix_successchat", "",
		"prefix_errorchat", ""
	);

	private MessageYamlSynchronizer() {
	}

	static boolean sync(YamlConfiguration yaml, List<MessageDefinition> defaults) {
		Objects.requireNonNull(yaml, "yaml");
		Objects.requireNonNull(defaults, "defaults");
		boolean changed = false;
		for (var entry : DEFAULT_TEMPLATES.entrySet()) {
			String path = "templates." + entry.getKey().configKey();
			if (!yaml.isSet(path)) {
				yaml.set(path, entry.getValue());
				changed = true;
			}
		}
		for (var entry : DEFAULT_GLYPHS.entrySet()) {
			String path = "glyphs." + entry.getKey();
			if (!yaml.isSet(path)) {
				yaml.set(path, entry.getValue());
				changed = true;
			}
		}
		for (MessageDefinition definition : defaults) {
			String path = "messages." + definition.type().configKey() + "." + definition.key().value();
			if (!yaml.isSet(path)) {
				yaml.set(path, definition.defaultText());
				changed = true;
			}
		}
		return changed;
	}
}
