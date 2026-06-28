package com.voluble.titanMC.onboarding.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

public record OnboardingStyledText(String text, String style) {
	public OnboardingStyledText {
		text = Objects.requireNonNull(text, "text").trim();
		style = Objects.requireNonNull(style, "style").trim();
		if (text.isBlank()) throw new IllegalArgumentException("onboarding presentation text must not be blank");
		if (style.isBlank()) throw new IllegalArgumentException("onboarding presentation style must not be blank");
		if (!style.contains("{{text}}")) throw new IllegalArgumentException("onboarding presentation style must contain {{text}}");
	}

	static OnboardingStyledText load(ConfigurationSection section, String path) {
		return new OnboardingStyledText(
			OnboardingConfiguration.requiredString(section, path + ".text"),
			OnboardingConfiguration.requiredString(section, path + ".style")
		);
	}

	public String render(String visibleText) {
		return style.replace("{{text}}", visibleText);
	}
}
