package com.voluble.titanMC.onboarding.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

public record OnboardingPresentationStep(
	OnboardingStyledText title,
	OnboardingStyledText subtitle,
	OnboardingTypewriter typewriter,
	long holdTicks
) {
	public OnboardingPresentationStep {
		title = Objects.requireNonNull(title, "title");
		subtitle = Objects.requireNonNull(subtitle, "subtitle");
		typewriter = Objects.requireNonNull(typewriter, "typewriter");
		if (holdTicks < 0L) throw new IllegalArgumentException("onboarding presentation hold ticks must not be negative");
	}

	static OnboardingPresentationStep load(ConfigurationSection section, String path) {
		return new OnboardingPresentationStep(
			OnboardingStyledText.load(OnboardingConfiguration.requiredSection(section, path + ".title"), path + ".title"),
			OnboardingStyledText.load(OnboardingConfiguration.requiredSection(section, path + ".subtitle"), path + ".subtitle"),
			OnboardingTypewriter.load(OnboardingConfiguration.requiredSection(section, path + ".typewriter"), path + ".typewriter"),
			OnboardingConfiguration.requiredLong(section, path + ".hold-ticks")
		);
	}
}
