package com.voluble.titanMC.onboarding.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record OnboardingPresentationConfiguration(
	boolean enabled,
	List<OnboardingPresentationStep> steps,
	OnboardingSoundCue completeSound,
	OnboardingSoundCue previewSpawnSound
) {
	public OnboardingPresentationConfiguration {
		steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
		completeSound = Objects.requireNonNull(completeSound, "completeSound");
		previewSpawnSound = Objects.requireNonNull(previewSpawnSound, "previewSpawnSound");
		if (enabled && steps.isEmpty()) throw new IllegalArgumentException("onboarding presentation must define at least one step");
	}

	static OnboardingPresentationConfiguration load(ConfigurationSection section) {
		boolean enabled = OnboardingConfiguration.requiredBoolean(section, "presentation.enabled");
		if (!enabled) {
			return new OnboardingPresentationConfiguration(false, List.of(), OnboardingSoundCue.disabled(), OnboardingSoundCue.disabled());
		}
		return new OnboardingPresentationConfiguration(
			true,
			steps(section),
			OnboardingSoundCue.load(OnboardingConfiguration.requiredSection(section, "presentation.complete-sound"), "presentation.complete-sound"),
			OnboardingSoundCue.load(OnboardingConfiguration.requiredSection(section, "presentation.preview-spawn-sound"), "presentation.preview-spawn-sound")
		);
	}

	private static List<OnboardingPresentationStep> steps(ConfigurationSection section) {
		if (!section.isList("steps")) throw new IllegalArgumentException("Onboarding config value must be a list: presentation.steps");
		List<OnboardingPresentationStep> steps = new ArrayList<>();
		List<Map<?, ?>> entries = section.getMapList("steps");
		for (int index = 0; index < entries.size(); index++) {
			Object raw = entries.get(index);
			if (!(raw instanceof Map<?, ?> map)) throw new IllegalArgumentException("Invalid onboarding presentation step at index " + index);
			steps.add(OnboardingPresentationStep.load(ConfigurationSectionAdapter.of(map), "presentation.steps[" + index + "]"));
		}
		return steps;
	}
}
