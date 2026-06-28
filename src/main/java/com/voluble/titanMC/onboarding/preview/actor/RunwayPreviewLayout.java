package com.voluble.titanMC.onboarding.preview.actor;

import com.voluble.titanMC.onboarding.config.OnboardingPreviewStage;
import org.bukkit.Location;

import java.util.Objects;

import static com.voluble.titanMC.onboarding.preview.actor.PreviewLayoutLocations.copy;
import static com.voluble.titanMC.onboarding.preview.actor.PreviewLayoutLocations.requireSameWorld;

record RunwayPreviewLayout(Location entrance, Location focus, Location exit) {
	RunwayPreviewLayout {
		entrance = copy(entrance, "runway entrance");
		focus = copy(focus, "focus");
		exit = copy(exit, "runway exit");
		requireSameWorld(entrance, focus, "runway entrance", "focus");
		requireSameWorld(focus, exit, "focus", "runway exit");
	}

	static RunwayPreviewLayout from(OnboardingPreviewStage stage) {
		Objects.requireNonNull(stage, "stage");
		return new RunwayPreviewLayout(
			stage.runwayEntrance().toLocation(),
			stage.focus().toLocation(),
			stage.runwayExit().toLocation()
		);
	}
}
