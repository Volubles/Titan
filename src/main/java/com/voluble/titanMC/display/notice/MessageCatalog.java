package com.voluble.titanMC.display.notice;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class MessageCatalog {
	private final Map<MessageType, String> templates;
	private final Map<MessageKey, List<String>> messages;
	private final Map<String, String> glyphs;

	private MessageCatalog(Map<MessageType, String> templates, Map<MessageKey, List<String>> messages, Map<String, String> glyphs) {
		this.templates = copyTemplates(templates);
		this.messages = copyMessages(messages);
		this.glyphs = Map.copyOf(glyphs);
	}

	public static MessageCatalog load(YamlConfiguration yaml) {
		Objects.requireNonNull(yaml, "yaml");
		Map<MessageType, String> templates = new EnumMap<>(MessageType.class);
		for (MessageType type : MessageType.values()) {
			String template = yaml.getString("templates." + type.configKey());
			if (template != null && !template.isBlank()) templates.put(type, template);
		}
		Map<MessageKey, List<String>> messages = new LinkedHashMap<>();
		ConfigurationSection section = yaml.getConfigurationSection("messages");
		if (section != null) {
			for (String key : section.getKeys(true)) {
				if (!section.isList(key)) continue;
				messages.put(MessageKey.of(key), section.getStringList(key));
			}
		}
		Map<String, String> glyphs = new LinkedHashMap<>();
		ConfigurationSection glyphSection = yaml.getConfigurationSection("glyphs");
		if (glyphSection != null) {
			for (String key : glyphSection.getKeys(false)) {
				glyphs.put(key, Objects.toString(glyphSection.getString(key), ""));
			}
		}
		return new MessageCatalog(templates, messages, glyphs);
	}

	public Optional<MessageEntry> find(MessageDefinition definition) {
		Objects.requireNonNull(definition, "definition");
		List<String> lines = messages.get(definition.key());
		if (lines == null) return Optional.empty();
		return Optional.of(new MessageEntry(definition.type(), definition.key(), lines));
	}

	public String template(MessageType type) {
		return templates.getOrDefault(type, type.defaultTemplate());
	}

	public String glyph(String key) {
		return glyphs.getOrDefault(key, "");
	}

	private static Map<MessageType, String> copyTemplates(Map<MessageType, String> source) {
		Map<MessageType, String> copy = new EnumMap<>(MessageType.class);
		copy.putAll(source);
		return Map.copyOf(copy);
	}

	private static Map<MessageKey, List<String>> copyMessages(Map<MessageKey, List<String>> source) {
		Map<MessageKey, List<String>> copy = new LinkedHashMap<>();
		for (var entry : source.entrySet()) {
			copy.put(entry.getKey(), List.copyOf(entry.getValue()));
		}
		return Map.copyOf(copy);
	}
}
