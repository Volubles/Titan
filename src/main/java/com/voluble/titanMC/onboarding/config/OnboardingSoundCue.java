package com.voluble.titanMC.onboarding.config;

import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;

public record OnboardingSoundCue(boolean enabled, String key, String category, float volume, float pitch) {
	public OnboardingSoundCue {
		key = key == null ? "" : key.trim();
		category = category == null ? "" : category.trim();
		if (enabled && key.isBlank()) throw new IllegalArgumentException("enabled onboarding sound key must not be blank");
		if (enabled && category.isBlank()) throw new IllegalArgumentException("enabled onboarding sound category must not be blank");
		if (enabled) soundCategory(category);
		if (volume < 0.0F) throw new IllegalArgumentException("onboarding sound volume must not be negative");
		if (pitch < 0.0F) throw new IllegalArgumentException("onboarding sound pitch must not be negative");
	}

	public SoundCategory soundCategory() {
		return soundCategory(category);
	}

	static OnboardingSoundCue disabled() {
		return new OnboardingSoundCue(false, "", "", 0.0F, 0.0F);
	}

	static OnboardingSoundCue load(ConfigurationSection section, String path) {
		boolean enabled = OnboardingConfiguration.requiredBoolean(section, path + ".enabled");
		if (!enabled) return disabled();
		return new OnboardingSoundCue(
			true,
			OnboardingConfiguration.requiredString(section, path + ".key"),
			OnboardingConfiguration.requiredString(section, path + ".category"),
			(float) OnboardingConfiguration.requiredDouble(section, path + ".volume"),
			(float) OnboardingConfiguration.requiredDouble(section, path + ".pitch")
		);
	}

	private static SoundCategory soundCategory(String category) {
		try {
			return SoundCategory.valueOf(category.replace('-', '_').toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("unknown onboarding sound category: " + category, exception);
		}
	}
}
