package com.voluble.titanMC.cells.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public record CellItemTemplate(Material material, int slot, String name, List<String> lore) {
	public CellItemTemplate {
		if (material == null || !material.isItem()) throw new IllegalArgumentException("GUI material must be an item");
		if (slot < 0 || slot > 53) throw new IllegalArgumentException("GUI slot must be between 0 and 53");
		lore = List.copyOf(lore);
	}

	public static CellItemTemplate load(ConfigurationSection section, CellItemTemplate defaults) {
		if (section == null) return defaults;
		Material material = Material.matchMaterial(section.getString("material", defaults.material().name()));
		if (material == null) throw new IllegalArgumentException("Unknown GUI material at " + section.getCurrentPath());
		return new CellItemTemplate(
			material,
			section.getInt("slot", defaults.slot()),
			section.getString("name", defaults.name()),
			section.contains("lore") ? section.getStringList("lore") : defaults.lore()
		);
	}
}
