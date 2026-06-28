package com.voluble.titanMC.onboarding.config;

import org.bukkit.configuration.ConfigurationSection;

public record OnboardingResourcePackConfiguration(
	boolean enabled,
	boolean requireNexo,
	boolean sendWithNexo,
	long sendDelayTicks,
	long timeoutTicks
) {
	public OnboardingResourcePackConfiguration {
		if (sendDelayTicks < 0L) throw new IllegalArgumentException("resource pack send delay must not be negative");
		if (timeoutTicks <= 0L) throw new IllegalArgumentException("resource pack timeout must be positive");
	}

	public static OnboardingResourcePackConfiguration load(ConfigurationSection section) {
		return new OnboardingResourcePackConfiguration(
			OnboardingConfiguration.requiredBoolean(section, "readiness.resource-pack.enabled"),
			OnboardingConfiguration.requiredBoolean(section, "readiness.resource-pack.require-nexo"),
			OnboardingConfiguration.requiredBoolean(section, "readiness.resource-pack.send-with-nexo"),
			OnboardingConfiguration.requiredLong(section, "readiness.resource-pack.send-delay-ticks"),
			OnboardingConfiguration.requiredLong(section, "readiness.resource-pack.timeout-ticks")
		);
	}
}
