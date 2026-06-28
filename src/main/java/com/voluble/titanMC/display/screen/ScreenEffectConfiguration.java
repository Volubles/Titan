package com.voluble.titanMC.display.screen;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record ScreenEffectConfiguration(Map<ScreenEffectId, ScreenEffectDefinition> screens) {
	public ScreenEffectConfiguration {
		screens = Map.copyOf(Objects.requireNonNull(screens, "screens"));
		if (screens.isEmpty()) throw new IllegalArgumentException("display/screens.yml must define at least one screen");
	}

	public Optional<ScreenEffectDefinition> find(ScreenEffectId id) {
		return Optional.ofNullable(screens.get(id));
	}

	public static ScreenEffectConfiguration load(FileConfiguration yaml) {
		ConfigurationSection defaults = requiredSection(yaml, "defaults");
		ConfigurationSection screens = requiredSection(yaml, "screens");
		Map<ScreenEffectId, ScreenEffectDefinition> definitions = new LinkedHashMap<>();
		for (String key : screens.getKeys(false)) {
			ConfigurationSection section = screens.getConfigurationSection(key);
			if (section == null) continue;
			ScreenEffectId id = ScreenEffectId.of(key);
			definitions.put(id, loadScreen(id, section, defaults));
		}
		return new ScreenEffectConfiguration(definitions);
	}

	private static ScreenEffectDefinition loadScreen(
		ScreenEffectId id,
		ConfigurationSection section,
		ConfigurationSection defaults
	) {
		String colorText = string(section, defaults, "color");
		TextColor color = TextColor.fromHexString(colorText);
		if (color == null) throw new IllegalArgumentException("invalid screen color for " + id.value() + ": " + colorText);
		ScreenEffectTiming timing = new ScreenEffectTiming(
			longValue(section, defaults, "fade-in-ticks"),
			longValue(section, defaults, "hold-ticks"),
			longValue(section, defaults, "fade-out-ticks")
		);
		return new ScreenEffectDefinition(
			id,
			optionalString(section, "nexo-glyph"),
			optionalString(section, "overlay"),
			color,
			string(section, defaults, "title"),
			timing,
			booleanValue(section, defaults, "hide-hud")
		);
	}

	private static ConfigurationSection requiredSection(ConfigurationSection section, String path) {
		ConfigurationSection child = section.getConfigurationSection(path);
		if (child == null) throw new IllegalArgumentException("Missing screen config section: " + path);
		return child;
	}

	private static Optional<String> optionalString(ConfigurationSection section, String key) {
		if (!section.isSet(key)) return Optional.empty();
		String value = section.getString(key);
		return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
	}

	private static String string(ConfigurationSection section, ConfigurationSection defaults, String key) {
		String value = section.isSet(key) ? section.getString(key) : defaults.getString(key);
		if (value == null) throw new IllegalArgumentException("Missing screen config value: " + key);
		return value;
	}

	private static long longValue(ConfigurationSection section, ConfigurationSection defaults, String key) {
		if (section.isSet(key)) return section.getLong(key);
		if (!defaults.isSet(key)) throw new IllegalArgumentException("Missing screen config value: " + key);
		return defaults.getLong(key);
	}

	private static boolean booleanValue(ConfigurationSection section, ConfigurationSection defaults, String key) {
		if (section.isSet(key)) return section.getBoolean(key);
		if (!defaults.isSet(key)) throw new IllegalArgumentException("Missing screen config value: " + key);
		return defaults.getBoolean(key);
	}
}
