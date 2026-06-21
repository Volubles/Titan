package com.voluble.titanMC.auctions.config;

import com.voluble.titanMC.managers.ConfigManager;
import com.voluble.titanMC.util.ComponentFiles;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class AuctionConfigurationManager implements ConfigManager.ComponentConfigManager {
	private final Plugin plugin;
	private final Path path;
	private AuctionConfiguration current;

	public AuctionConfigurationManager(Plugin plugin) {
		this.plugin = plugin;
		path = ComponentFiles.resolve(plugin.getDataFolder().toPath(), "auctions", "auctions.yml");
	}

	@Override
	public void initialize() {
		try {
			if (Files.notExists(path)) {
				try (InputStream source = plugin.getResource("auctions.yml")) {
					if (source == null) throw new IllegalStateException("Missing bundled auctions.yml");
					Files.copy(source, path, StandardCopyOption.REPLACE_EXISTING);
				}
			}
			reload();
		} catch (Exception exception) {
			throw new IllegalStateException("Could not initialize auctions.yml", exception);
		}
	}

	@Override
	public void reload() {
		current = AuctionConfiguration.load(YamlConfiguration.loadConfiguration(path.toFile()));
	}

	public AuctionConfiguration current() {
		return current;
	}
}
