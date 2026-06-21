package com.voluble.titanMC.donatortools.config;

import com.voluble.titanMC.managers.ConfigManager;
import com.voluble.titanMC.util.ComponentFiles;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class DonatorToolsConfigurationManager
	implements ConfigManager.ComponentConfigManager, DonatorToolsSettings {

	private final Plugin plugin;
	private final Path path;
	private final AtomicReference<DonatorToolsConfiguration> current =
		new AtomicReference<>(new DonatorToolsConfiguration(false, java.util.Set.of()));

	public DonatorToolsConfigurationManager(Plugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.path = ComponentFiles.resolve(
			plugin.getDataFolder().toPath(),
			"donator-tools",
			"donator-tools.yml"
		);
	}

	@Override
	public void initialize() {
		try {
			Files.createDirectories(path.getParent());
			if (Files.notExists(path)) {
				try (InputStream source = plugin.getResource("donator-tools.yml")) {
					if (source == null) throw new IllegalStateException("Missing bundled donator-tools.yml");
					Files.copy(source, path, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		} catch (IOException exception) {
			throw new IllegalStateException("Could not initialize donator-tools.yml", exception);
		}
		reload();
	}

	@Override
	public void reload() {
		YamlConfiguration yaml = new YamlConfiguration();
		try {
			yaml.load(path.toFile());
		} catch (IOException | InvalidConfigurationException exception) {
			throw new IllegalStateException("Could not read donator-tools.yml", exception);
		}
		DonatorToolsConfiguration loaded = DonatorToolsConfiguration.load(yaml);
		current.set(loaded);
	}

	@Override
	public DonatorToolsConfiguration current() {
		return current.get();
	}
}
