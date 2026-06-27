package com.voluble.titanMC.onboarding.preview.actor;

import com.voluble.titanMC.onboarding.config.OnboardingPreviewStage;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

public record PreviewPath(Location entrance, Location focus, Location exit) {
	public PreviewPath {
		entrance = clone(entrance, "entrance");
		focus = clone(focus, "focus");
		exit = clone(exit, "exit");
		requireSameWorld(entrance, focus, "entrance", "focus");
		requireSameWorld(focus, exit, "focus", "exit");
	}

	public static PreviewPath from(OnboardingPreviewStage stage) {
		Objects.requireNonNull(stage, "stage");
		return new PreviewPath(
			stage.entrance().toLocation(),
			stage.focus().toLocation(),
			stage.exit().toLocation()
		);
	}

	private static Location clone(Location location, String label) {
		Location copy = Objects.requireNonNull(location, label).clone();
		if (copy.getWorld() == null) throw new IllegalArgumentException("Preview " + label + " location has no world");
		return copy;
	}

	private static void requireSameWorld(Location first, Location second, String firstLabel, String secondLabel) {
		World firstWorld = first.getWorld();
		World secondWorld = second.getWorld();
		if (!Objects.equals(firstWorld, secondWorld)) {
			throw new IllegalArgumentException("Preview " + firstLabel + " and " + secondLabel + " must be in the same world");
		}
	}
}
