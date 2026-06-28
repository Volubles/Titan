package com.voluble.titanMC.display.screen;

import com.voluble.titanMC.managers.ConfigManager;
import com.voluble.titanMC.util.ComponentFiles;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class ScreenEffectConfigurationManager implements ConfigManager.ComponentConfigManager {
	private static final String RESOURCE = "display/screens.yml";

	private final Plugin plugin;
	private final Path path;
	private final AtomicReference<ScreenEffectConfiguration> current = new AtomicReference<>();

	public ScreenEffectConfigurationManager(Plugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.path = ComponentFiles.resolve(plugin.getDataFolder().toPath(), "display", "screens.yml");
	}

	@Override
	public void initialize() {
		try {
			if (Files.notExists(path)) {
				try (InputStream source = plugin.getResource(RESOURCE)) {
					if (source == null) throw new IllegalStateException("Missing bundled " + RESOURCE);
					Files.copy(source, path, StandardCopyOption.REPLACE_EXISTING);
				}
			}
			reload();
		} catch (Exception exception) {
			throw new IllegalStateException("Could not initialize display/screens.yml", exception);
		}
	}

	@Override
	public void reload() {
		current.set(ScreenEffectConfiguration.load(YamlConfiguration.loadConfiguration(path.toFile())));
	}

	public ScreenEffectConfiguration current() {
		return Objects.requireNonNull(current.get(), "screen effect configuration has not been initialized");
	}
}
