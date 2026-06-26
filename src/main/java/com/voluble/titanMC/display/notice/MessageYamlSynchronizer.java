package com.voluble.titanMC.display.notice;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Objects;

final class MessageYamlSynchronizer {
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
		for (MessageType type : MessageType.values()) {
			String path = "templates." + type.configKey();
			if (!yaml.isSet(path)) {
				yaml.set(path, type.defaultTemplate());
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
			String path = "messages." + definition.key().value();
			if (!yaml.isList(path)) {
				yaml.set(path, definition.defaultLines());
				changed = true;
			}
		}
		return changed;
	}
}
