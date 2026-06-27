package com.voluble.titanMC.cinematics.config;

import com.voluble.titanMC.cinematics.model.CameraPoint;
import com.voluble.titanMC.cinematics.model.CinematicDefinition;
import com.voluble.titanMC.cinematics.model.CinematicEvent;
import com.voluble.titanMC.cinematics.model.CinematicEventPosition;
import com.voluble.titanMC.cinematics.model.CinematicId;
import com.voluble.titanMC.cinematics.model.CommandCinematicEvent;
import com.voluble.titanMC.cinematics.model.ParticleCinematicEvent;
import com.voluble.titanMC.cinematics.model.SoundCinematicEvent;
import com.voluble.titanMC.managers.ConfigManager;
import com.voluble.titanMC.util.ComponentFiles;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class CinematicConfigurationManager implements ConfigManager.ComponentConfigManager {
	private final Plugin plugin;
	private final Path path;
	private final AtomicReference<CinematicConfiguration> current = new AtomicReference<>();

	public CinematicConfigurationManager(Plugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.path = ComponentFiles.resolve(plugin.getDataFolder().toPath(), "cinematics", "cinematics.yml");
	}

	@Override
	public void initialize() {
		try {
			if (Files.notExists(path)) {
				try (InputStream source = plugin.getResource("cinematics/cinematics.yml")) {
					if (source == null) throw new IllegalStateException("Missing bundled cinematics/cinematics.yml");
					Files.copy(source, path, StandardCopyOption.REPLACE_EXISTING);
				}
			}
			reload();
		} catch (Exception exception) {
			throw new IllegalStateException("Could not initialize cinematics/cinematics.yml", exception);
		}
	}

	@Override
	public void reload() {
		current.set(CinematicConfiguration.load(YamlConfiguration.loadConfiguration(path.toFile())));
	}

	public CinematicConfiguration current() {
		return Objects.requireNonNull(current.get(), "cinematic configuration has not been initialized");
	}

	public void createIfMissing(CinematicId id) {
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(path.toFile());
		String base = "cinematics." + id.value();
		if (yaml.isConfigurationSection(base)) return;
		yaml.set(base + ".duration-ticks", 80);
		yaml.set(base + ".camera.restore-player", current().defaultRestorePlayer());
		yaml.set(base + ".camera.points", List.of(
			Map.of("tick", 0, "world", "world", "x", 0.5, "y", 80.0, "z", 0.5, "yaw", 0.0, "pitch", 0.0),
			Map.of("tick", 80, "world", "world", "x", 0.5, "y", 80.0, "z", 0.5, "yaw", 0.0, "pitch", 0.0)
		));
		yaml.set(base + ".timeline.events", List.of());
		save(yaml);
		reload();
	}

	public void saveDefinition(CinematicDefinition definition) {
		Objects.requireNonNull(definition, "definition");
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(path.toFile());
		String base = "cinematics." + definition.id().value();
		yaml.set(base, null);
		yaml.set(base + ".duration-ticks", definition.durationTicks());
		yaml.set(base + ".camera.restore-player", definition.camera().restorePlayer());
		yaml.set(base + ".camera.points", definition.camera().points().stream()
			.map(this::cameraPointMap)
			.toList());
		yaml.set(base + ".timeline.events", definition.timeline().events().stream()
			.map(this::eventMap)
			.toList());
		save(yaml);
		reload();
	}

	public void appendCameraPoint(CinematicId id, CameraPoint point) {
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(path.toFile());
		String base = "cinematics." + id.value();
		if (!yaml.isConfigurationSection(base)) createIfMissing(id);
		yaml = YamlConfiguration.loadConfiguration(path.toFile());
		List<Map<?, ?>> points = yaml.getMapList(base + ".camera.points");
		points.add(Map.of(
			"tick", point.tick(),
			"world", point.world(),
			"x", point.x(),
			"y", point.y(),
			"z", point.z(),
			"yaw", point.yaw(),
			"pitch", point.pitch()
		));
		yaml.set(base + ".duration-ticks", Math.max(yaml.getInt(base + ".duration-ticks", 0), point.tick()));
		yaml.set(base + ".camera.points", points);
		save(yaml);
		reload();
	}

	public int nextPointTick(CinematicId id) {
		return current().find(id)
			.map(definition -> definition.camera().points().stream()
				.mapToInt(CameraPoint::tick)
				.max()
				.orElse(0) + current().defaultPointDurationTicks())
			.orElse(0);
	}

	private void save(YamlConfiguration yaml) {
		try {
			yaml.save(path.toFile());
		} catch (Exception exception) {
			throw new IllegalStateException("Could not save cinematics/cinematics.yml", exception);
		}
	}

	private Map<String, Object> cameraPointMap(CameraPoint point) {
		Map<String, Object> raw = new java.util.LinkedHashMap<>();
		raw.put("tick", point.tick());
		raw.put("world", point.world());
		raw.put("x", point.x());
		raw.put("y", point.y());
		raw.put("z", point.z());
		raw.put("yaw", point.yaw());
		raw.put("pitch", point.pitch());
		return raw;
	}

	private Map<String, Object> eventMap(CinematicEvent event) {
		Map<String, Object> raw = new java.util.LinkedHashMap<>();
		raw.put("type", event.type().name().toLowerCase(java.util.Locale.ROOT));
		raw.put("tick", event.tick());
		raw.put("row", event.row());
		switch (event) {
			case CommandCinematicEvent command -> {
				raw.put("command", command.command());
				raw.put("console", command.console());
			}
			case ParticleCinematicEvent particle -> {
				writePosition(raw, particle.position());
				raw.put("particle", particle.particle());
				raw.put("count", particle.count());
				raw.put("offset-x", particle.offsetX());
				raw.put("offset-y", particle.offsetY());
				raw.put("offset-z", particle.offsetZ());
				raw.put("speed", particle.speed());
			}
			case SoundCinematicEvent sound -> {
				writePosition(raw, sound.position());
				raw.put("key", sound.key());
				raw.put("volume", sound.volume());
				raw.put("pitch", sound.pitch());
				raw.put("category", sound.category());
			}
		}
		return raw;
	}

	private void writePosition(Map<String, Object> raw, CinematicEventPosition position) {
		raw.put("world", position.world());
		raw.put("x", position.x());
		raw.put("y", position.y());
		raw.put("z", position.z());
	}
}
