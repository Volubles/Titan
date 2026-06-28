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
		return switch (mode) {
			case RUNWAY -> loadRunway(root);
			case CAROUSEL -> loadCarousel(root);
		};
	}

	public OnboardingConfiguration.LocationSpec point(OnboardingPreviewPoint point) {
		return switch (point) {
			case RUNWAY_ENTRANCE -> runwayEntrance;
			case FOCUS, RUNWAY_FOCUS -> focus;
			case RUNWAY_EXIT -> runwayExit;
			case LEFT_ENTRANCE -> leftEntrance;
			case LEFT_STAGE -> leftStage;
			case LEFT_EXIT -> leftExit;
			case RIGHT_ENTRANCE -> rightEntrance;
			case RIGHT_STAGE -> rightStage;
			case RIGHT_EXIT -> rightExit;
		};
	}

	private static OnboardingPreviewStage loadRunway(ConfigurationSection root) {
		ConfigurationSection runway = requiredSection(root, "preview.runway");
		return new OnboardingPreviewStage(
			loadRequired(runway, "preview.runway.entrance"),
			loadRequired(runway, "preview.runway.focus"),
			loadRequired(runway, "preview.runway.exit"),
			null,
			null,
			null,
			null,
			null,
			null
		);
	}

	private static OnboardingPreviewStage loadCarousel(ConfigurationSection root) {
		ConfigurationSection carousel = requiredSection(root, "preview.carousel");
		ConfigurationSection left = requiredSection(carousel, "preview.carousel.left");
		ConfigurationSection right = requiredSection(carousel, "preview.carousel.right");
		return new OnboardingPreviewStage(
			null,
			loadRequired(carousel, "preview.carousel.focus"),
			null,
			loadRequired(left, "preview.carousel.left.entrance"),
			loadRequired(left, "preview.carousel.left.stage"),
			loadRequired(left, "preview.carousel.left.exit"),
			loadRequired(right, "preview.carousel.right.entrance"),
			loadRequired(right, "preview.carousel.right.stage"),
			loadRequired(right, "preview.carousel.right.exit")
		);
	}

	private static OnboardingConfiguration.LocationSpec loadRequired(ConfigurationSection section, String path) {
		ConfigurationSection pointSection = section.getConfigurationSection(lastPathPart(path));
		if (pointSection == null) throw new IllegalArgumentException("Missing onboarding preview point: " + path);
		return OnboardingConfiguration.LocationSpec.load(pointSection);
	}

	private static ConfigurationSection requiredSection(ConfigurationSection section, String path) {
		ConfigurationSection child = section.getConfigurationSection(lastPathPart(path));
		if (child == null) throw new IllegalArgumentException("Missing onboarding config section: " + path);
		return child;
	}

	private static String lastPathPart(String path) {
		int separator = path.lastIndexOf('.');
		return separator < 0 ? path : path.substring(separator + 1);
	}
}
