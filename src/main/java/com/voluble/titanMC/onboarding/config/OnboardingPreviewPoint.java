package com.voluble.titanMC.onboarding.config;

import java.util.Arrays;
import java.util.Locale;

public enum OnboardingPreviewPoint {
	RUNWAY_ENTRANCE("runway-entrance", "preview.runway.entrance"),
	FOCUS("focus", "preview.carousel.focus"),
	RUNWAY_FOCUS("runway-focus", "preview.runway.focus"),
	RUNWAY_EXIT("runway-exit", "preview.runway.exit"),
	LEFT_ENTRANCE("left-entrance", "preview.carousel.left.entrance"),
	LEFT_STAGE("left-stage", "preview.carousel.left.stage"),
	LEFT_EXIT("left-exit", "preview.carousel.left.exit"),
	RIGHT_ENTRANCE("right-entrance", "preview.carousel.right.entrance"),
	RIGHT_STAGE("right-stage", "preview.carousel.right.stage"),
	RIGHT_EXIT("right-exit", "preview.carousel.right.exit");

	private final String key;
	private final String configPath;

	OnboardingPreviewPoint(String key, String configPath) {
		this.key = key;
		this.configPath = configPath;
	}

	public String key() {
		return key;
	}

	public String configPath() {
		return configPath;
	}

	public static OnboardingPreviewPoint parse(String input) {
		String normalized = input.trim().replace('-', '_').toUpperCase(Locale.ROOT);
		return Arrays.stream(values())
			.filter(point -> point.name().equals(normalized) || point.key.equalsIgnoreCase(input.trim()))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unknown preview point: " + input));
	}
}
