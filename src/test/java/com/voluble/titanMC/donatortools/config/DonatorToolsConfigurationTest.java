package com.voluble.titanMC.donatortools.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonatorToolsConfigurationTest {

	@Test
	void emptyBlockListAllowsEveryBlockWhenEnabled() {
		DonatorToolsConfiguration configuration =
			DonatorToolsConfiguration.load(new YamlConfiguration());

		assertTrue(configuration.allows(Material.STONE));
		assertTrue(configuration.allows(Material.DIAMOND_ORE));
	}

	@Test
	void explicitBlockListAndEnabledFlagAreHonored() {
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.set("enabled", true);
		yaml.set("blocks", List.of("STONE"));
		DonatorToolsConfiguration configuration = DonatorToolsConfiguration.load(yaml);

		assertTrue(configuration.allows(Material.STONE));
		assertFalse(configuration.allows(Material.DIAMOND_ORE));

		yaml.set("enabled", false);
		assertFalse(DonatorToolsConfiguration.load(yaml).allows(Material.STONE));
	}

	@Test
	void invalidMaterialsFailTheWholeReload() {
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.set("blocks", List.of("STONE", "definitely_not_a_block"));

		assertThrows(
			IllegalArgumentException.class,
			() -> DonatorToolsConfiguration.load(yaml)
		);
	}
}
