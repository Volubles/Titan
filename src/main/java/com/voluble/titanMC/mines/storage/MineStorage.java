package com.voluble.titanMC.mines.storage;

import com.voluble.titanMC.mines.Mine;
import com.voluble.titanMC.mines.WeightedPalette;
import com.voluble.titanMC.util.RegionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class MineStorage {

	private final Plugin plugin;
	private final File file;

	public MineStorage(Plugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.file = new File(plugin.getDataFolder(), "mines.yml");
	}

	public Map<String, Mine> loadAll() {
		FileConfiguration cfg = loadConfig();
		ConfigurationSection minesSec = cfg.getConfigurationSection("mines");
		Map<String, Mine> result = new LinkedHashMap<>();
		if (minesSec == null) return result;
		for (String name : minesSec.getKeys(false)) {
			ConfigurationSection s = minesSec.getConfigurationSection(name);
			if (s == null) continue;
			String worldStr = s.getString("world", null);
			UUID worldId;
			try {
				worldId = worldStr == null ? null : UUID.fromString(worldStr);
			} catch (IllegalArgumentException ex) {
				continue;
			}
			ConfigurationSection min = s.getConfigurationSection("min");
			ConfigurationSection max = s.getConfigurationSection("max");
			if (worldId == null || min == null || max == null) continue;
			int minX = min.getInt("x");
			int minY = min.getInt("y");
			int minZ = min.getInt("z");
			int maxX = max.getInt("x");
			int maxY = max.getInt("y");
			int maxZ = max.getInt("z");
			RegionUtils.Cuboid cuboid = new RegionUtils.Cuboid(worldId, minX, minY, minZ, maxX, maxY, maxZ);

			int interval = Math.max(1, s.getInt("interval_seconds", 900));
			int batch = Math.max(1, s.getInt("batch_per_tick", 1500));
			boolean enabled = s.getBoolean("enabled", true);
			int autoBelow = s.getInt("auto_reset_below_percent", -1);

			ConfigurationSection paletteSec = s.getConfigurationSection("palette");
			Map<String, Object> paletteMap = paletteSec != null ? paletteSec.getValues(false) : Collections.emptyMap();
			WeightedPalette palette = WeightedPalette.fromConfigMap(paletteMap);
			if (palette.isEmpty()) {
				// Ensure a reasonable default to avoid AIR-only mines
				palette.addOrUpdate(Material.STONE, 1);
			}

			Mine mine = new Mine(name, cuboid, interval, enabled, batch, palette);
			mine.setAutoResetBelowPercent(autoBelow);
			mine.setBrokenBlocks(s.getInt("broken_blocks", 0));
			long nextReset = s.getLong("next_reset_epoch_ms", mine.getNextResetEpochMs());
			mine.setNextResetEpochMs(nextReset);
			ConfigurationSection safeSpawn = s.getConfigurationSection("safe_spawn");
			if (safeSpawn != null) {
				String safeWorldStr = safeSpawn.getString("world");
				if (safeWorldStr != null) {
					try {
						UUID safeWorldId = UUID.fromString(safeWorldStr);
						World safeWorld = Bukkit.getWorld(safeWorldId);
						if (safeWorld != null) {
							double x = safeSpawn.getDouble("x");
							double y = safeSpawn.getDouble("y");
							double z = safeSpawn.getDouble("z");
							float yaw = (float) safeSpawn.getDouble("yaw", 0);
							float pitch = (float) safeSpawn.getDouble("pitch", 0);
							mine.setSafeSpawn(new Location(safeWorld, x, y, z, yaw, pitch));
						}
					} catch (IllegalArgumentException ignored) {
					}
				}
			}
			result.put(name, mine);
		}
		return result;
	}

	public void saveAll(Collection<Mine> mines) {
		FileConfiguration cfg = new YamlConfiguration();
		ConfigurationSection minesSec = cfg.createSection("mines");
		if (mines != null) {
			for (Mine mine : mines) {
				saveIntoSection(minesSec, mine);
			}
		}
		writeConfig(cfg);
	}

	public void saveMine(Mine mine) {
		FileConfiguration cfg = loadConfig();
		ConfigurationSection minesSec = cfg.getConfigurationSection("mines");
		if (minesSec == null) minesSec = cfg.createSection("mines");
		saveIntoSection(minesSec, mine);
		writeConfig(cfg);
	}

	public void deleteMine(String name) {
		FileConfiguration cfg = loadConfig();
		ConfigurationSection minesSec = cfg.getConfigurationSection("mines");
		if (minesSec != null) {
			minesSec.set(name, null);
		}
		writeConfig(cfg);
	}

	private void saveIntoSection(ConfigurationSection minesSec, Mine mine) {
		ConfigurationSection s = minesSec.createSection(mine.getName());
		s.set("world", mine.getCuboid().worldId.toString());
		ConfigurationSection min = s.createSection("min");
		min.set("x", mine.getCuboid().minX);
		min.set("y", mine.getCuboid().minY);
		min.set("z", mine.getCuboid().minZ);
		ConfigurationSection max = s.createSection("max");
		max.set("x", mine.getCuboid().maxX);
		max.set("y", mine.getCuboid().maxY);
		max.set("z", mine.getCuboid().maxZ);
		s.set("interval_seconds", mine.getResetIntervalSeconds());
		s.set("batch_per_tick", mine.getBatchSizePerTick());
		s.set("enabled", mine.isEnabled());
		s.set("auto_reset_below_percent", mine.getAutoResetBelowPercent());
		s.set("broken_blocks", mine.getBrokenBlocks());
		s.set("next_reset_epoch_ms", mine.getNextResetEpochMs());
		Location safeSpawn = mine.getSafeSpawn();
		if (safeSpawn != null) {
			ConfigurationSection safeSec = s.createSection("safe_spawn");
			safeSec.set("world", safeSpawn.getWorld().getUID().toString());
			safeSec.set("x", safeSpawn.getX());
			safeSec.set("y", safeSpawn.getY());
			safeSec.set("z", safeSpawn.getZ());
			safeSec.set("yaw", safeSpawn.getYaw());
			safeSec.set("pitch", safeSpawn.getPitch());
		}
		ConfigurationSection palette = s.createSection("palette");
		for (Map.Entry<String, Integer> e : mine.getPalette().toConfigMap().entrySet()) {
			palette.set(e.getKey(), e.getValue());
		}
	}

	private FileConfiguration loadConfig() {
		if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
		FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
		return cfg;
	}

	private void writeConfig(FileConfiguration cfg) {
		try {
			cfg.save(file);
		} catch (IOException e) {
			plugin.getLogger().severe("Failed to save mines.yml: " + e.getMessage());
		}
	}
}


