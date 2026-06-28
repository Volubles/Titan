package com.voluble.titanMC.onboarding;

import com.voluble.titanMC.outfits.model.OutfitId;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public record OnboardingOutfitSelection(OutfitId outfitId, boolean original) {
	private static final String ORIGINAL = "original";

	public OnboardingOutfitSelection {
		if (!original) Objects.requireNonNull(outfitId, "outfitId");
		if (original && outfitId != null) throw new IllegalArgumentException("original selection must not have an outfit id");
	}

	public static OnboardingOutfitSelection parse(String value) {
		String normalized = Objects.requireNonNull(value, "value").trim().toLowerCase(Locale.ROOT);
		if (normalized.equals(ORIGINAL)) return originalSelection();
		return outfit(OutfitId.of(normalized));
	}

	public static OnboardingOutfitSelection outfit(OutfitId outfitId) {
		return new OnboardingOutfitSelection(Objects.requireNonNull(outfitId, "outfitId"), false);
	}

	public static OnboardingOutfitSelection originalSelection() {
		return new OnboardingOutfitSelection(null, true);
	}

	public Optional<OutfitId> outfit() {
		return Optional.ofNullable(outfitId);
	}

	public String storageValue() {
		return original ? ORIGINAL : outfitId.value();
	}
}
