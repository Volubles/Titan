package com.voluble.titanMC.onboarding.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OnboardingConfigurationTest {
	@Test
	void loadsPreviewStagePoints() {
		OnboardingConfiguration config = OnboardingConfiguration.load(yaml("""
			enabled: true
			cinematic: onboarding_intro
			preview-stage:
			  entrance: { world: world, x: 1, y: 2, z: 3, yaw: 4, pitch: 5 }
			  focus: { world: world, x: 6, y: 7, z: 8, yaw: 9, pitch: 10 }
			  exit: { world: world, x: 11, y: 12, z: 13, yaw: 14, pitch: 15 }
			outfits:
			  - prison
			"""));

		assertEquals(1.0, config.previewStage().entrance().x());
		assertEquals(6.0, config.previewStage().focus().x());
		assertEquals(11.0, config.previewStage().exit().x());
	}

	@Test
	void legacyPreviewLocationBecomesWholeStage() {
		OnboardingConfiguration config = OnboardingConfiguration.load(yaml("""
			enabled: true
			cinematic: onboarding_intro
			preview: { world: world, x: 2, y: 3, z: 4, yaw: 5, pitch: 6 }
			outfits:
			  - prison
			"""));

		assertEquals(2.0, config.previewStage().entrance().x());
		assertEquals(2.0, config.previewStage().focus().x());
		assertEquals(2.0, config.previewStage().exit().x());
	}

	@Test
	void partialPreviewStageCanBeCapturedInAnyOrder() {
		OnboardingConfiguration config = OnboardingConfiguration.load(yaml("""
			enabled: true
			cinematic: onboarding_intro
			preview-stage:
			  entrance: { world: world, x: 9, y: 2, z: 3, yaw: 4, pitch: 5 }
			outfits:
			  - prison
			"""));

		assertEquals(9.0, config.previewStage().entrance().x());
		assertEquals(9.0, config.previewStage().focus().x());
		assertEquals(9.0, config.previewStage().exit().x());
	}

	private static YamlConfiguration yaml(String source) {
		YamlConfiguration yaml = new YamlConfiguration();
		try {
			yaml.loadFromString(source);
		} catch (Exception exception) {
			throw new IllegalStateException(exception);
		}
		return yaml;
	}
}
