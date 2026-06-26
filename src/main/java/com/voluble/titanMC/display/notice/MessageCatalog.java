package com.voluble.titanMC.display.notice;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class MessageCatalog {
	private final Map<MessageType, String> templates;
	private final Map<String, String> glyphs;
	private final Map<MessageType, Map<MessageKey, String>> messages;

	private MessageCatalog(
			Map<MessageType, String> templates,
			Map<String, String> glyphs,
			Map<MessageType, Map<MessageKey, String>> messages
	) {
		this.templates = copyTemplates(templates);
		this.glyphs = Map.copyOf(glyphs);
		this.messages = copyMessages(messages);
	}

	public static MessageCatalog load(YamlConfiguration yaml) {
		Objects.requireNonNull(yaml, "yaml");
		Map<MessageType, String> templates = new EnumMap<>(MessageType.class);
		for (MessageType type : MessageType.values()) {
			String template = yaml.getString("templates." + type.configKey());
			if (template != null && !template.isBlank()) templates.put(type, template);
		}

		Map<String, String> glyphs = new LinkedHashMap<>();
		ConfigurationSection glyphSection = yaml.getConfigurationSection("glyphs");
		if (glyphSection != null) {
			for (String key : glyphSection.getKeys(false)) {
				glyphs.put(key, Objects.toString(glyphSection.getString(key), ""));
			}
		}

		Map<MessageType, Map<MessageKey, String>> messages = new EnumMap<>(MessageType.class);
		for (MessageType type : MessageType.values()) {
			ConfigurationSection section = yaml.getConfigurationSection("messages." + type.configKey());
			if (section == null) continue;
			Map<MessageKey, String> typed = new LinkedHashMap<>();
			for (String key : section.getKeys(true)) {
				if (!section.isString(key)) continue;
				String text = section.getString(key);
				if (text != null && !text.isBlank()) typed.put(MessageKey.of(key), text);
			}
			messages.put(type, typed);
		}
		return new MessageCatalog(templates, glyphs, messages);
	}

	public Optional<MessageEntry> find(MessageDefinition definition) {
		Objects.requireNonNull(definition, "definition");
		String text = messages.getOrDefault(definition.type(), Map.of()).get(definition.key());
		if (text == null) return Optional.empty();
		return Optional.of(new MessageEntry(definition.type(), definition.key(), text));
	}

	public String template(MessageType type) {
		return templates.getOrDefault(type, fallbackTemplate(type));
	}

	public String glyph(String key) {
		return glyphs.getOrDefault(key, "");
	}

	private static String fallbackTemplate(MessageType type) {
		return switch (type) {
			case INFO -> "<gray>{{message}}</gray>";
			case SUCCESS -> "<green>{{message}}</green>";
			case ERROR -> "<red>{{message}}</red>";
		};
	}

	private static Map<MessageType, String> copyTemplates(Map<MessageType, String> source) {
		Map<MessageType, String> copy = new EnumMap<>(MessageType.class);
		copy.putAll(source);
		return Map.copyOf(copy);
	}

	private static Map<MessageType, Map<MessageKey, String>> copyMessages(
			Map<MessageType, Map<MessageKey, String>> source
	) {
		Map<MessageType, Map<MessageKey, String>> copy = new EnumMap<>(MessageType.class);
		for (var entry : source.entrySet()) {
			copy.put(entry.getKey(), Map.copyOf(entry.getValue()));
		}
		return Map.copyOf(copy);
	}
}
