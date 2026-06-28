package com.voluble.titanMC.onboarding.preview.actor;

import com.voluble.titanMC.onboarding.config.OnboardingPreviewStage;
import org.bukkit.Location;

import java.util.Objects;

import static com.voluble.titanMC.onboarding.preview.actor.PreviewLayoutLocations.copy;
import static com.voluble.titanMC.onboarding.preview.actor.PreviewLayoutLocations.requireSameWorld;

record CarouselPreviewLayout(
	Location leftEntrance,
	Location leftStage,
	Location leftExit,
	Location focus,
	Location rightEntrance,
	Location rightStage,
	Location rightExit
) {
	CarouselPreviewLayout {
		leftEntrance = copy(leftEntrance, "left entrance");
		leftStage = copy(leftStage, "left stage");
		leftExit = copy(leftExit, "left exit");
		focus = copy(focus, "focus");
		rightEntrance = copy(rightEntrance, "right entrance");
		rightStage = copy(rightStage, "right stage");
		rightExit = copy(rightExit, "right exit");
		requireSameWorld(leftEntrance, focus, "left entrance", "focus");
		requireSameWorld(leftStage, focus, "left stage", "focus");
		requireSameWorld(leftExit, focus, "left exit", "focus");
		requireSameWorld(rightEntrance, focus, "right entrance", "focus");
		requireSameWorld(rightStage, focus, "right stage", "focus");
		requireSameWorld(rightExit, focus, "right exit", "focus");
	}

	static CarouselPreviewLayout from(OnboardingPreviewStage stage) {
		Objects.requireNonNull(stage, "stage");
		return new CarouselPreviewLayout(
			stage.leftEntrance().toLocation(),
			stage.leftStage().toLocation(),
			stage.leftExit().toLocation(),
			stage.focus().toLocation(),
			stage.rightEntrance().toLocation(),
			stage.rightStage().toLocation(),
			stage.rightExit().toLocation()
		);
	}
}
