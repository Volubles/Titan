package com.voluble.titanMC.onboarding.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Map;

final class ConfigurationSectionAdapter {
	private ConfigurationSectionAdapter() {
	}

	static ConfigurationSection of(Map<?, ?> values) {
		YamlConfiguration yaml = new YamlConfiguration();
		values.forEach((key, value) -> set(yaml, String.valueOf(key), value));
		return yaml;
	}

	private static void set(YamlConfiguration yaml, String path, Object value) {
		if (value instanceof Map<?, ?> map) {
			map.forEach((key, nested) -> set(yaml, path + "." + key, nested));
			return;
		}
		yaml.set(path, value);
	}
}
