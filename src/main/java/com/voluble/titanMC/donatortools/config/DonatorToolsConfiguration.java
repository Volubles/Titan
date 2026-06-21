package com.voluble.titanMC.donatortools.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record DonatorToolsConfiguration(boolean enabled, Set<Material> allowedBlocks) {

	public DonatorToolsConfiguration {
		allowedBlocks = Set.copyOf(Objects.requireNonNull(allowedBlocks, "allowedBlocks"));
	}

	public static DonatorToolsConfiguration load(ConfigurationSection config) {
		Objects.requireNonNull(config, "config");
		boolean enabled = config.getBoolean("enabled", true);
		List<String> names = config.getStringList("blocks");
		Set<Material> materials = new LinkedHashSet<>();
		for (String name : names) {
			if (name == null || name.isBlank()) {
				throw new IllegalArgumentException("Donator tool block list must not contain blank values.");
			}
			Material material = Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));
			if (material == null || !material.isBlock()) {
				throw new IllegalArgumentException("Unknown donator tool block material: " + name);
			}
			materials.add(material);
		}
		return new DonatorToolsConfiguration(enabled, materials);
	}

	public boolean allows(Material material) {
		return enabled && (allowedBlocks.isEmpty() || allowedBlocks.contains(material));
	}
}
