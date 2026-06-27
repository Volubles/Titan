package com.voluble.titanMC.cinematics.runtime.camera;

import com.voluble.titanMC.integrations.entitylib.EntityLibRuntime;
import org.bukkit.plugin.java.JavaPlugin;

final class PacketDisplayCameraRuntime {
	private PacketDisplayCameraRuntime() {
	}

	static void initialize(JavaPlugin plugin) {
		EntityLibRuntime.initialize(plugin);
	}
}
