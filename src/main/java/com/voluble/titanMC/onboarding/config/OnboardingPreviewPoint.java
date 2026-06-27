package com.voluble.titanMC.onboarding.config;

import java.util.Arrays;
import java.util.Locale;

public enum OnboardingPreviewPoint {
	ENTRANCE("entrance"),
	FOCUS("focus"),
	EXIT("exit");

	private final String key;

	OnboardingPreviewPoint(String key) {
		this.key = key;
	}

	public String key() {
		return key;
	}

	public static OnboardingPreviewPoint parse(String input) {
		String normalized = input.trim().replace('-', '_').toUpperCase(Locale.ROOT);
		return Arrays.stream(values())
			.filter(point -> point.name().equals(normalized) || point.key.equalsIgnoreCase(input.trim()))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unknown preview point: " + input));
	}
}
