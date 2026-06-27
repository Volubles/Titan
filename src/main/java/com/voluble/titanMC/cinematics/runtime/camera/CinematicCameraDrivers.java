package com.voluble.titanMC.cinematics.runtime.camera;

import com.voluble.titanMC.integrations.entitylib.EntityLibRuntime;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

public final class CinematicCameraDrivers {
	private CinematicCameraDrivers() {
	}

	public static CinematicCameraDriver create(Plugin plugin, Player player) {
		Objects.requireNonNull(plugin, "plugin");
		Objects.requireNonNull(player, "player");
		if (!packetCameraAvailable(plugin)) {
			return new TeleportCameraDriver(player);
		}
		if (!(plugin instanceof JavaPlugin javaPlugin)) {
			return new TeleportCameraDriver(player);
		}
		try {
			PacketDisplayCameraRuntime.initialize(javaPlugin);
			return new PacketDisplayCameraDriver(plugin, player);
		} catch (Throwable exception) {
			plugin.getLogger().log(Level.WARNING, "Packet cinematic camera unavailable; falling back to player teleports", exception);
			return new TeleportCameraDriver(player);
		}
	}

	private static boolean packetCameraAvailable(Plugin plugin) {
		return EntityLibRuntime.available(plugin);
	}
}
