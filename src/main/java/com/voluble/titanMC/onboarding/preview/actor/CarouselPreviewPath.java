package com.voluble.titanMC.onboarding.preview.actor;

import com.voluble.titanMC.onboarding.config.OnboardingPreviewStage;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

record CarouselPreviewPath(
	Location leftEntrance,
	Location leftStage,
	Location leftExit,
	Location focus,
	Location rightEntrance,
	Location rightStage,
	Location rightExit
) {
	CarouselPreviewPath {
		leftEntrance = clone(leftEntrance, "left entrance");
		leftStage = clone(leftStage, "left stage");
		leftExit = clone(leftExit, "left exit");
		focus = clone(focus, "focus");
		rightEntrance = clone(rightEntrance, "right entrance");
		rightStage = clone(rightStage, "right stage");
		rightExit = clone(rightExit, "right exit");
		requireSameWorld(leftEntrance, focus, "left entrance", "focus");
		requireSameWorld(leftStage, focus, "left stage", "focus");
		requireSameWorld(leftExit, focus, "left exit", "focus");
		requireSameWorld(rightEntrance, focus, "right entrance", "focus");
		requireSameWorld(rightStage, focus, "right stage", "focus");
		requireSameWorld(rightExit, focus, "right exit", "focus");
	}

	static CarouselPreviewPath from(OnboardingPreviewStage stage) {
		Objects.requireNonNull(stage, "stage");
		return new CarouselPreviewPath(
			stage.leftEntrance().toLocation(),
			stage.leftStage().toLocation(),
			stage.leftExit().toLocation(),
			stage.focus().toLocation(),
			stage.rightEntrance().toLocation(),
			stage.rightStage().toLocation(),
			stage.rightExit().toLocation()
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
