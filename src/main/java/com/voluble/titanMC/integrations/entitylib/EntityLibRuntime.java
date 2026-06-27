package com.voluble.titanMC.integrations.entitylib;

import com.github.retrooper.packetevents.PacketEvents;
import me.tofaa.entitylib.APIConfig;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class EntityLibRuntime {
	private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

	private EntityLibRuntime() {
	}

	public static boolean available(Plugin plugin) {
		Objects.requireNonNull(plugin, "plugin");
		if (!plugin.getServer().getPluginManager().isPluginEnabled("packetevents")
			&& !plugin.getServer().getPluginManager().isPluginEnabled("PacketEvents")) {
			return false;
		}
		return present("com.github.retrooper.packetevents.PacketEvents")
			&& present("me.tofaa.entitylib.EntityLib");
	}

	public static void initialize(JavaPlugin plugin) {
		Objects.requireNonNull(plugin, "plugin");
		if (EntityLib.getOptionalApi().isPresent()) {
			INITIALIZED.set(true);
			return;
		}
		if (!INITIALIZED.compareAndSet(false, true)) return;
		SpigotEntityLibPlatform platform = new SpigotEntityLibPlatform(plugin);
		APIConfig settings = new APIConfig(PacketEvents.getAPI())
			.usePlatformLogger();
		EntityLib.init(platform, settings);
		plugin.getLogger().info("EntityLib runtime initialized");
	}

	private static boolean present(String className) {
		try {
			Class.forName(className);
			return true;
		} catch (ClassNotFoundException exception) {
			return false;
		}
	}
}
