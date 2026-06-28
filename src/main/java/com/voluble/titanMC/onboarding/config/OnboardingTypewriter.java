package com.voluble.titanMC.onboarding.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

public record OnboardingTypewriter(long totalTicks, OnboardingSoundCue sound) {
	public OnboardingTypewriter {
		sound = Objects.requireNonNull(sound, "sound");
		if (totalTicks <= 0L) throw new IllegalArgumentException("onboarding presentation typewriter ticks must be positive");
	}

	static OnboardingTypewriter load(ConfigurationSection section, String path) {
		return new OnboardingTypewriter(
			OnboardingConfiguration.requiredLong(section, path + ".total-ticks"),
			OnboardingSoundCue.load(OnboardingConfiguration.requiredSection(section, path + ".sound"), path + ".sound")
		);
	}
}
