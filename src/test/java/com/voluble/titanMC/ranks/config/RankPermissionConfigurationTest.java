package com.voluble.titanMC.ranks.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankPermissionConfigurationTest {
	@Test
	void bundledPermissionsAllowPlayersAndRestrictAdministration() throws Exception {
		try (var source = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
			assertNotNull(source);
			YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
				new InputStreamReader(source, StandardCharsets.UTF_8)
			);

			assertTrue(yaml.getBoolean("permissions.titanmc.rank.use.default"));
			assertEquals("op", yaml.getString("permissions.titanmc.rank.admin.default"));
		}
	}
}
