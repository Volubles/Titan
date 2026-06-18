package com.voluble.titanMC.managers;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

	private final JavaPlugin plugin;
	private final List<ComponentConfigManager> componentManagers;

	public ConfigManager(JavaPlugin plugin) {
		this.plugin = plugin;
		this.componentManagers = new ArrayList<>();
	}

	public void initialize() {
		plugin.saveDefaultConfig();
	}

	public void registerComponent(ComponentConfigManager componentManager) {
		componentManager.initialize();
		componentManagers.add(componentManager);
	}

	public void reloadAll() {
		for (ComponentConfigManager manager : componentManagers) {
			manager.reload();
		}
	}

	public interface ComponentConfigManager {
		void initialize();
		void reload();
	}
}
