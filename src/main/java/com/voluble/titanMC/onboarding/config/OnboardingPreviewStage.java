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
		Objects.requireNonNull(focus, "focus");
	}

	public static OnboardingPreviewStage load(ConfigurationSection root, OnboardingPreviewMode mode) {
		Objects.requireNonNull(mode, "mode");
		ConfigurationSection section = root.getConfigurationSection("preview-stage");
		if (section == null) throw new IllegalArgumentException("Missing section: preview-stage");
		OnboardingConfiguration.LocationSpec focus = loadRequired(section, OnboardingPreviewPoint.FOCUS);
		return switch (mode) {
			case RUNWAY -> new OnboardingPreviewStage(
				loadRequired(section, OnboardingPreviewPoint.RUNWAY_ENTRANCE),
				focus,
				loadRequired(section, OnboardingPreviewPoint.RUNWAY_EXIT),
				loadOptional(section, OnboardingPreviewPoint.LEFT_ENTRANCE),
				loadOptional(section, OnboardingPreviewPoint.LEFT_STAGE),
				loadOptional(section, OnboardingPreviewPoint.LEFT_EXIT),
				loadOptional(section, OnboardingPreviewPoint.RIGHT_ENTRANCE),
				loadOptional(section, OnboardingPreviewPoint.RIGHT_STAGE),
				loadOptional(section, OnboardingPreviewPoint.RIGHT_EXIT)
			);
			case CAROUSEL -> new OnboardingPreviewStage(
				loadOptional(section, OnboardingPreviewPoint.RUNWAY_ENTRANCE),
				focus,
				loadOptional(section, OnboardingPreviewPoint.RUNWAY_EXIT),
				loadRequired(section, OnboardingPreviewPoint.LEFT_ENTRANCE),
				loadRequired(section, OnboardingPreviewPoint.LEFT_STAGE),
				loadRequired(section, OnboardingPreviewPoint.LEFT_EXIT),
				loadRequired(section, OnboardingPreviewPoint.RIGHT_ENTRANCE),
				loadRequired(section, OnboardingPreviewPoint.RIGHT_STAGE),
				loadRequired(section, OnboardingPreviewPoint.RIGHT_EXIT)
			);
		};
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

	private static OnboardingConfiguration.LocationSpec loadOptional(
		ConfigurationSection section,
		OnboardingPreviewPoint point
	) {
		ConfigurationSection pointSection = section.getConfigurationSection(point.key());
		return pointSection == null ? null : OnboardingConfiguration.LocationSpec.load(pointSection);
	}
}
