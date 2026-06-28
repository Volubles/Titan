package com.voluble.titanMC.onboarding.preview.actor;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

final class PreviewLayoutLocations {
	private PreviewLayoutLocations() {
	}

	static Location copy(Location location, String label) {
		Location copy = Objects.requireNonNull(location, label).clone();
		if (copy.getWorld() == null) throw new IllegalArgumentException("Preview " + label + " location has no world");
		return copy;
	}

	static void requireSameWorld(Location first, Location second, String firstLabel, String secondLabel) {
		World firstWorld = first.getWorld();
		World secondWorld = second.getWorld();
		if (!Objects.equals(firstWorld, secondWorld)) {
			throw new IllegalArgumentException("Preview " + firstLabel + " and " + secondLabel + " must be in the same world");
		}
	}
}
