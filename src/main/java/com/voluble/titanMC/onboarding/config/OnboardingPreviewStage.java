package com.voluble.titanMC.onboarding.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

public record OnboardingPreviewStage(
	OnboardingConfiguration.LocationSpec entrance,
	OnboardingConfiguration.LocationSpec focus,
	OnboardingConfiguration.LocationSpec exit
) {
	public OnboardingPreviewStage {
		Objects.requireNonNull(entrance, "entrance");
		Objects.requireNonNull(focus, "focus");
		Objects.requireNonNull(exit, "exit");
	}

	public static OnboardingPreviewStage load(ConfigurationSection root) {
		OnboardingConfiguration.LocationSpec legacy = loadIfPresent(root, "preview");
		ConfigurationSection section = root.getConfigurationSection("preview-stage");
		if (section == null) {
			if (legacy == null) throw new IllegalArgumentException("Missing section: preview-stage");
			return new OnboardingPreviewStage(legacy, legacy, legacy);
		}
		OnboardingConfiguration.LocationSpec fallback = firstPresent(section, legacy);
		OnboardingConfiguration.LocationSpec focus = loadOr(section, OnboardingPreviewPoint.FOCUS, fallback);
		return new OnboardingPreviewStage(
			loadOr(section, OnboardingPreviewPoint.ENTRANCE, focus),
			focus,
			loadOr(section, OnboardingPreviewPoint.EXIT, focus)
		);
	}

	public OnboardingConfiguration.LocationSpec point(OnboardingPreviewPoint point) {
		return switch (point) {
			case ENTRANCE -> entrance;
			case FOCUS -> focus;
			case EXIT -> exit;
		};
	}

	private static OnboardingConfiguration.LocationSpec loadOr(
		ConfigurationSection section,
		OnboardingPreviewPoint point,
		OnboardingConfiguration.LocationSpec fallback
	) {
		ConfigurationSection pointSection = section.getConfigurationSection(point.key());
		return pointSection == null ? fallback : OnboardingConfiguration.LocationSpec.load(pointSection);
	}

	private static OnboardingConfiguration.LocationSpec firstPresent(
		ConfigurationSection section,
		OnboardingConfiguration.LocationSpec legacy
	) {
		if (legacy != null) return legacy;
		for (OnboardingPreviewPoint point : OnboardingPreviewPoint.values()) {
			OnboardingConfiguration.LocationSpec spec = loadIfPresent(section, point.key());
			if (spec != null) return spec;
		}
		throw new IllegalArgumentException("Missing preview-stage focus location");
	}

	private static OnboardingConfiguration.LocationSpec loadIfPresent(ConfigurationSection root, String path) {
		ConfigurationSection section = root.getConfigurationSection(path);
		return section == null ? null : OnboardingConfiguration.LocationSpec.load(section);
	}
}
