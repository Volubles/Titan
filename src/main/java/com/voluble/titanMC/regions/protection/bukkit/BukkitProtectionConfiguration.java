package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.model.WorldId;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.policy.WorldProtectionDefaults;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record BukkitProtectionConfiguration(
	boolean enabled,
	Set<WorldId> protectedWorlds,
	String bypassPermission
) {

	public BukkitProtectionConfiguration {
		protectedWorlds = Set.copyOf(Objects.requireNonNull(protectedWorlds, "protectedWorlds"));
		bypassPermission = Objects.requireNonNull(bypassPermission, "bypassPermission").trim();
		if (bypassPermission.isEmpty()) throw new IllegalArgumentException("bypass permission must not be blank");
	}

	public static BukkitProtectionConfiguration load(ConfigurationSection config, Server server) {
		Objects.requireNonNull(config, "config");
		Objects.requireNonNull(server, "server");
		boolean enabled = config.getBoolean("protection.enabled", true);
		String bypass = config.getString("protection.bypass-permission", "titanmc.protection.bypass");
		List<String> worldNames = config.getStringList("protection.protected-worlds");
		Set<WorldId> worlds = new LinkedHashSet<>();
		for (String worldName : worldNames) {
			if (worldName == null || worldName.isBlank()) {
				throw new IllegalArgumentException("protected-worlds must not contain blank names");
			}
			World world = server.getWorld(worldName);
			if (world == null) throw new IllegalArgumentException("Protected world is not loaded: " + worldName);
			worlds.add(new WorldId(world.getUID()));
		}
		return new BukkitProtectionConfiguration(enabled, worlds, bypass);
	}

	public WorldProtectionDefaults defaults() {
		WorldProtectionDefaults.Builder builder = WorldProtectionDefaults.builder()
			.fallback(ProtectionDecision.ALLOW);
		for (WorldId world : protectedWorlds) builder.worldDefault(world, ProtectionDecision.DENY);
		return builder.build();
	}
}
