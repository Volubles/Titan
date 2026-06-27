package com.voluble.titanMC.cinematics.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

public record CinematicEventPosition(
	String world,
	double x,
	double y,
	double z
) {
	public CinematicEventPosition {
		world = Objects.requireNonNull(world, "world").trim();
		if (world.isBlank()) throw new IllegalArgumentException("cinematic event position world must not be blank");
	}

	public static CinematicEventPosition at(Location location) {
		Objects.requireNonNull(location, "location");
		World world = Objects.requireNonNull(location.getWorld(), "location world");
		return new CinematicEventPosition(world.getName(), location.getX(), location.getY(), location.getZ());
	}

	public Location toLocation() {
		World bukkitWorld = Bukkit.getWorld(world);
		if (bukkitWorld == null) throw new IllegalStateException("Unknown cinematic event world: " + world);
		return new Location(bukkitWorld, x, y, z);
	}
}
