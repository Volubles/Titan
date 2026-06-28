package com.voluble.titanMC.onboarding.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

public record OnboardingPreviewStage(
	OnboardingConfiguration.LocationSpec runwayEntrance,
	OnboardingConfiguration.LocationSpec focus,
	OnboardingConfiguration.LocationSpec runwayExit,
	OnboardingConfiguration.LocationSpec leftEntrance,
	OnboardingConfiguration.LocationSpec leftStage,
	OnboardingConfiguration.LocationSpec leftExit,
	OnboardingConfiguration.LocationSpec rightEntrance,
	OnboardingConfiguration.LocationSpec rightStage,
	OnboardingConfiguration.LocationSpec rightExit
) {
	public OnboardingPreviewStage {
		Objects.requireNonNull(runwayEntrance, "runwayEntrance");
		Objects.requireNonNull(focus, "focus");
		Objects.requireNonNull(runwayExit, "runwayExit");
		Objects.requireNonNull(leftEntrance, "leftEntrance");
		Objects.requireNonNull(leftStage, "leftStage");
		Objects.requireNonNull(leftExit, "leftExit");
		Objects.requireNonNull(rightEntrance, "rightEntrance");
		Objects.requireNonNull(rightStage, "rightStage");
		Objects.requireNonNull(rightExit, "rightExit");
	}

	public static OnboardingPreviewStage load(ConfigurationSection root) {
		ConfigurationSection section = root.getConfigurationSection("preview-stage");
		if (section == null) throw new IllegalArgumentException("Missing section: preview-stage");
		return new OnboardingPreviewStage(
			loadRequired(section, OnboardingPreviewPoint.RUNWAY_ENTRANCE),
			loadRequired(section, OnboardingPreviewPoint.FOCUS),
			loadRequired(section, OnboardingPreviewPoint.RUNWAY_EXIT),
			loadRequired(section, OnboardingPreviewPoint.LEFT_ENTRANCE),
			loadRequired(section, OnboardingPreviewPoint.LEFT_STAGE),
			loadRequired(section, OnboardingPreviewPoint.LEFT_EXIT),
			loadRequired(section, OnboardingPreviewPoint.RIGHT_ENTRANCE),
			loadRequired(section, OnboardingPreviewPoint.RIGHT_STAGE),
			loadRequired(section, OnboardingPreviewPoint.RIGHT_EXIT)
		);
	}

	public OnboardingConfiguration.LocationSpec point(OnboardingPreviewPoint point) {
		return switch (point) {
			case RUNWAY_ENTRANCE -> runwayEntrance;
			case FOCUS -> focus;
			case RUNWAY_EXIT -> runwayExit;
			case LEFT_ENTRANCE -> leftEntrance;
			case LEFT_STAGE -> leftStage;
			case LEFT_EXIT -> leftExit;
			case RIGHT_ENTRANCE -> rightEntrance;
			case RIGHT_STAGE -> rightStage;
			case RIGHT_EXIT -> rightExit;
		};
	}

	private static OnboardingConfiguration.LocationSpec loadRequired(
		ConfigurationSection section,
		OnboardingPreviewPoint point
	) {
		ConfigurationSection pointSection = section.getConfigurationSection(point.key());
		if (pointSection == null) throw new IllegalArgumentException("Missing preview-stage point: " + point.key());
		return OnboardingConfiguration.LocationSpec.load(pointSection);
	}
}
