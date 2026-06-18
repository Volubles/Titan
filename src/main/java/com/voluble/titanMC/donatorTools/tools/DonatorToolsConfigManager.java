package com.voluble.titanMC.donatorTools.tools;

import com.voluble.titanMC.managers.ConfigManager;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class DonatorToolsConfigManager implements ConfigManager.ComponentConfigManager {

	private final JavaPlugin plugin;
	private FileConfiguration config;
	private File configFile;
	private Set<Material> allowedBlocks;

	public DonatorToolsConfigManager(JavaPlugin plugin) {
		this.plugin = plugin;
	}

	public void initialize() {
		saveDefaultConfig();
		reload();
	}

	private void saveDefaultConfig() {
		if (configFile == null) {
			configFile = new File(plugin.getDataFolder(), "donator-tools.yml");
		}
		if (!configFile.exists()) {
			try (InputStream in = plugin.getResource("donator-tools.yml")) {
				if (in != null) {
					Files.copy(in, configFile.toPath());
				}
			} catch (Exception e) {
				plugin.getLogger().log(Level.SEVERE, "Could not save donator-tools.yml", e);
			}
		}
	}

	public void reload() {
		if (configFile == null) {
			configFile = new File(plugin.getDataFolder(), "donator-tools.yml");
		}
		config = YamlConfiguration.loadConfiguration(configFile);
		reloadAllowedBlocks();
	}

	public FileConfiguration getConfig() {
		if (config == null) {
			reload();
		}
		return config;
	}

	private void reloadAllowedBlocks() {
		allowedBlocks = new HashSet<>();
		List<String> blockNames = getConfig().getStringList("blocks");

		if (blockNames == null || blockNames.isEmpty()) {
			allowedBlocks = null;
			return;
		}

		for (String blockName : blockNames) {
			try {
				Material material = Material.valueOf(blockName.toUpperCase());
				allowedBlocks.add(material);
			} catch (IllegalArgumentException e) {
				plugin.getLogger().warning("Invalid block material in donator-tools.yml: " + blockName);
			}
		}
	}

	public boolean isBlockAllowed(Material material) {
		return allowedBlocks == null || allowedBlocks.contains(material);
	}
}

