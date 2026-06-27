package com.voluble.titanMC.cinematics.config;

import com.voluble.titanMC.cinematics.model.CommandCinematicEvent;
import com.voluble.titanMC.cinematics.model.CinematicId;
import com.voluble.titanMC.cinematics.model.ParticleCinematicEvent;
import com.voluble.titanMC.cinematics.model.SoundCinematicEvent;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CinematicConfigurationTest {
	@Test
	void loadsTimelineEvents() {
		CinematicConfiguration config = CinematicConfiguration.load(yaml("""
			enabled: true
			defaults:
			  point-duration-ticks: 40
			  restore-player: true
			cinematics:
			  intro:
			    duration-ticks: 80
			    camera:
			      restore-player: true
			      points:
			        - { tick: 0, world: world, x: 0, y: 80, z: 0, yaw: 0, pitch: 0 }
			    timeline:
			      events:
			        - { type: sound, tick: 5, row: 1, world: world, x: 0, y: 80, z: 0, key: "minecraft:block.note_block.pling", volume: 1.0, pitch: 1.2, category: MASTER }
			        - { type: command, tick: 10, row: 2, command: "tell {player} hello", console: true }
			        - { type: particle, tick: 15, row: 3, world: world, x: 0, y: 80, z: 0, particle: CLOUD, count: 8, offset-x: 0.1, offset-y: 0.2, offset-z: 0.3, speed: 0.0 }
			"""));

		var definition = config.find(CinematicId.of("intro")).orElseThrow();

		assertEquals(3, definition.timeline().events().size());
		assertInstanceOf(SoundCinematicEvent.class, definition.timeline().events().get(0));
		assertInstanceOf(CommandCinematicEvent.class, definition.timeline().events().get(1));
		assertInstanceOf(ParticleCinematicEvent.class, definition.timeline().events().get(2));
		assertEquals(1, definition.timeline().atTick(5).size());
	}

	@Test
	void bundledConfigurationLoads() throws Exception {
		try (var source = getClass().getClassLoader().getResourceAsStream("cinematics/cinematics.yml")) {
			assertNotNull(source);
			YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(source, StandardCharsets.UTF_8));
			CinematicConfiguration config = CinematicConfiguration.load(yaml);

			assertTrue(config.enabled());
			assertTrue(config.find(CinematicId.of("onboarding_intro")).isPresent());
		}
	}

	private static YamlConfiguration yaml(String source) {
		YamlConfiguration yaml = new YamlConfiguration();
		try {
			yaml.loadFromString(source);
		} catch (Exception exception) {
			throw new IllegalStateException(exception);
		}
		return yaml;
	}
}
